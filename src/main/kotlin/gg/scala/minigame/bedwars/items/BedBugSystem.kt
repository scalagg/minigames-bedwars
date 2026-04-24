package gg.scala.minigame.bedwars.items

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import gg.tropic.practice.games.player.SpectatingPlayerProvider
import gg.tropic.practice.visuals.TeamVisuals
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import org.bukkit.ChatColor
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Silverfish
import org.bukkit.entity.Snowball
import org.bukkit.event.entity.ProjectileHitEvent
import java.util.UUID

/**
 * @author Limo
 * @date 2/14/2026
 */
class BedBugSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val LIFETIME_TICKS = 20L * 15
        private const val AGGRO_RANGE = 8.0
        private const val AGGRO_CHECK_TICKS = 10L
    }

    private val activeSilverfish = mutableListOf<Silverfish>()

    fun configure()
    {
        Events
            .subscribe(ProjectileHitEvent::class.java)
            .filter { event ->
                event.entity is Snowball
            }
            .filter { event ->
                val shooter = (event.entity as Snowball).shooter
                shooter is Player && GameService.byPlayer(shooter)?.identifier == lifecycle.game.identifier
            }
            .handler { event ->
                val snowball = event.entity as Snowball
                val shooter = snowball.shooter as Player

                if (!lifecycle.game.state(GameState.Playing))
                {
                    return@handler
                }

                val team = lifecycle.game.getNullableTeam(shooter)
                    ?.teamIdentifier
                    ?: return@handler

                val world = lifecycle.game.arenaWorld
                    ?: return@handler

                val spawnLoc = snowball.location.clone()
                val silverfish = world.spawnEntity(spawnLoc, EntityType.SILVERFISH) as Silverfish
                val teamColor = TeamVisuals.toChatColor(team)
                silverfish.customName = "$teamColor${shooter.name}'s Bed Bug"
                silverfish.isCustomNameVisible = true

                activeSilverfish += silverfish

                Schedulers
                    .sync()
                    .runRepeating({ task ->
                        if (silverfish.isDead || !silverfish.isValid || !lifecycle.game.state(GameState.Playing))
                        {
                            task.close()
                            return@runRepeating
                        }

                        val nearestEnemy = findNearestEnemy(silverfish, team)
                        if (nearestEnemy != null)
                        {
                            silverfish.target = nearestEnemy
                        }
                    }, AGGRO_CHECK_TICKS, AGGRO_CHECK_TICKS)
                    .bindWith(lifecycle)

                Schedulers
                    .sync()
                    .runLater({
                        if (silverfish.isValid && !silverfish.isDead)
                        {
                            silverfish.remove()
                            activeSilverfish -= silverfish
                        }
                    }, LIFETIME_TICKS)
                    .bindWith(lifecycle)
            }
            .bindWith(lifecycle)
    }

    private fun findNearestEnemy(
        silverfish: Silverfish,
        ownerTeam: gg.tropic.practice.games.team.TeamIdentifier
    ): LivingEntity?
    {
        return silverfish.location.world.players
            .filter { target ->
                val targetTeam = lifecycle.game.getNullableTeam(target)
                    ?.teamIdentifier
                    ?: return@filter false

                targetTeam != ownerTeam &&
                    !SpectatingPlayerProvider.spectating(target) &&
                    target.location.distanceSquared(silverfish.location) <= AGGRO_RANGE * AGGRO_RANGE
            }
            .minByOrNull { it.location.distanceSquared(silverfish.location) }
    }

    fun cleanup()
    {
        activeSilverfish.forEach { silverfish ->
            if (silverfish.isValid && !silverfish.isDead)
            {
                silverfish.remove()
            }
        }
        activeSilverfish.clear()
    }
}
