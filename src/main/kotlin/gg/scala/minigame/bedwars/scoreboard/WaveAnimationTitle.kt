package gg.scala.minigame.bedwars.scoreboard

import gg.scala.commons.scoreboard.TextAnimator
import gg.scala.commons.scoreboard.animations.TextFadeAnimation
import gg.scala.flavor.service.Close
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.lemon.metadata.NetworkMetadataDataSync
import net.evilblock.cubed.util.CC
import org.bukkit.ChatColor

/**
 * @author Limo
 * @date 2/11/2026
 */
@Service
object WaveAnimationTitle
{
    private val titleAnimator = TextAnimator.of(
        TextFadeAnimation(
            "${CC.BOLD}BED WARS",
            ChatColor.AQUA,
            ChatColor.DARK_AQUA,
            ChatColor.WHITE
        )
    )

    fun getCurrentTitle() = titleAnimator.current().text

    @Configure
    fun configure() {
        titleAnimator.schedule()
    }

    @Close
    fun close() {
        titleAnimator.dispose()
    }
}