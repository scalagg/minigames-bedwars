package gg.scala.minigame.bedwars.items.rotational

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import me.lucko.helper.Events
import net.evilblock.cubed.util.CC
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

/**
 * @author Limo
 * @date 2/18/2026
 */
class UnstableTeleportationSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val COOLDOWN_MS = 500L
        private const val MAX_ATTEMPTS = 50
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
                item.type == Material.EYE_OF_ENDER &&
                    item.itemMeta?.displayName?.contains("Unstable") == true
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

                val origin = player.location
                val rng = ThreadLocalRandom.current()

                var destination: Location? = null
                for (i in 0 until MAX_ATTEMPTS)
                {
                    val dx = rng.nextInt(-40, 41)
                    val dz = rng.nextInt(-40, 41)
                    val x = origin.blockX + dx
                    val z = origin.blockZ + dz

                    val highestY = world.getHighestBlockYAt(x, z)
                    if (highestY < 1) continue

                    val candidate = Location(world, x + 0.5, highestY + 1.0, z + 0.5, origin.yaw, origin.pitch)
                    val blockBelow = world.getBlockAt(x, highestY, z)

                    if (blockBelow.type.isSolid &&
                        blockBelow.type != Material.LAVA &&
                        blockBelow.type != Material.STATIONARY_LAVA)
                    {
                        destination = candidate
                        break
                    }
                }

                if (destination == null)
                {
                    player.sendMessage("${CC.RED}Teleportation failed! No safe location found.")
                    return@handler
                }

                consumeOneFromHand(player)

                player.playSound(player.location, Sound.ENDERMAN_TELEPORT, 1.0f, 1.0f)
                player.teleport(destination)
                player.playSound(player.location, Sound.ENDERMAN_TELEPORT, 1.0f, 1.2f)
                player.sendMessage("${CC.YELLOW}Whoosh! ${CC.GREEN}You teleported to a random location!")
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
