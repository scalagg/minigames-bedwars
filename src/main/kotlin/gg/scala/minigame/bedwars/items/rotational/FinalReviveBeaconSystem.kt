package gg.scala.minigame.bedwars.items.rotational

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import gg.tropic.practice.games.player.SpectatingPlayerProvider
import me.lucko.helper.Events
import net.evilblock.cubed.util.CC
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import java.util.UUID

/**
 * @author Limo
 * @date 2/18/2026
 */
class FinalReviveBeaconSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val COOLDOWN_MS = 1000L
    }

    private val cooldowns = mutableMapOf<UUID, Long>()

    fun configure()
    {
        Events
            .subscribe(PlayerInteractEvent::class.java)
            .filter { event ->
                event.action == Action.RIGHT_CLICK_BLOCK || event.action == Action.RIGHT_CLICK_AIR
            }
            .filter { event ->
                val item = event.item ?: return@filter false
                item.type == Material.BEACON &&
                    item.itemMeta?.displayName?.contains("Final Revive") == true
            }
            .filter { event ->
                val game = GameService.byPlayer(event.player)
                game?.identifier == lifecycle.game.identifier
            }
            .handler { event ->
                event.isCancelled = true
                val player = event.player

                if (!lifecycle.game.state(GameState.Playing)) return@handler

                val now = System.currentTimeMillis()
                if (now - cooldowns.getOrDefault(player.uniqueId, 0L) < COOLDOWN_MS) return@handler
                cooldowns[player.uniqueId] = now

                val team = lifecycle.game.getNullableTeam(player)
                    ?.teamIdentifier
                    ?: return@handler

                val eliminatedTeammate = player.location.world.players
                    .filter { target ->
                        target.uniqueId != player.uniqueId &&
                            SpectatingPlayerProvider.spectating(target) &&
                            lifecycle.game.getNullableTeam(target)?.teamIdentifier == team
                    }
                    .firstOrNull()

                if (eliminatedTeammate == null)
                {
                    player.sendMessage("${CC.RED}No eliminated teammates to revive!")
                    return@handler
                }

                consumeOneFromHand(player)

                GameService.spectatorPlayers -= eliminatedTeammate.uniqueId

                eliminatedTeammate.gameMode = GameMode.SURVIVAL
                eliminatedTeammate.allowFlight = false
                eliminatedTeammate.isFlying = false
                eliminatedTeammate.health = eliminatedTeammate.maxHealth
                eliminatedTeammate.foodLevel = 20
                eliminatedTeammate.saturation = 20.0f
                eliminatedTeammate.inventory.clear()
                eliminatedTeammate.inventory.armorContents = arrayOfNulls(4)

                eliminatedTeammate.teleport(player.location)

                eliminatedTeammate.playSound(eliminatedTeammate.location, Sound.LEVEL_UP, 1.0f, 1.0f)
                eliminatedTeammate.sendMessage("${CC.GREEN}${CC.BOLD}You have been revived by ${player.name}!")

                player.playSound(player.location, Sound.LEVEL_UP, 1.0f, 1.0f)
                player.sendMessage("${CC.GREEN}You revived ${eliminatedTeammate.name}!")

                lifecycle.game.sendMessage(
                    "${CC.GREEN}${CC.BOLD}REVIVE! ${CC.GRAY}${player.name} revived ${eliminatedTeammate.name} using Final Revive Beacon!"
                )
            }
            .bindWith(lifecycle)
    }

    private fun consumeOneFromHand(player: Player)
    {
        val inHand = player.itemInHand ?: return
        if (inHand.amount <= 1)
        {
            player.inventory.removeItem(inHand)
        }
        else
        {
            inHand.amount -= 1
        }
        player.updateInventory()
    }
}
