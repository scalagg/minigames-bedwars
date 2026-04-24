package gg.scala.minigame.bedwars.items.rotational

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.EnderPearl
import org.bukkit.entity.Player
import org.bukkit.event.entity.ProjectileLaunchEvent
import java.util.UUID

/**
 * @author Limo
 * @date 2/18/2026
 */
class TimeWarpPearlSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val WARP_BACK_TICKS = 20L * 3
    }

    private val warpOrigins = mutableMapOf<UUID, Location>()

    fun configure()
    {
        Events
            .subscribe(ProjectileLaunchEvent::class.java)
            .filter { event ->
                event.entity is EnderPearl
            }
            .filter { event ->
                val shooter = event.entity.shooter as? Player ?: return@filter false
                val item = shooter.itemInHand
                item?.type == Material.ENDER_PEARL &&
                    item.itemMeta?.displayName?.contains("Time Warp") == true
            }
            .filter { event ->
                val shooter = event.entity.shooter as? Player ?: return@filter false
                val game = GameService.byPlayer(shooter)
                game?.identifier == lifecycle.game.identifier
            }
            .handler { event ->
                val shooter = event.entity.shooter as Player

                if (!lifecycle.game.state(GameState.Playing)) return@handler

                warpOrigins[shooter.uniqueId] = shooter.location.clone()

                shooter.sendMessage("${CC.YELLOW}Time Warp Pearl thrown! You will warp back in 3 seconds.")

                Schedulers
                    .sync()
                    .runLater({
                        if (!lifecycle.game.state(GameState.Playing)) return@runLater

                        val origin = warpOrigins.remove(shooter.uniqueId) ?: return@runLater

                        if (shooter.isOnline && !shooter.isDead)
                        {
                            shooter.playSound(shooter.location, Sound.ENDERMAN_TELEPORT, 1.0f, 0.8f)
                            shooter.teleport(origin)
                            shooter.playSound(shooter.location, Sound.ENDERMAN_TELEPORT, 1.0f, 1.2f)
                            shooter.sendMessage("${CC.YELLOW}Time Warp! ${CC.GREEN}You've been warped back!")
                        }
                    }, WARP_BACK_TICKS)
                    .bindWith(lifecycle)
            }
            .bindWith(lifecycle)
    }
}
