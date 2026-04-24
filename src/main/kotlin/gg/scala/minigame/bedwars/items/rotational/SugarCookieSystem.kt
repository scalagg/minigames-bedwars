package gg.scala.minigame.bedwars.items.rotational

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import me.lucko.helper.Events
import net.evilblock.cubed.util.CC
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * @author Limo
 * @date 2/18/2026
 */
class SugarCookieSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val EFFECT_DURATION_TICKS = 20 * 15
        private const val SPEED_AMPLIFIER = 2   
        private const val JUMP_AMPLIFIER = 3    
    }

    fun configure()
    {
        Events
            .subscribe(PlayerInteractEvent::class.java)
            .filter { event ->
                event.action == Action.RIGHT_CLICK_BLOCK || event.action == Action.RIGHT_CLICK_AIR
            }
            .filter { event ->
                val item = event.item ?: return@filter false
                item.type == Material.COOKIE &&
                    item.itemMeta?.displayName?.contains("Super Cookie") == true
            }
            .filter { event ->
                val game = GameService.byPlayer(event.player)
                game?.identifier == lifecycle.game.identifier
            }
            .handler { event ->
                event.isCancelled = true
                val player = event.player

                if (!lifecycle.game.state(GameState.Playing)) return@handler

                if (player.hasPotionEffect(PotionEffectType.JUMP) || player.hasPotionEffect(PotionEffectType.SPEED))
                {
                    player.sendMessage("${CC.RED}You already have Jump or Speed potion effect!")
                    return@handler
                }

                consumeOneFromHand(player)

                player.addPotionEffect(
                    PotionEffect(PotionEffectType.SPEED, EFFECT_DURATION_TICKS, SPEED_AMPLIFIER),
                    true
                )
                player.addPotionEffect(
                    PotionEffect(PotionEffectType.JUMP, EFFECT_DURATION_TICKS, JUMP_AMPLIFIER),
                    true
                )

                player.sendMessage("${CC.GREEN}Super Cookie activated! Speed III and Jump IV for 15 seconds!")
            }
            .bindWith(lifecycle)
    }

    private fun consumeOneFromHand(player: Player)
    {
        val inHand = player.itemInHand ?: return
        if (inHand.amount <= 1)
        {
            player.itemInHand = null
        }
        else
        {
            inHand.amount -= 1
        }
        player.updateInventory()
    }
}
