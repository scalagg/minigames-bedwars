package gg.scala.minigame.bedwars.chest

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import me.lucko.helper.Events
import net.evilblock.cubed.util.CC
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.Inventory
import java.util.UUID

/**
 * @author Limo
 * @date 2/14/2026
 */
class EnderChestSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val ENDER_CHEST_ROWS = 3
    }

    private val playerEnderChests = mutableMapOf<UUID, Inventory>()

    fun configure()
    {
        Events
            .subscribe(PlayerInteractEvent::class.java)
            .filter { event ->
                event.action == Action.RIGHT_CLICK_BLOCK
            }
            .filter { event ->
                event.clickedBlock?.type == Material.ENDER_CHEST
            }
            .filter { event ->
                val game = GameService.byPlayer(event.player)
                game?.identifier == lifecycle.game.identifier
            }
            .handler { event ->
                event.isCancelled = true

                if (!lifecycle.game.state(GameState.Playing))
                {
                    return@handler
                }

                val inventory = getOrCreateEnderChest(event.player)
                event.player.openInventory(inventory)
            }
            .bindWith(lifecycle)

        Events
            .subscribe(PlayerInteractEvent::class.java)
            .filter { event ->
                event.action == Action.LEFT_CLICK_BLOCK
            }
            .filter { event ->
                event.clickedBlock?.type == Material.ENDER_CHEST
            }
            .filter { event ->
                val game = GameService.byPlayer(event.player)
                game?.identifier == lifecycle.game.identifier
            }
            .handler { event ->
                event.isCancelled = true

                if (!lifecycle.game.state(GameState.Playing))
                {
                    return@handler
                }

                val player = event.player
                val heldItem = player.itemInHand

                if (heldItem == null || heldItem.type == Material.AIR)
                {
                    player.sendMessage("${CC.RED}You are not holding any item!")
                    return@handler
                }

                val inventory = getOrCreateEnderChest(player)
                val depositAmount = heldItem.amount
                val itemName = heldItem.itemMeta?.displayName
                    ?: heldItem.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                val remaining = inventory.addItem(heldItem)

                if (remaining.isEmpty())
                {
                    player.itemInHand = null
                    player.sendMessage("${CC.GREEN}Deposited ${CC.WHITE}${depositAmount}x $itemName${CC.GREEN} into your Ender Chest!")
                } else
                {
                    val leftover = remaining.values.first()
                    val deposited = depositAmount - leftover.amount
                    player.itemInHand = leftover
                    if (deposited > 0)
                    {
                        player.sendMessage("${CC.YELLOW}Deposited ${CC.WHITE}${deposited}x $itemName${CC.YELLOW} into your Ender Chest! (chest full)")
                    } else
                    {
                        player.sendMessage("${CC.RED}Your Ender Chest is full!")
                    }
                }

                player.playSound(player.location, Sound.CHEST_OPEN, 1.0f, 1.0f)
            }
            .bindWith(lifecycle)
    }

    private fun getOrCreateEnderChest(player: Player): Inventory
    {
        return playerEnderChests.getOrPut(player.uniqueId) {
            Bukkit.createInventory(
                null,
                ENDER_CHEST_ROWS * 9,
                "${CC.D_PURPLE}Ender Chest"
            )
        }
    }
}
