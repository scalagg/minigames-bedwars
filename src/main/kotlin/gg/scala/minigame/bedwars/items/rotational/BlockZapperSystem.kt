package gg.scala.minigame.bedwars.items.rotational

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import me.lucko.helper.Events
import net.evilblock.cubed.util.CC
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
class BlockZapperSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val MAX_BLOCKS = 16
        private const val MAX_DISTANCE = 20
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
                item.type == Material.BLAZE_ROD &&
                    item.itemMeta?.displayName?.contains("Block Zapper") == true
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

                consumeOneFromHand(player)

                val destroyed = zapBlocks(player)

                player.playSound(player.location, Sound.FIZZ, 1.0f, 0.8f)
                player.sendMessage("${CC.GREEN}Block Zapper destroyed $destroyed block${if (destroyed != 1) "s" else ""}!")
            }
            .bindWith(lifecycle)
    }

    private fun zapBlocks(player: Player): Int
    {
        val direction = player.eyeLocation.direction.normalize()
        val origin = player.eyeLocation.clone()
        var destroyed = 0

        for (i in 1..MAX_DISTANCE)
        {
            if (destroyed >= MAX_BLOCKS) break

            val checkLoc = origin.clone().add(
                direction.x * i,
                direction.y * i,
                direction.z * i
            )

            val block = checkLoc.block

            if (block.type == Material.AIR) continue

            if (!block.hasMetadata("placed")) continue

            block.type = Material.AIR
            destroyed++
        }

        return destroyed
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
