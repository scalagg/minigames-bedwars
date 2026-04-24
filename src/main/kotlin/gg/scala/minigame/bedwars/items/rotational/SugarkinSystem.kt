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
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.EulerAngle
import java.util.UUID

/**
 * @author Limo
 * @date 2/18/2026
 */
class SugarkinSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val SPEED = 1.0
        private const val MAX_RANGE = 30
        private const val DAMAGE = 5.0
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
                item.type == Material.NETHER_STAR &&
                    item.itemMeta?.displayName?.contains("Sugarkin") == true
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

                consumeOneFromHand(player)

                val direction = player.eyeLocation.direction.normalize()
                val spawnLoc = player.eyeLocation.clone().add(direction.clone().multiply(1.5))

                val star = world.spawn(spawnLoc, ArmorStand::class.java)
                star.isVisible = false
                star.setGravity(false)
                star.setBasePlate(false)
                star.setArms(true)
                star.itemInHand = ItemStack(Material.NETHER_STAR)
                star.rightArmPose = EulerAngle(Math.PI / 2.0, 0.0, 0.0)

                var distanceTravelled = 0.0
                var alive = true

                Schedulers
                    .sync()
                    .runRepeating({ task ->
                        if (!alive)
                        {
                            task.close()
                            return@runRepeating
                        }

                        distanceTravelled += SPEED

                        val newLoc = star.location.add(direction.clone().multiply(SPEED))
                        star.teleport(newLoc)

                        for (entity in star.getNearbyEntities(0.5, 0.5, 0.5))
                        {
                            if (entity is Player && entity.uniqueId != player.uniqueId)
                            {
                                val targetTeam = lifecycle.game.getNullableTeam(entity)?.teamIdentifier?.label
                                val playerTeam = lifecycle.game.getNullableTeam(player)?.teamIdentifier?.label

                                if (targetTeam != playerTeam)
                                {
                                    entity.damage(DAMAGE, player)
                                    player.playSound(player.location, Sound.SUCCESSFUL_HIT, 1.0f, 1.0f)
                                    star.remove()
                                    alive = false
                                    task.close()
                                    return@runRepeating
                                }
                            }
                        }

                        if (distanceTravelled > MAX_RANGE)
                        {
                            star.remove()
                            alive = false
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
