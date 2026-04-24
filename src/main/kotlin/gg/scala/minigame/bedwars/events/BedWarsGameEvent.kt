package gg.scala.minigame.bedwars.events

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.minigame.MiniGameEvent
import java.time.Duration

/**
 * @author Limo
 * @date 2/13/2026
 */
abstract class BedWarsGameEvent(
    val lifecycle: BedWarsLifecycle,
    private val name: String,
    final override val duration: Duration
) : MiniGameEvent
{
    final override val description: String
        get() = name
}
