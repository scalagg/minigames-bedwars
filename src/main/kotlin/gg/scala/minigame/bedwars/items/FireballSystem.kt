package gg.scala.minigame.bedwars.items

import com.cryptomorin.xseries.XMaterial
import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import me.lucko.helper.Events
import org.bukkit.entity.Fireball
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import java.util.*

/**
 * @author Limo
 * @date 2/14/2026
 */
class FireballSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val EXPLOSION_POWER = 2.0f
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
                item.type == XMaterial.FIRE_CHARGE.get()
            }
            .filter { event ->
                val game = GameService.byPlayer(event.player)
                game?.identifier == lifecycle.game.identifier
            }
            .handler { event ->
                val player = event.player

                event.isCancelled = true

                if (!lifecycle.game.state(GameState.Playing))
                {
                    return@handler
                }

                val now = System.currentTimeMillis()
                if (now - cooldowns.getOrDefault(player.uniqueId, 0L) < COOLDOWN_MS)
                {
                    return@handler
                }

                cooldowns[player.uniqueId] = now

                val fireball = player.launchProjectile(Fireball::class.java)
                fireball.velocity = player.eyeLocation.direction.multiply(2.0)
                fireball.setIsIncendiary(false)
                fireball.yield = EXPLOSION_POWER
                fireball.shooter = player

                consumeOneFromHand(player)
            }
            .bindWith(lifecycle)

        Events
            .subscribe(EntityDamageByEntityEvent::class.java)
            .filter { event ->
                event.damager is Fireball
            }
            .filter { event ->
                event.entity is Player
            }
            .filter { event ->
                val game = GameService.byPlayer(event.entity as Player)
                game?.identifier == lifecycle.game.identifier
            }
            .handler { event ->
                val fireball = event.damager as Fireball
                val target = event.entity as Player
                val shooter = fireball.shooter

                if (shooter is Player && shooter.uniqueId == target.uniqueId)
                {
                    event.damage = 0.0
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
        } else
        {
            inHand.amount -= 1
        }
        player.updateInventory()
    }
}
