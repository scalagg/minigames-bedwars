package gg.scala.minigame.bedwars.items.rotational

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import gg.tropic.practice.games.player.SpectatingPlayerProvider
import gg.tropic.practice.games.team.TeamIdentifier
import gg.tropic.practice.visuals.TeamVisuals
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import org.bukkit.Material
import org.bukkit.entity.Chicken
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import java.util.UUID

/**
 * @author Limo
 * @date 2/18/2026
 */
class ChickenTurretSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val CHICKEN_COUNT = 3
        private const val LIFETIME_TICKS = 20L * 15
        private const val AGGRO_RANGE = 8.0
        private const val AGGRO_CHECK_TICKS = 10L
        private const val COOLDOWN_MS = 500L
    }

    private val activeChickens = mutableListOf<Chicken>()
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
                item.type == Material.MONSTER_EGG &&
                    item.itemMeta?.displayName?.contains("Chicken Turret") == true
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

                val teamColor = TeamVisuals.toChatColor(team)
                val spawnLoc = player.location.clone()

                for (i in 0 until CHICKEN_COUNT)
                {
                    val chicken = world.spawn(spawnLoc, Chicken::class.java)
                    chicken.customName = "$teamColor${player.name}'s Chicken"
                    chicken.isCustomNameVisible = true

                    activeChickens += chicken

                    Schedulers
                        .sync()
                        .runRepeating({ task ->
                            if (chicken.isDead || !chicken.isValid || !lifecycle.game.state(GameState.Playing))
                            {
                                task.close()
                                return@runRepeating
                            }

                            val nearestEnemy = findNearestEnemy(chicken, team)
                            if (nearestEnemy != null)
                            {
                                chicken.target = nearestEnemy
                            }
                        }, AGGRO_CHECK_TICKS, AGGRO_CHECK_TICKS)
                        .bindWith(lifecycle)

                    Schedulers
                        .sync()
                        .runLater({
                            if (chicken.isValid && !chicken.isDead)
                            {
                                chicken.remove()
                                activeChickens -= chicken
                            }
                        }, LIFETIME_TICKS)
                        .bindWith(lifecycle)
                }

                player.sendMessage("${CC.GREEN}Deployed $CHICKEN_COUNT chickens!")
            }
            .bindWith(lifecycle)
    }

    private fun findNearestEnemy(chicken: Chicken, ownerTeam: TeamIdentifier): LivingEntity?
    {
        return chicken.location.world.players
            .filter { target ->
                val targetTeam = lifecycle.game.getNullableTeam(target)
                    ?.teamIdentifier
                    ?: return@filter false

                targetTeam != ownerTeam &&
                    !SpectatingPlayerProvider.spectating(target) &&
                    target.location.distanceSquared(chicken.location) <= AGGRO_RANGE * AGGRO_RANGE
            }
            .minByOrNull { it.location.distanceSquared(chicken.location) }
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
        activeChickens.forEach { chicken ->
            if (chicken.isValid && !chicken.isDead)
            {
                chicken.remove()
            }
        }
        activeChickens.clear()
    }
}
