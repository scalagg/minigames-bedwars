package gg.scala.minigame.bedwars.items.rotational

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import me.lucko.helper.Events
import me.lucko.helper.Helper
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.metadata.FixedMetadataValue
import java.util.UUID

/**
 * @author Limo
 * @date 2/18/2026
 */
class MegaTntSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val FUSE_TICKS = 40
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
                item.type == Material.TNT &&
                    item.itemMeta?.displayName?.contains("Mega TNT") == true
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

                val world = lifecycle.game.arenaWorld ?: return@handler
                val tnt = world.spawn(player.eyeLocation, TNTPrimed::class.java)
                tnt.fuseTicks = FUSE_TICKS
                tnt.velocity = player.eyeLocation.direction.multiply(1.0)
                tnt.setMetadata("bw_mega_tnt_owner", FixedMetadataValue(Helper.hostPlugin(), player.uniqueId.toString()))

                consumeOneFromHand(player)
            }
            .bindWith(lifecycle)

        Events
            .subscribe(EntityDamageByEntityEvent::class.java)
            .filter { event ->
                event.damager is TNTPrimed && event.entity is Player
            }
            .filter { event ->
                val game = GameService.byPlayer(event.entity as Player)
                game?.identifier == lifecycle.game.identifier
            }
            .handler { event ->
                val tnt = event.damager as TNTPrimed
                val target = event.entity as Player
                val ownerStr = tnt.getMetadata("bw_mega_tnt_owner").firstOrNull()?.asString()
                if (ownerStr != null && ownerStr == target.uniqueId.toString())
                {
                    event.damage *= 0.5
                }
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
