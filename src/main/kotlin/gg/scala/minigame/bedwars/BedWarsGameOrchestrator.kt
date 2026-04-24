package gg.scala.minigame.bedwars

import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.minigame.bedwars.shared.BedWarsGameConfiguration
import gg.scala.minigame.bedwars.shared.profile.BedWarsStatistics
import gg.tropic.practice.games.GameState
import gg.tropic.practice.games.event.GameStartEvent
import gg.tropic.practice.minigame.AbstractMiniGameGameImpl
import gg.tropic.practice.minigame.BasicMiniGameOrchestrator
import gg.tropic.practice.minigame.broadcastSeconds
import gg.tropic.practice.minigame.event.MiniGameStartTickEvent
import me.lucko.helper.Events
import net.evilblock.cubed.util.CC
import org.bukkit.Sound

/**
 * @author Limo
 * @date 2/13/2026
 */
@Service
object BedWarsGameOrchestrator : BasicMiniGameOrchestrator<BedWarsGameConfiguration>()
{
    override fun prepare(
        miniGame: AbstractMiniGameGameImpl<BedWarsGameConfiguration>,
        configuration: BedWarsGameConfiguration
    ) = BedWarsLifecycle(configuration, miniGame)

    @Configure
    fun configure()
    {
        BedWarsStatistics.ensureCustomStatisticsRegistered()

        Events
            .subscribe(MiniGameStartTickEvent::class.java)
            .filter { it.game.miniGameLifecycle is BedWarsLifecycle }
            .filter { it.secondsLeftUntilStart in broadcastSeconds }
            .handler {
                it.game.playSound(Sound.NOTE_STICKS, 1.0f)
                it.game.sendMessage(
                    "${CC.YELLOW}Game starts in ${CC.RED}${it.secondsLeftUntilStart}${CC.YELLOW} second${if (it.secondsLeftUntilStart == 1) "" else "s"}!"
                )
            }

        Events
            .subscribe(GameStartEvent::class.java)
            .filter { it.game.miniGameLifecycle is BedWarsLifecycle }
            .handler {
                it.game.playSound(Sound.ENDERDRAGON_GROWL, 1.0f)
                it.game.sendMessage(
                    "",
                    "${CC.YELLOW}Protect your bed and eliminate every other team.",
                    "${CC.RED}If your bed is destroyed, you will no longer respawn!",
                    ""
                )
            }
    }

    override val id = "bedwars"
}
