package gg.scala.minigame.bedwars.bed

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.extensions.setOwnership
import gg.tropic.practice.games.team.TeamIdentifier
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.material.Bed
import org.bukkit.util.Vector

/**
 * @author Limo
 * @date 2/13/2026
 */
data class TrackedBed(
    val team: TeamIdentifier,
    val foot: Vector,
    val head: Vector,
    var destroyed: Boolean = false
)

/**
 * @author Limo
 * @date 2/13/2026
 */
class BedTracker(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val SCAN_RADIUS = 20
    }

    private val beds = mutableMapOf<TeamIdentifier, TrackedBed>()

    fun totalBeds() = beds.size

    fun configure()
    {
        val world = lifecycle.game.arenaWorld
            ?: return

        lifecycle.game.teams.forEach { team ->
            val spawnPos = lifecycle.game.map
                .findSpawnLocationMatchingTeam(team.teamIdentifier)
                ?: return@forEach

            val spawnLoc = spawnPos.toLocation(world)
            val bedFoot = findClosestBedFoot(world, spawnLoc)
                ?: return@forEach

            val headBlock = findBedHead(bedFoot)
                ?: return@forEach

            bedFoot.setOwnership(team.teamIdentifier)
            headBlock.setOwnership(team.teamIdentifier)

            beds[team.teamIdentifier] = TrackedBed(
                team = team.teamIdentifier,
                foot = bedFoot.location.toVector(),
                head = headBlock.location.toVector()
            )
        }
    }

    private fun findClosestBedFoot(world: World, origin: Location): Block?
    {
        var closest: Block? = null
        var closestDist = Double.MAX_VALUE

        val cx = origin.blockX
        val cy = origin.blockY
        val cz = origin.blockZ

        for (x in (cx - SCAN_RADIUS)..(cx + SCAN_RADIUS))
        {
            for (y in (cy - SCAN_RADIUS)..(cy + SCAN_RADIUS))
            {
                for (z in (cz - SCAN_RADIUS)..(cz + SCAN_RADIUS))
                {
                    val block = world.getBlockAt(x, y, z)
                    if (block.type != Material.BED_BLOCK)
                    {
                        continue
                    }

                    val bedData = block.state.data
                    if (bedData is Bed && bedData.isHeadOfBed)
                    {
                        continue
                    }

                    val dist = block.location.distanceSquared(origin)
                    if (dist < closestDist)
                    {
                        closestDist = dist
                        closest = block
                    }
                }
            }
        }

        return closest
    }

    private fun findBedHead(foot: Block): Block?
    {
        val bedData = foot.state.data
        if (bedData !is Bed)
        {
            return null
        }

        return foot.getRelative(bedData.facing)
    }

    fun isBedAlive(team: TeamIdentifier) = beds[team]?.destroyed == false
    fun hasTrackedBed(team: TeamIdentifier) = beds.containsKey(team)
    fun aliveBedCount() = beds.values.count { tracked -> !tracked.destroyed }

    fun bedLocations(team: TeamIdentifier, world: World): List<org.bukkit.Location>
    {
        val tracked = beds[team]
            ?: return emptyList()

        return listOf(
            tracked.foot.toLocation(world),
            tracked.head.toLocation(world)
        )
    }

    fun destroyAllAlive(): List<TeamIdentifier>
    {
        return beds.values
            .filter { tracked -> !tracked.destroyed }
            .mapNotNull { tracked ->
                if (destroy(tracked.team)) tracked.team else null
            }
    }

    fun destroy(team: TeamIdentifier): Boolean
    {
        val tracked = beds[team]
            ?: return false

        if (tracked.destroyed)
        {
            return false
        }

        val world = lifecycle.game.arenaWorld
            ?: return false

        tracked.destroyed = true

        world.getBlockAt(tracked.foot.toLocation(world)).type = Material.AIR
        world.getBlockAt(tracked.head.toLocation(world)).type = Material.AIR
        return true
    }
}
