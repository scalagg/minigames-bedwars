package gg.scala.minigame.bedwars.items.rotational

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import gg.tropic.practice.games.player.SpectatingPlayerProvider
import me.lucko.helper.Events
import me.lucko.helper.Helper
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.metadata.FixedMetadataValue

/**
 * @author Limo
 * @date 2/18/2026
 */
class FestivityMineSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    fun configure()
    {
        Events
            .subscribe(PlayerInteractEvent::class.java)
            .filter { event ->
                event.action == Action.RIGHT_CLICK_BLOCK
            }
            .filter { event ->
                val item = event.item ?: return@filter false
                item.type == Material.CARPET &&
                    item.itemMeta?.displayName?.contains("Festivity Mine") == true
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

                val world = lifecycle.game.arenaWorld ?: return@handler
                val plugin = Helper.hostPlugin()

                val team = lifecycle.game.getNullableTeam(player)
                    ?.teamIdentifier?.label ?: "none"

                val above = clickedBlock.getRelative(org.bukkit.block.BlockFace.UP)
                if (above.type != Material.AIR) {
                    player.sendMessage("${CC.RED}There's no space to place the mine!")
                    return@handler
                }

                consumeOneFromHand(player)

                above.type = Material.CARPET
                above.data = 7.toByte()

                above.setMetadata("bw_festivity_mine", FixedMetadataValue(plugin, player.uniqueId.toString()))
                above.setMetadata("bw_festivity_team", FixedMetadataValue(plugin, team))
                above.setMetadata("placed", FixedMetadataValue(plugin, player.uniqueId))

                player.sendMessage("${CC.GREEN}Festivity Mine placed!")

                Schedulers
                    .sync()
                    .runRepeating({ task ->
                        if (above.type != Material.CARPET || !above.hasMetadata("bw_festivity_mine"))
                        {
                            task.close()
                            return@runRepeating
                        }

                        if (!lifecycle.game.state(GameState.Playing))
                        {
                            task.close()
                            return@runRepeating
                        }

                        val mineTeam = above.getMetadata("bw_festivity_team").firstOrNull()?.asString() ?: "none"

                        val playersOnMine = world.players.filter { p ->
                            p.location.block == above &&
                                !SpectatingPlayerProvider.spectating(p) &&
                                (lifecycle.game.getNullableTeam(p)?.teamIdentifier?.label ?: "unknown") != mineTeam
                        }

                        if (playersOnMine.isNotEmpty())
                        {
                            above.type = Material.AIR
                            above.removeMetadata("bw_festivity_mine", plugin)
                            above.removeMetadata("bw_festivity_team", plugin)

                            for (i in 0 until 10)
                            {
                                val tnt = world.spawnEntity(above.location.add(0.5, 0.0, 0.5), EntityType.PRIMED_TNT) as TNTPrimed
                                tnt.fuseTicks = 0
                            }

                            task.close()
                        }
                    }, 0L, 1L)
                    .bindWith(lifecycle)
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
