package gg.scala.minigame.bedwars.items

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import gg.tropic.practice.games.team.TeamIdentifier
import gg.tropic.practice.visuals.TeamVisuals
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import org.bukkit.Material
import org.bukkit.entity.IronGolem
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import java.util.UUID

/**
 * @author Limo
 * @date 2/14/2026
 */
class DreamDefenderSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val LIFETIME_TICKS = 20L * 60 * 4
        private const val AGGRO_RANGE = 10.0
        private const val AGGRO_CHECK_TICKS = 10L
    }

    private val activeGolems = mutableMapOf<UUID, IronGolem>()

    fun configure()
    {
        Events
            .subscribe(PlayerInteractEvent::class.java)
            .filter { event ->
                event.action == Action.RIGHT_CLICK_BLOCK || event.action == Action.RIGHT_CLICK_AIR
            }
            .filter { event ->
                val item = event.item ?: return@filter false
                item.type == Material.MONSTER_EGG && item.durability.toInt() == 99
            }
            .filter { event ->
                val game = GameService.byPlayer(event.player)
                game?.identifier == lifecycle.game.identifier
            }
            .handler { event ->
                event.isCancelled = true

                val player = event.player
                if (!lifecycle.game.state(GameState.Playing))
                {
                    return@handler
                }

                val team = lifecycle.game.getNullableTeam(player)
                    ?.teamIdentifier
                    ?: return@handler

                val existing = activeGolems[player.uniqueId]
                if (existing != null && existing.isValid && !existing.isDead)
                {
                    player.sendMessage("${CC.RED}You already have an active Dream Defender!")
                    return@handler
                }

                val spawnLoc = if (event.action == Action.RIGHT_CLICK_BLOCK)
                {
                    event.clickedBlock.location.add(0.5, 1.0, 0.5)
                } else
                {
                    player.location.clone()
                }

                consumeOneInHand(player)

                val world = lifecycle.game.arenaWorld
                    ?: return@handler

                val golem = world.spawn(spawnLoc, IronGolem::class.java)
                golem.customName = "${TeamVisuals.toChatColor(team)}${player.name}'s Defender"
                golem.isCustomNameVisible = true
                golem.setPlayerCreated(true)

                activeGolems[player.uniqueId] = golem

                player.sendMessage("${CC.GREEN}Dream Defender spawned! It will last 4 minutes.")

                Schedulers
                    .sync()
                    .runRepeating({ task ->
                        if (golem.isDead || !golem.isValid || !lifecycle.game.state(GameState.Playing))
                        {
                            task.close()
                            return@runRepeating
                        }

                        val nearestEnemy = findNearestEnemy(golem, team)
                        if (nearestEnemy != null)
                        {
                            golem.target = nearestEnemy
                        }
                    }, AGGRO_CHECK_TICKS, AGGRO_CHECK_TICKS)
                    .bindWith(lifecycle)

                Schedulers
                    .sync()
                    .runLater({
                        if (golem.isValid && !golem.isDead)
                        {
                            golem.remove()
                            activeGolems.remove(player.uniqueId)
                        }
                    }, LIFETIME_TICKS)
                    .bindWith(lifecycle)
            }
            .bindWith(lifecycle)
    }

    private fun findNearestEnemy(golem: IronGolem, ownerTeam: TeamIdentifier): LivingEntity?
    {
        return golem.location.world.players
            .filter { target ->
                val targetTeam = lifecycle.game.getNullableTeam(target)
                    ?.teamIdentifier
                    ?: return@filter false

                targetTeam != ownerTeam &&
                    !gg.tropic.practice.games.player.SpectatingPlayerProvider.spectating(target) &&
                    target.location.distanceSquared(golem.location) <= AGGRO_RANGE * AGGRO_RANGE
            }
            .minByOrNull { it.location.distanceSquared(golem.location) }
    }

    private fun consumeOneInHand(player: Player)
    {
        val item = player.itemInHand ?: return
        if (item.amount <= 1)
        {
            player.itemInHand = null
        } else
        {
            item.amount -= 1
        }
    }

    fun cleanup()
    {
        activeGolems.values.forEach { golem ->
            if (golem.isValid && !golem.isDead)
            {
                golem.remove()
            }
        }
        activeGolems.clear()
    }
}
