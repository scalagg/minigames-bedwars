package gg.scala.minigame.bedwars.items

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.scala.minigame.bedwars.items.menu.TrackerTeamMenu
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import gg.tropic.practice.games.team.TeamIdentifier
import gg.tropic.practice.visuals.TeamVisuals
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import net.minecraft.server.v1_8_R3.ChatComponentText
import net.minecraft.server.v1_8_R3.PacketPlayOutChat
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer
import org.bukkit.entity.Player
import java.util.*

/**
 * @author Limo
 * @date 2/18/2026
 */
class TrackerSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val COMPASS_UPDATE_TICKS = 10L
    }

    val trackedTeams = mutableMapOf<UUID, TeamIdentifier>()

    fun openTrackerMenu(player: Player, viewerTeam: TeamIdentifier)
    {
        TrackerTeamMenu(lifecycle, this, viewerTeam).openMenu(player)
    }

    fun configure()
    {
        Schedulers.sync().runRepeating({ _ ->
            if (!lifecycle.game.state(GameState.Playing))
            {
                return@runRepeating
            }

            val toRemove = mutableListOf<UUID>()

            trackedTeams.forEach { (playerUuid, targetTeamId) ->
                val player = Bukkit.getPlayer(playerUuid) ?: return@forEach
                if (!player.isOnline || GameService.isSpectating(player)) return@forEach

                val targetTeam = lifecycle.game.teams
                    .firstOrNull { it.teamIdentifier == targetTeamId }
                    ?: return@forEach

                val nearestEnemy = targetTeam.nonSpectators()
                    .filter { p: Player -> p.isOnline && p.world == player.world }
                    .minByOrNull { p: Player -> p.location.distanceSquared(player.location) }

                if (nearestEnemy != null)
                {
                    player.compassTarget = nearestEnemy.location
                    val distance = player.location.distance(nearestEnemy.location).toInt()
                    val teamColor = TeamVisuals.toChatColor(targetTeamId)
                    val teamName = TeamVisuals.toReadable(teamColor)
                    sendActionBar(player, "${CC.BOLD}Tracking: $teamColor$teamName ${CC.RESET}- ${CC.WHITE}${distance}m away")
                } else
                {
                    toRemove += playerUuid
                    player.sendMessage("${CC.RED}The team you were tracking has been eliminated!")
                    sendActionBar(player, "${CC.RED}${CC.BOLD}Target Lost!")
                }
            }

            toRemove.forEach { trackedTeams.remove(it) }
        }, COMPASS_UPDATE_TICKS, COMPASS_UPDATE_TICKS).bindWith(lifecycle)
    }

    private fun sendActionBar(player: Player, message: String)
    {
        val packet = PacketPlayOutChat(ChatComponentText(message), 2.toByte())
        (player as CraftPlayer).handle.playerConnection.sendPacket(packet)
    }
}
