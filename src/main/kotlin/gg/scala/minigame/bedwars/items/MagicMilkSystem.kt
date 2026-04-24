package gg.scala.minigame.bedwars.items

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import me.lucko.helper.Events
import net.evilblock.cubed.util.CC
import org.bukkit.Material
import org.bukkit.event.player.PlayerItemConsumeEvent
import java.util.UUID

/**
 * @author Limo
 * @date 2/14/2026
 */
class MagicMilkSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val IMMUNITY_DURATION_MS = 30_000L
    }

    private val immuneUntil = mutableMapOf<UUID, Long>()

    fun hasImmunity(uuid: UUID): Boolean
    {
        val expiry = immuneUntil[uuid] ?: return false
        if (System.currentTimeMillis() >= expiry)
        {
            immuneUntil.remove(uuid)
            return false
        }

        return true
    }

    fun configure()
    {
        Events
            .subscribe(PlayerItemConsumeEvent::class.java)
            .filter { event ->
                event.item?.type == Material.MILK_BUCKET
            }
            .filter { event ->
                val game = GameService.byPlayer(event.player)
                game?.identifier == lifecycle.game.identifier
            }
            .handler { event ->
                event.isCancelled = true
                event.player.itemInHand = null

                immuneUntil[event.player.uniqueId] = System.currentTimeMillis() + IMMUNITY_DURATION_MS

                event.player.sendMessage(
                    "${CC.GREEN}You now have trap immunity for 30 seconds!"
                )
            }
            .bindWith(lifecycle)
    }
}
