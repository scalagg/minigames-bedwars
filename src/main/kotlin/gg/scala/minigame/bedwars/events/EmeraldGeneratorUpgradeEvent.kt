package gg.scala.minigame.bedwars.events

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.scala.minigame.bedwars.generator.ResourceGeneratorType
import net.evilblock.cubed.util.CC
import org.bukkit.Sound
import java.time.Duration

/**
 * @author Limo
 * @date 2/13/2026
 */
class EmeraldGeneratorUpgradeEvent(
    lifecycle: BedWarsLifecycle,
    description: String,
    duration: Duration
) : BedWarsGameEvent(lifecycle, description, duration)
{
    override fun execute()
    {
        lifecycle.generatorSystem.upgrade(ResourceGeneratorType.Emerald)
        lifecycle.game.playSound(Sound.LEVEL_UP, 1.0f)
        lifecycle.game.sendMessage(
            "",
            "${CC.GREEN}Emerald generators have been upgraded!",
            ""
        )
    }
}
