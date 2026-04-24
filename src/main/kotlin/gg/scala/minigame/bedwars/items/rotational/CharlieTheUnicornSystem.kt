package gg.scala.minigame.bedwars.items.rotational

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Horse
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import java.util.UUID

/**
 * @author Limo
 * @date 2/18/2026
 */
class CharlieTheUnicornSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val FLIGHT_DURATION_SECONDS = 10
        private const val COOLDOWN_MS = 1000L
        private const val HORIZONTAL_SPEED = 1.0
    }

    private val activeHorses = mutableMapOf<UUID, Horse>()
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
                item.type == Material.WHEAT &&
                    item.itemMeta?.displayName?.contains("Charlie the Unicorn") == true
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

                val existing = activeHorses[player.uniqueId]
                if (existing != null && existing.isValid && !existing.isDead)
                {
                    existing.remove()
                }

                val world = lifecycle.game.arenaWorld ?: return@handler

                consumeOneFromHand(player)

                val horse = world.spawn(player.location, Horse::class.java)
                horse.customName = "${CC.LIGHT_PURPLE}Charlie the Unicorn"
                horse.isCustomNameVisible = true
                horse.isTamed = true
                horse.variant = Horse.Variant.HORSE
                horse.color = Horse.Color.WHITE
                horse.style = Horse.Style.WHITE
                horse.setPassenger(player)

                activeHorses[player.uniqueId] = horse

                player.playSound(player.location, Sound.HORSE_GALLOP, 1.0f, 1.5f)
                player.sendMessage("${CC.LIGHT_PURPLE}Charlie the Unicorn has arrived! You can fly for 10 seconds!")

                var ticksElapsed = 0

                Schedulers
                    .sync()
                    .runRepeating({ task ->
                        ticksElapsed++

                        if (!horse.isValid || horse.isDead || !player.isOnline || player.vehicle != horse)
                        {
                            if (horse.isValid && !horse.isDead)
                            {
                                horse.eject()
                                horse.remove()
                            }
                            activeHorses.remove(player.uniqueId)
                            task.close()
                            return@runRepeating
                        }

                        if (ticksElapsed >= FLIGHT_DURATION_SECONDS * 20)
                        {
                            horse.eject()
                            horse.remove()
                            activeHorses.remove(player.uniqueId)

                            if (player.isOnline)
                            {
                                player.sendMessage("${CC.LIGHT_PURPLE}Charlie has left!")
                            }

                            task.close()
                            return@runRepeating
                        }

                        val direction = player.location.direction.clone()
                        val velocity = direction.multiply(HORIZONTAL_SPEED)

                        val pitch = player.location.pitch
                        if (pitch < -45)
                        {
                            velocity.y = velocity.y - 1.0
                        }

                        horse.velocity = velocity
                        horse.fallDistance = 0f
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
    }

    fun cleanup()
    {
        activeHorses.values.forEach { horse ->
            if (horse.isValid && !horse.isDead)
            {
                horse.eject()
                horse.remove()
            }
        }
        activeHorses.clear()
    }
}
