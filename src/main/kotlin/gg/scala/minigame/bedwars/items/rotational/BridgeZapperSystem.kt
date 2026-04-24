package gg.scala.minigame.bedwars.items.rotational

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import me.lucko.helper.Events
import net.evilblock.cubed.util.CC
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import java.util.UUID

/**
 * @author Limo
 * @date 2/18/2026
 */
class BridgeZapperSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val RADIUS = 3
        private const val MAX_DESTROYED = 8
        private const val COOLDOWN_MS = 500L
    }

    private val cooldowns = mutableMapOf<UUID, Long>()

    fun configure()
    {
        Events
            .subscribe(PlayerInteractEvent::class.java)
            .filter { event ->
                event.action == Action.RIGHT_CLICK_BLOCK
            }
            .filter { event ->
                val item = event.item ?: return@filter false
                item.type == Material.SHEARS &&
                    item.itemMeta?.displayName?.contains("Bridge Zapper") == true
            }
            .filter { event ->
                val game = GameService.byPlayer(event.player)
                game?.identifier == lifecycle.game.identifier
            }
            .handler { event ->
                event.isCancelled = true
                val player = event.player
                val clickedBlock = event.clickedBlock ?: return@handler

                if (!lifecycle.game.state(GameState.Playing)) return@handler

                val now = System.currentTimeMillis()
                if (now - cooldowns.getOrDefault(player.uniqueId, 0L) < COOLDOWN_MS) return@handler
                cooldowns[player.uniqueId] = now

                if (!isWool(clickedBlock))
                {
                    player.sendMessage("${CC.RED}You can only use Bridge Zapper on wool!")
                    return@handler
                }

                if (!clickedBlock.hasMetadata("placed"))
                {
                    player.sendMessage("${CC.RED}You can only use Bridge Zapper on placed blocks!")
                    return@handler
                }

                consumeOneFromHand(player)

                val destroyed = zapNearbyWool(clickedBlock)

                player.playSound(player.location, Sound.FIZZ, 1.0f, 1.2f)
                player.sendMessage("${CC.GREEN}Bridge Zapper destroyed $destroyed wool block${if (destroyed != 1) "s" else ""}!")
            }
            .bindWith(lifecycle)
    }

    private fun zapNearbyWool(origin: Block): Int
    {
        var destroyed = 0
        val loc = origin.location

        for (x in (loc.blockX - RADIUS)..(loc.blockX + RADIUS))
        {
            for (y in (loc.blockY - RADIUS)..(loc.blockY + RADIUS))
            {
                for (z in (loc.blockZ - RADIUS)..(loc.blockZ + RADIUS))
                {
                    if (destroyed >= MAX_DESTROYED) return destroyed

                    val block = loc.world.getBlockAt(x, y, z)
                    if (isWool(block) && block.hasMetadata("placed"))
                    {
                        block.type = Material.AIR
                        destroyed++
                    }
                }
            }
        }

        return destroyed
    }

    private fun isWool(block: Block): Boolean
    {
        return block.type == Material.WOOL
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
