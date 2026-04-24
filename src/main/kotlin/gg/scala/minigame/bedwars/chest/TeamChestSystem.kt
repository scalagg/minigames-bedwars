package gg.scala.minigame.bedwars.chest

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import gg.tropic.practice.games.team.TeamIdentifier
import gg.tropic.practice.visuals.TeamVisuals
import me.lucko.helper.Events
import net.evilblock.cubed.util.CC
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.Inventory
import org.bukkit.util.Vector

/**
 * @author Limo
 * @date 2/14/2026
 */
class TeamChestSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val SCAN_RADIUS = 20
        private const val CHEST_ROWS = 3
    }

    private val teamChestLocations = mutableMapOf<TeamIdentifier, Vector>()
    private val teamChestInventories = mutableMapOf<TeamIdentifier, Inventory>()

    fun configure()
    {
        val world = lifecycle.game.arenaWorld
            ?: return

        lifecycle.game.teams.forEach { team ->
            val spawnPos = lifecycle.game.map
                .findSpawnLocationMatchingTeam(team.teamIdentifier)
                ?: return@forEach

            val spawnLoc = spawnPos.toLocation(world)
            val chestBlock = findClosestChest(world, spawnLoc)
                ?: return@forEach

            val color = TeamVisuals.toChatColor(team.teamIdentifier)
            val readable = TeamVisuals.toReadable(color)

            teamChestLocations[team.teamIdentifier] = chestBlock.location.toVector()
            teamChestInventories[team.teamIdentifier] = Bukkit.createInventory(
                null,
                CHEST_ROWS * 9,
                "$color$readable Team Chest"
            )
        }

        Events
            .subscribe(PlayerInteractEvent::class.java)
            .filter { event ->
                event.action == Action.RIGHT_CLICK_BLOCK
            }
            .filter { event ->
                event.clickedBlock?.type == Material.CHEST || event.clickedBlock?.type == Material.TRAPPED_CHEST
            }
            .filter { event ->
                val game = GameService.byPlayer(event.player)
                game?.identifier == lifecycle.game.identifier
            }
            .handler { event ->
                val clickedVec = event.clickedBlock.location.toVector()
                val ownerTeam = teamChestLocations.entries
                    .firstOrNull { (_, vec) -> vec == clickedVec }
                    ?.key
                    ?: return@handler

                event.isCancelled = true

                if (!lifecycle.game.state(GameState.Playing))
                {
                    return@handler
                }

                val playerTeam = lifecycle.game.getNullableTeam(event.player)
                    ?.teamIdentifier

                if (playerTeam != ownerTeam)
                {
                    val isEliminated = isTeamEliminated(ownerTeam)
                    if (!isEliminated)
                    {
                        val color = TeamVisuals.toChatColor(ownerTeam)
                        val readable = TeamVisuals.toReadable(color)
                        event.player.sendMessage(
                            "${CC.RED}This is $color$readable${CC.RED}'s team chest! Eliminate them first."
                        )
                        return@handler
                    }
                }

                val inventory = teamChestInventories[ownerTeam]
                    ?: return@handler

                event.player.openInventory(inventory)
            }
            .bindWith(lifecycle)

        Events
            .subscribe(PlayerInteractEvent::class.java)
            .filter { event ->
                event.action == Action.LEFT_CLICK_BLOCK
            }
            .filter { event ->
                event.clickedBlock?.type == Material.CHEST || event.clickedBlock?.type == Material.TRAPPED_CHEST
            }
            .filter { event ->
                val game = GameService.byPlayer(event.player)
                game?.identifier == lifecycle.game.identifier
            }
            .handler { event ->
                val clickedVec = event.clickedBlock.location.toVector()
                val ownerTeam = teamChestLocations.entries
                    .firstOrNull { (_, vec) -> vec == clickedVec }
                    ?.key
                    ?: return@handler

                event.isCancelled = true

                if (!lifecycle.game.state(GameState.Playing))
                {
                    return@handler
                }

                val playerTeam = lifecycle.game.getNullableTeam(event.player)
                    ?.teamIdentifier

                if (playerTeam != ownerTeam)
                {
                    val isEliminated = isTeamEliminated(ownerTeam)
                    if (!isEliminated)
                    {
                        val color = TeamVisuals.toChatColor(ownerTeam)
                        val readable = TeamVisuals.toReadable(color)
                        event.player.sendMessage(
                            "${CC.RED}This is $color$readable${CC.RED}'s team chest! Eliminate them first."
                        )
                        return@handler
                    }
                }

                val player = event.player
                val heldItem = player.itemInHand

                if (heldItem == null || heldItem.type == Material.AIR)
                {
                    player.sendMessage("${CC.RED}You are not holding any item!")
                    return@handler
                }

                val inventory = teamChestInventories[ownerTeam]
                    ?: return@handler

                val depositAmount = heldItem.amount
                val itemName = heldItem.itemMeta?.displayName
                    ?: heldItem.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                val remaining = inventory.addItem(heldItem)

                if (remaining.isEmpty())
                {
                    player.itemInHand = null
                    player.sendMessage("${CC.GREEN}Deposited ${CC.WHITE}${depositAmount}x $itemName${CC.GREEN} into Team Chest!")
                } else
                {
                    val leftover = remaining.values.first()
                    val deposited = depositAmount - leftover.amount
                    player.itemInHand = leftover
                    if (deposited > 0)
                    {
                        player.sendMessage("${CC.YELLOW}Deposited ${CC.WHITE}${deposited}x $itemName${CC.YELLOW} into Team Chest! (chest full)")
                    } else
                    {
                        player.sendMessage("${CC.RED}Team Chest is full!")
                    }
                }

                player.playSound(player.location, Sound.CHEST_OPEN, 1.0f, 1.0f)
            }
            .bindWith(lifecycle)
    }

    private fun isTeamEliminated(team: TeamIdentifier): Boolean
    {
        val bedAlive = lifecycle.bedTracker.isBedAlive(team)
        if (bedAlive)
        {
            return false
        }

        val gameTeam = lifecycle.game.teams
            .firstOrNull { t -> t.teamIdentifier == team }
            ?: return true

        return gameTeam.nonSpectators().isEmpty()
    }

    private fun findClosestChest(world: World, origin: Location): Block?
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
                    if (block.type != Material.CHEST && block.type != Material.TRAPPED_CHEST)
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
}
