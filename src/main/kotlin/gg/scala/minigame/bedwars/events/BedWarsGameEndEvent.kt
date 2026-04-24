package gg.scala.minigame.bedwars.events

import gg.scala.minigame.bedwars.BedWarsLifecycle
import java.time.Duration

/**
 * @author Limo
 * @date 2/13/2026
 */
class BedWarsGameEndEvent(
    lifecycle: BedWarsLifecycle,
    description: String,
    duration: Duration
) : BedWarsGameEvent(lifecycle, description, duration)
{
    override fun execute()
    {
        lifecycle.game.complete(null)
    }
}
