package gg.scala.minigame.bedwars.items.rotational

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import gg.tropic.practice.games.player.SpectatingPlayerProvider
import me.lucko.helper.Events
import me.lucko.helper.Helper
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.ArmorStand
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
class BlackHoleSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val PULL_RADIUS = 8.0
        private const val PULL_STRENGTH = 0.35
        private const val LIFETIME_TICKS = 20L * 5
        private const val PULL_INTERVAL_TICKS = 5L
        private const val COOLDOWN_MS = 500L
    }

    private val activeHoles = mutableListOf<ArmorStand>()
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
                item.type == Material.ENDER_PORTAL_FRAME &&
                    item.itemMeta?.displayName?.contains("Black Hole") == true
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
                    ?.teamIdentifier
                    ?: return@handler

                val world = lifecycle.game.arenaWorld ?: return@handler

                consumeOneFromHand(player)

                val dir = player.eyeLocation.direction.clone().normalize()
                val spawnLoc = player.eyeLocation.clone().add(dir.multiply(3))

                val armorStand = world.spawn(spawnLoc, ArmorStand::class.java)
                armorStand.isVisible = false
                armorStand.setGravity(false)
                armorStand.customName = "${CC.DARK_PURPLE}${CC.BOLD}Black Hole"
                armorStand.isCustomNameVisible = true
                armorStand.isSmall = true

                activeHoles += armorStand

                player.sendMessage("${CC.DARK_PURPLE}Black Hole deployed!")

                Schedulers
                    .sync()
                    .runRepeating({ task ->
                        if (armorStand.isDead || !armorStand.isValid || !lifecycle.game.state(GameState.Playing))
                        {
                            task.close()
                            return@runRepeating
                        }

                        val holeLoc = armorStand.location

                        world.players
                            .filter { target ->
                                val targetTeam = lifecycle.game.getNullableTeam(target)
                                    ?.teamIdentifier
                                    ?: return@filter false

                                targetTeam != team &&
                                    !SpectatingPlayerProvider.spectating(target) &&
                                    target.location.distanceSquared(holeLoc) <= PULL_RADIUS * PULL_RADIUS
                            }
                            .forEach { target ->
                                val pullDir = holeLoc.toVector()
                                    .subtract(target.location.toVector())
                                    .normalize()
                                    .multiply(PULL_STRENGTH)

                                target.velocity = target.velocity.add(pullDir)
                            }

                        world.playSound(holeLoc, Sound.PORTAL, 0.5f, 0.5f)
                    }, PULL_INTERVAL_TICKS, PULL_INTERVAL_TICKS)
                    .bindWith(lifecycle)

                Schedulers
                    .sync()
                    .runLater({
                        if (armorStand.isValid && !armorStand.isDead)
                        {
                            armorStand.remove()
                            activeHoles -= armorStand
                        }
                    }, LIFETIME_TICKS)
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

    fun cleanup()
    {
        activeHoles.forEach { stand ->
            if (stand.isValid && !stand.isDead)
            {
                stand.remove()
            }
        }
        activeHoles.clear()
    }
}
