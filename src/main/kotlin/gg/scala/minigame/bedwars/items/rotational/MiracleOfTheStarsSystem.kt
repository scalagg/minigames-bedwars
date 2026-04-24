package gg.scala.minigame.bedwars.items.rotational

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import gg.tropic.practice.games.player.SpectatingPlayerProvider
import me.lucko.helper.Events
import net.evilblock.cubed.util.CC
import org.bukkit.Effect
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
class MiracleOfTheStarsSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val HEAL_AMOUNT = 12.0
        private const val RADIUS = 5.0
        private const val COOLDOWN_MS = 500L
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
                item.type == Material.NETHER_STAR &&
                    item.itemMeta?.displayName?.contains("Miracle of the Stars") == true
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

                consumeOneFromHand(player)

                var healed = 0

                player.health = (player.health + HEAL_AMOUNT).coerceAtMost(player.maxHealth)
                healed++

                player.location.world.players
                    .filter { target ->
                        target.uniqueId != player.uniqueId &&
                            !SpectatingPlayerProvider.spectating(target) &&
                            lifecycle.game.getNullableTeam(target)?.teamIdentifier == team &&
                            target.location.distanceSquared(player.location) <= RADIUS * RADIUS
                    }
                    .forEach { teammate ->
                        teammate.health = (teammate.health + HEAL_AMOUNT).coerceAtMost(teammate.maxHealth)
                        teammate.sendMessage("${CC.GREEN}You were healed by ${player.name}'s Miracle of the Stars!")
                        teammate.playSound(teammate.location, Sound.ORB_PICKUP, 1.0f, 1.5f)
                        healed++
                    }

                player.playSound(player.location, Sound.LEVEL_UP, 1.0f, 1.5f)
                player.sendMessage("${CC.GREEN}Healed $healed player${if (healed > 1) "s" else ""}!")
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
