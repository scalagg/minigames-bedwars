package gg.scala.minigame.bedwars.chat

import gg.scala.game.extensions.profile.CorePlayerProfileService
import gg.scala.lemon.channel.ChatChannelBuilder
import gg.scala.lemon.channel.ChatChannelService
import gg.scala.lemon.channel.channels.DefaultChatChannel
import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.visuals.TeamVisuals
import net.evilblock.cubed.util.CC
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * @author Limo
 * @date 2/14/2026
 */
class BedWarsTeamChatChannel(
    private val lifecycle: BedWarsLifecycle
)
{
    private val serializer = LegacyComponentSerializer.legacySection()

    fun configure()
    {
        val teamChannel = ChatChannelBuilder.newBuilder()
            .identifier("bedwars-team-${lifecycle.game.identifier}")
            .format { sender, receiver, message, server, rank ->
                val bukkitSender = org.bukkit.Bukkit.getPlayer(sender)
                val bwLevel = CorePlayerProfileService.find(bukkitSender)
                    ?.getFormattedGameLevel("bedwars") ?: "${CC.GRAY}[1\u272B${CC.GRAY}]"

                val team = lifecycle.game.getNullableTeam(bukkitSender)?.teamIdentifier
                val teamPrefix = if (team != null)
                {
                    val teamColor = TeamVisuals.toChatColor(team)
                    val readable = TeamVisuals.toReadable(teamColor)
                    "$teamColor[$readable] "
                } else ""

                val prefix = "$bwLevel $teamPrefix"

                val strippedRankPrefix = org.bukkit.ChatColor.stripColor(rank.prefix)
                val rankPrefix = if (strippedRankPrefix.isNotEmpty()) "${rank.prefix} " else ""

                serializer.deserialize("$prefix$rankPrefix${rank.color}${bukkitSender.name}${CC.WHITE}: $message")
            }
            .compose()
            .override(5000) { player ->
                val game = GameService.byPlayer(player)
                game?.identifier == lifecycle.game.identifier &&
                    game.getNullableTeam(player) != null
            }
            .displayToPlayer { senderUUID, target ->
                val senderTeam = lifecycle.game.teams
                    .firstOrNull { it.players.contains(senderUUID) }
                    ?.teamIdentifier
                val targetTeam = lifecycle.game.getNullableTeam(target)?.teamIdentifier

                if (senderTeam == null) return@displayToPlayer false

                senderTeam == targetTeam
            }

        ChatChannelService.register(teamChannel)

        DefaultChatChannel.provideAdditionalPrefix { player ->
            val gameImpl = GameService.byPlayer(player)
            val bwLevel = CorePlayerProfileService.find(player)
                ?.getFormattedGameLevel("bedwars") ?: "${CC.GRAY}[1\u272B${CC.GRAY}]"

            if (gameImpl?.identifier != lifecycle.game.identifier)
            {
                return@provideAdditionalPrefix serializer
                    .deserialize("$bwLevel ")
            }

            val team = gameImpl.getNullableTeam(player)?.teamIdentifier
            if (team != null)
            {
                val teamColor = TeamVisuals.toChatColor(team)
                val readable = TeamVisuals.toReadable(teamColor)

                serializer.deserialize("$bwLevel $teamColor[$readable] ")
            } else
            {
                serializer.deserialize("$bwLevel ")
            }
        }

        ChatChannelService.default.displayToPlayer = { senderUUID, target ->
            val senderGame = GameService.byPlayerOrSpectator(senderUUID)
            if (senderGame?.identifier != lifecycle.game.identifier)
            {
                true
            } else
            {
                val targetInGame = lifecycle.game.toBukkitPlayers()
                    .filterNotNull()
                    .any { it.uniqueId == target.uniqueId }
                val targetIsSpectator = lifecycle.game.expectedSpectators.contains(target.uniqueId)
                targetInGame || targetIsSpectator
            }
        }
    }
}
