package gg.scala.minigame.bedwars.items.rotational

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import me.lucko.helper.Events
import me.lucko.helper.Helper
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Chest
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

/**
 * @author Limo
 * @date 2/18/2026
 */
class LuckyChestSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val CHEST_LIFETIME_TICKS = 20L * 30
        private const val COOLDOWN_MS = 500L

        private val LOOT_TABLE = listOf(
            { ItemStack(Material.DIAMOND, ThreadLocalRandom.current().nextInt(1, 5)) },
            { ItemStack(Material.EMERALD, ThreadLocalRandom.current().nextInt(1, 4)) },
            { ItemStack(Material.IRON_INGOT, ThreadLocalRandom.current().nextInt(8, 25)) },
            { ItemStack(Material.GOLD_INGOT, ThreadLocalRandom.current().nextInt(4, 13)) },
            { ItemStack(Material.OBSIDIAN, ThreadLocalRandom.current().nextInt(2, 5)) },
            { ItemStack(Material.TNT, ThreadLocalRandom.current().nextInt(1, 3)) },
            { ItemStack(Material.ENDER_PEARL, 1) },
            { ItemStack(Material.GOLDEN_APPLE, ThreadLocalRandom.current().nextInt(1, 3)) },
            { ItemStack(Material.ARROW, ThreadLocalRandom.current().nextInt(4, 13)) },
            { ItemStack(Material.WOOL, ThreadLocalRandom.current().nextInt(8, 33)) },
            { ItemStack(Material.WOOD, ThreadLocalRandom.current().nextInt(8, 17)) },
            { ItemStack(Material.COOKIE, ThreadLocalRandom.current().nextInt(1, 4)) },
            { ItemStack(Material.WEB, ThreadLocalRandom.current().nextInt(2, 5)) }
        )
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
                item.type == Material.ENDER_CHEST &&
                    item.itemMeta?.displayName?.contains("Lucky Chest") == true
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

                val placeLoc = if (event.action == Action.RIGHT_CLICK_BLOCK)
                {
                    event.clickedBlock.location.add(0.0, 1.0, 0.0)
                }
                else
                {
                    val dir = player.location.direction.clone()
                    dir.setY(0).normalize()
                    player.location.clone().add(dir.multiply(2)).apply { y = blockY.toDouble() }
                }

                val block = world.getBlockAt(placeLoc)
                if (block.type != Material.AIR)
                {
                    player.sendMessage("${CC.RED}No room to place the Lucky Chest!")
                    return@handler
                }

                consumeOneFromHand(player)

                block.type = Material.CHEST
                block.setMetadata("bw_lucky_chest", FixedMetadataValue(plugin, true))
                block.setMetadata("placed", FixedMetadataValue(plugin, player.uniqueId))

                val chest = block.state as Chest
                val rng = ThreadLocalRandom.current()

                val lootCount = rng.nextInt(3, 6)
                val slots = (0 until 27).shuffled().take(lootCount)
                for ((idx, slot) in slots.withIndex())
                {
                    val lootItem = LOOT_TABLE[rng.nextInt(LOOT_TABLE.size)]()
                    chest.inventory.setItem(slot, lootItem)
                }

                player.playSound(player.location, Sound.CHEST_OPEN, 1.0f, 1.0f)
                player.sendMessage("${CC.GREEN}Lucky Chest placed! It will vanish in 30 seconds.")

                Schedulers
                    .sync()
                    .runLater({
                        if (block.type == Material.CHEST && block.hasMetadata("bw_lucky_chest"))
                        {
                            block.type = Material.AIR
                        }
                    }, CHEST_LIFETIME_TICKS)
                    .bindWith(lifecycle)
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
