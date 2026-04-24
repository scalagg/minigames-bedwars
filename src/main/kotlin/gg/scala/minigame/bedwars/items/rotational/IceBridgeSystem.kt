package gg.scala.minigame.bedwars.items.rotational

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import me.lucko.helper.Events
import me.lucko.helper.Helper
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import org.bukkit.Effect
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.util.Vector
import java.util.UUID

/**
 * @author Limo
 * @date 2/18/2026
 */
class IceBridgeSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val BRIDGE_LENGTH = 30
        private const val MELT_TICKS = 20L * 30
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
                item.type == Material.ICE &&
                    item.itemMeta?.displayName?.contains("Ice Bridge") == true
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
                val plugin = Helper.hostPlugin()

                consumeOneFromHand(player)

                val direction = player.location.direction.clone()
                direction.setY(0).normalize()

                val perp = Vector(-direction.z, 0.0, direction.x).normalize()

                val startX = player.location.x + direction.x
                val startY = player.location.y
                val startZ = player.location.z + direction.z

                val allPlacedBlocks = mutableListOf<org.bukkit.block.Block>()

                for (i in 0 until BRIDGE_LENGTH)
                {
                    val stepX = startX + direction.x * i
                    val stepZ = startZ + direction.z * i
                    val tickDelay = i * 2L

                    Schedulers
                        .sync()
                        .runLater({
                            if (!lifecycle.game.state(GameState.Playing)) return@runLater

                            for (j in -1..1)
                            {
                                val offsetX = j * perp.x
                                val offsetZ = j * perp.z
                                val bx = Math.round(stepX + offsetX).toInt()
                                val by = (startY - 1).toInt()
                                val bz = Math.round(stepZ + offsetZ).toInt()

                                val block = world.getBlockAt(bx, by, bz)
                                if (!block.type.isSolid)
                                {
                                    block.type = Material.ICE
                                    block.setMetadata("bw_ice_bridge", FixedMetadataValue(plugin, player.uniqueId.toString()))
                                    block.setMetadata("placed", FixedMetadataValue(plugin, player.uniqueId))
                                    allPlacedBlocks += block

                                    world.playSound(block.location, Sound.GLASS, 1.0f, 0.5f)
                                    world.playEffect(block.location.clone().add(0.0, 1.3, 0.0), Effect.HAPPY_VILLAGER, 0)

                                    Schedulers
                                        .sync()
                                        .runLater({
                                            if (block.type == Material.ICE && block.hasMetadata("bw_ice_bridge"))
                                            {
                                                world.playSound(block.location, Sound.CHICKEN_EGG_POP, 1.0f, 0.5f)
                                                world.playEffect(block.location.clone().add(0.0, 1.3, 0.0), Effect.CLOUD, 0)
                                                block.type = Material.AIR
                                            }
                                        }, MELT_TICKS)
                                        .bindWith(lifecycle)
                                }
                            }
                        }, tickDelay)
                        .bindWith(lifecycle)
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
