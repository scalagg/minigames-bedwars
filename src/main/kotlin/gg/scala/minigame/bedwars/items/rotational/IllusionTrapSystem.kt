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
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.metadata.FixedMetadataValue
import java.util.UUID

/**
 * @author Limo
 * @date 2/18/2026
 */
class IllusionTrapSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val TRAP_LIFETIME_TICKS = 20L * 15
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
                item.type == Material.TRIPWIRE_HOOK &&
                    item.itemMeta?.displayName?.contains("Illusion Trap") == true
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
                    ?.teamIdentifier?.label ?: return@handler

                val world = lifecycle.game.arenaWorld ?: return@handler
                val plugin = Helper.hostPlugin()

                val clickedBlock = event.clickedBlock
                val baseLoc = clickedBlock.location.add(0.0, 1.0, 0.0)

                consumeOneFromHand(player)

                val placedBlocks = mutableListOf<org.bukkit.block.Block>()
                for (dx in -1..1)
                {
                    for (dz in -1..1)
                    {
                        val block = world.getBlockAt(
                            baseLoc.blockX + dx,
                            baseLoc.blockY,
                            baseLoc.blockZ + dz
                        )
                        if (block.type == Material.AIR)
                        {
                            block.type = Material.STAINED_GLASS
                            block.data = 15.toByte() 
                            block.setMetadata("bw_illusion_trap", FixedMetadataValue(plugin, team))
                            block.setMetadata("placed", FixedMetadataValue(plugin, player.uniqueId))
                            placedBlocks += block
                        }
                    }
                }

                player.playSound(player.location, Sound.DIG_STONE, 1.0f, 1.5f)
                player.sendMessage("${CC.GREEN}Illusion Trap placed! Enemy players will fall through.")

                if (placedBlocks.isNotEmpty())
                {
                    Schedulers
                        .sync()
                        .runLater({
                            placedBlocks.forEach { block ->
                                if (block.hasMetadata("bw_illusion_trap"))
                                {
                                    block.type = Material.AIR
                                }
                            }
                        }, TRAP_LIFETIME_TICKS)
                        .bindWith(lifecycle)
                }
            }
            .bindWith(lifecycle)

        Events
            .subscribe(org.bukkit.event.player.PlayerMoveEvent::class.java)
            .filter { event ->
                val game = GameService.byPlayer(event.player)
                game?.identifier == lifecycle.game.identifier
            }
            .filter { event ->
                lifecycle.game.state(GameState.Playing)
            }
            .handler { event ->
                val player = event.player
                val playerTeam = lifecycle.game.getNullableTeam(player)
                    ?.teamIdentifier?.label ?: return@handler

                val below = player.location.clone().subtract(0.0, 0.3, 0.0).block
                if (!below.hasMetadata("bw_illusion_trap")) return@handler

                val trapTeam = below.getMetadata("bw_illusion_trap").firstOrNull()?.asString() ?: return@handler

                if (trapTeam == playerTeam) return@handler

                below.type = Material.AIR
                player.playSound(player.location, Sound.GLASS, 1.0f, 0.5f)
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
