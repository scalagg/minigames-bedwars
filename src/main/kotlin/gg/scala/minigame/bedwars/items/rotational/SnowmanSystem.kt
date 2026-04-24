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
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.entity.Snowman
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import java.util.UUID

/**
 * @author Limo
 * @date 2/18/2026
 */
class SnowmanSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val LIFETIME_TICKS = 20L * 20
        private const val FIRE_INTERVAL_TICKS = 30L
        private const val AGGRO_RANGE = 12.0
        private const val COOLDOWN_MS = 500L
    }

    private val activeSnowmen = mutableMapOf<UUID, Snowman>()
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
                item.type == Material.PUMPKIN &&
                    item.itemMeta?.displayName?.contains("Snowman") == true
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

                val existing = activeSnowmen[player.uniqueId]
                if (existing != null && existing.isValid && !existing.isDead)
                {
                    existing.remove()
                }

                val world = lifecycle.game.arenaWorld ?: return@handler

                consumeOneFromHand(player)

                val spawnLoc = if (event.action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
                {
                    event.clickedBlock.location.add(0.5, 1.0, 0.5)
                }
                else
                {
                    player.location.clone()
                }

                val snowman = world.spawn(spawnLoc, Snowman::class.java)
                val teamColor = TeamVisuals.toChatColor(team)
                snowman.customName = "$teamColor${player.name}'s Snowman"
                snowman.isCustomNameVisible = true

                activeSnowmen[player.uniqueId] = snowman

                player.sendMessage("${CC.GREEN}Snowman deployed! It will last 20 seconds.")

                Schedulers
                    .sync()
                    .runRepeating({ task ->
                        if (snowman.isDead || !snowman.isValid || !lifecycle.game.state(GameState.Playing))
                        {
                            task.close()
                            return@runRepeating
                        }

                        val target = findNearestEnemy(snowman, team) ?: return@runRepeating

                        val direction = target.eyeLocation.toVector()
                            .subtract(snowman.eyeLocation.toVector())
                            .normalize()

                        val snowball = snowman.launchProjectile(Snowball::class.java)
                        snowball.velocity = direction.multiply(1.5)
                    }, FIRE_INTERVAL_TICKS, FIRE_INTERVAL_TICKS)
                    .bindWith(lifecycle)

                Schedulers
                    .sync()
                    .runLater({
                        if (snowman.isValid && !snowman.isDead)
                        {
                            snowman.remove()
                            activeSnowmen.remove(player.uniqueId)
                        }
                    }, LIFETIME_TICKS)
                    .bindWith(lifecycle)
            }
            .bindWith(lifecycle)
    }

    private fun findNearestEnemy(snowman: Snowman, ownerTeam: TeamIdentifier): LivingEntity?
    {
        return snowman.location.world.players
            .filter { target ->
                val targetTeam = lifecycle.game.getNullableTeam(target)
                    ?.teamIdentifier
                    ?: return@filter false

                targetTeam != ownerTeam &&
                    !SpectatingPlayerProvider.spectating(target) &&
                    target.location.distanceSquared(snowman.location) <= AGGRO_RANGE * AGGRO_RANGE
            }
            .minByOrNull { it.location.distanceSquared(snowman.location) }
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
        activeSnowmen.values.forEach { snowman ->
            if (snowman.isValid && !snowman.isDead)
            {
                snowman.remove()
            }
        }
        activeSnowmen.clear()
    }
}
