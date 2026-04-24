package gg.scala.minigame.bedwars.chat

import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.Syntax
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.game.extensions.profile.CorePlayerProfileService
import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.visuals.TeamVisuals
import net.evilblock.cubed.util.CC
import org.bukkit.entity.Player
import java.util.UUID

/**
 * @author Limo
 * @date 2/14/2026
 */
@AutoRegister
object ShoutCommand : ScalaCommand()
{
    private val cooldowns = mutableMapOf<UUID, Long>()
    private const val COOLDOWN_MS = 60_000L

    @CommandAlias("shout")
    @Syntax("<message>")
    fun onShout(player: Player, message: String)
    {
        val game = GameService.byPlayer(player)
        if (game == null)
        {
            player.sendMessage("${CC.RED}You must be in a game to use this command!")
            return
        }

        val lifecycle = game.miniGameLifecycle
        if (lifecycle !is BedWarsLifecycle)
        {
            player.sendMessage("${CC.RED}This command is only available in BedWars!")
            return
        }

        val team = game.getNullableTeam(player)?.teamIdentifier
        if (team == null)
        {
            player.sendMessage("${CC.RED}You must be on a team to shout!")
            return
        }

        val now = System.currentTimeMillis()
        val cooldownUntil = cooldowns[player.uniqueId] ?: 0L
        if (now < cooldownUntil)
        {
            val secondsLeft = ((cooldownUntil - now) / 1000L).toInt() + 1
            player.sendMessage("${CC.RED}You must wait ${CC.YELLOW}${secondsLeft}s${CC.RED} before shouting again!")
            return
        }

        cooldowns[player.uniqueId] = now + COOLDOWN_MS

        val bwLevel = CorePlayerProfileService.find(player)
            ?.getFormattedGameLevel("bedwars") ?: "${CC.GRAY}[1\u272B${CC.GRAY}]"

        val teamColor = TeamVisuals.toChatColor(team)
        val readable = TeamVisuals.toReadable(teamColor)

        val formatted = "$bwLevel $teamColor[$readable] ${CC.WHITE}${player.name}${CC.WHITE}: $message"
        val shoutFormatted = "${CC.GOLD}[SHOUT] $formatted"

        game.toBukkitPlayers()
            .filterNotNull()
            .forEach { target -> target.sendMessage(shoutFormatted) }

        game.expectedSpectators.forEach { spectatorUUID ->
            org.bukkit.Bukkit.getPlayer(spectatorUUID)?.sendMessage(shoutFormatted)
        }
    }
}
