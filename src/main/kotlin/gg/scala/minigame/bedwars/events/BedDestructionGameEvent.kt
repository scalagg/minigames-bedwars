package gg.scala.minigame.bedwars.events

import gg.scala.minigame.bedwars.BedWarsLifecycle
import net.evilblock.cubed.util.CC
import org.bukkit.Sound
import java.time.Duration

/**
 * @author Limo
 * @date 2/13/2026
 */
class BedDestructionGameEvent(
    lifecycle: BedWarsLifecycle,
    description: String,
    duration: Duration
) : BedWarsGameEvent(lifecycle, description, duration)
{
    override fun execute()
    {
        val destroyed = lifecycle.bedTracker.destroyAllAlive()
        if (destroyed.isEmpty())
        {
            lifecycle.game.sendMessage("${CC.GRAY}All beds were already destroyed.")
            return
        }

        lifecycle.game.playSound(Sound.ENDERDRAGON_GROWL, 1.0f)
        lifecycle.game.sendMessage(
            "",
            "${CC.RED}${CC.BOLD}BED DESTRUCTION! ${CC.GRAY}All remaining beds have been destroyed.",
            "${CC.GRAY}No more respawns from this point onward.",
            ""
        )
    }
}
