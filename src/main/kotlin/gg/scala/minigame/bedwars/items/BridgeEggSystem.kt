package gg.scala.minigame.bedwars.items

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.scala.minigame.bedwars.team.BedWarsTeamPalette
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import me.lucko.helper.Events
import me.lucko.helper.Helper
import me.lucko.helper.Schedulers
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Egg
import org.bukkit.entity.Player
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.metadata.FixedMetadataValue

/**
 * @author Limo
 * @date 2/14/2026
 */
class BridgeEggSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val MAX_DISTANCE_SQUARED = 30.0 * 30.0
        private const val TRACK_INTERVAL_TICKS = 1L
    }

    fun configure()
    {
        Events
            .subscribe(ProjectileLaunchEvent::class.java)
            .filter { event ->
                event.entity is Egg
            }
            .filter { event ->
                val shooter = event.entity.shooter as? Player
                    ?: return@filter false

                val game = GameService.byPlayer(shooter)
                game?.identifier == lifecycle.game.identifier
            }
            .filter { event ->
                val shooter = event.entity.shooter as? Player
                    ?: return@filter false

                val item = shooter.itemInHand
                item?.type == Material.EGG && item.itemMeta?.displayName?.contains("Bridge Egg") == true
            }
            .handler { event ->
                val egg = event.entity as Egg
                val shooter = egg.shooter as? Player
                    ?: return@handler

                val team = lifecycle.game.getNullableTeam(shooter)
                    ?.teamIdentifier
                    ?: return@handler

                val woolData = BedWarsTeamPalette.woolData(team).toByte()
                val world = lifecycle.game.arenaWorld
                    ?: return@handler

                val startLocation = shooter.location.clone()
                val plugin = Helper.hostPlugin()

                Schedulers
                    .sync()
                    .runRepeating({ task ->
                        if (egg.isDead || !egg.isValid)
                        {
                            task.close()
                            return@runRepeating
                        }

                        if (!lifecycle.game.state(GameState.Playing))
                        {
                            task.close()
                            return@runRepeating
                        }

                        val loc = egg.location
                        if (loc.distanceSquared(startLocation) > MAX_DISTANCE_SQUARED)
                        {
                            task.close()
                            return@runRepeating
                        }

                        val bx = loc.blockX
                        val by = loc.blockY - 3
                        val bz = loc.blockZ

                        placeWool(world.getBlockAt(bx, by, bz), woolData, shooter, plugin)
                        placeWool(world.getBlockAt(bx - 1, by, bz), woolData, shooter, plugin)
                        placeWool(world.getBlockAt(bx, by, bz - 1), woolData, shooter, plugin)
                    }, TRACK_INTERVAL_TICKS, TRACK_INTERVAL_TICKS)
                    .bindWith(lifecycle)
            }
            .bindWith(lifecycle)
    }

    private fun placeWool(
        block: Block,
        woolData: Byte,
        player: Player,
        plugin: org.bukkit.plugin.Plugin
    )
    {
        if (block.type != Material.AIR)
        {
            return
        }

        block.type = Material.WOOL
        block.data = woolData
        block.setMetadata("placed", FixedMetadataValue(plugin, player.uniqueId))

        player.playSound(
            block.location,
            Sound.CHICKEN_EGG_POP,
            1.0f, 1.0f
        )
    }
}
