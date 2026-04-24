package gg.scala.minigame.bedwars.scoreboard

import gg.scala.commons.tablist.TablistPlayer
import gg.scala.commons.tablist.adapter.populator.PlayersProvider
import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import gg.tropic.practice.visuals.TeamVisuals
import net.evilblock.cubed.util.CC
import org.bukkit.entity.Player

/**
 * @author Limo
 * @date 2/19/2026
 */
class BedWarsPlayersProvider(
    private val lifecycle: BedWarsLifecycle
) : PlayersProvider()
{
    override fun getPlayers(viewer: Player): Collection<TablistPlayer>
    {
        if (!lifecycle.game.state(GameState.Playing))
        {
            return lifecycle.game.toBukkitPlayers()
                .filterNotNull()
                .map { player ->
                    TablistPlayer(
                        "",
                        "",
                        1,
                        1,
                        player.name,
                        "${CC.GRAY}${player.name}"
                    )
                }
        }

        return lifecycle.game.toBukkitPlayers()
            .filterNotNull()
            .filter { !GameService.isSpectating(it) }
            .map { player ->
                val team = lifecycle.game.getNullableTeam(player)
                val teamColor = team?.let { TeamVisuals.toChatColor(it.teamIdentifier) } ?: ""
                val teamLetter = team?.let {
                    val readable = TeamVisuals.toReadable(TeamVisuals.toChatColor(it.teamIdentifier))
                    readable.first().uppercaseChar()
                } ?: ""
                val prefix = if (team != null) "$teamColor[$teamLetter] " else ""
                TablistPlayer(
                    "",
                    "",
                    1,
                    1,
                    player.name,
                    "$prefix$teamColor${player.name}"
                )
            }
    }
}
