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
import org.bukkit.entity.Enderman
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import java.util.UUID

/**
 * @author Limo
 * @date 2/18/2026
 */
class BlockShufflerSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val LIFETIME_TICKS = 20L * 15
        private const val SHUFFLE_INTERVAL_TICKS = 20L
        private const val SHUFFLE_RADIUS = 5
        private const val COOLDOWN_MS = 500L
    }

    private val activeEndermen = mutableMapOf<UUID, Enderman>()
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
                    item.itemMeta?.displayName?.contains("Block Shuffler") == true
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

                val existing = activeEndermen[player.uniqueId]
                if (existing != null && existing.isValid && !existing.isDead)
                {
                    existing.remove()
                }

                val world = lifecycle.game.arenaWorld ?: return@handler

                consumeOneFromHand(player)

                val spawnLoc = if (event.action == Action.RIGHT_CLICK_BLOCK)
                {
                    event.clickedBlock.location.add(0.5, 1.0, 0.5)
                }
                else
                {
                    player.location.clone()
                }

                val enderman = world.spawn(spawnLoc, Enderman::class.java)
                val teamColor = TeamVisuals.toChatColor(team)
                enderman.customName = "$teamColor${player.name}'s Shuffler"
                enderman.isCustomNameVisible = true

                activeEndermen[player.uniqueId] = enderman

                player.sendMessage("${CC.GREEN}Block Shuffler deployed! It will rearrange blocks for 15 seconds.")

                Schedulers
                    .sync()
                    .runRepeating({ task ->
                        if (enderman.isDead || !enderman.isValid || !lifecycle.game.state(GameState.Playing))
                        {
                            task.close()
                            return@runRepeating
                        }

                        shuffleBlocks(enderman)
                    }, SHUFFLE_INTERVAL_TICKS, SHUFFLE_INTERVAL_TICKS)
                    .bindWith(lifecycle)

                Schedulers
                    .sync()
                    .runLater({
                        if (enderman.isValid && !enderman.isDead)
                        {
                            enderman.remove()
                            activeEndermen.remove(player.uniqueId)
                        }
                    }, LIFETIME_TICKS)
                    .bindWith(lifecycle)
            }
            .bindWith(lifecycle)
    }

    private fun shuffleBlocks(enderman: Enderman)
    {
        val world = enderman.world
        val cx = enderman.location.blockX
        val cy = enderman.location.blockY
        val cz = enderman.location.blockZ

        val placedBlocks = mutableListOf<org.bukkit.block.Block>()
        for (x in (cx - SHUFFLE_RADIUS)..(cx + SHUFFLE_RADIUS))
        {
            for (y in (cy - SHUFFLE_RADIUS)..(cy + SHUFFLE_RADIUS))
            {
                for (z in (cz - SHUFFLE_RADIUS)..(cz + SHUFFLE_RADIUS))
                {
                    val block = world.getBlockAt(x, y, z)
                    if (block.type != Material.AIR && block.hasMetadata("placed"))
                    {
                        placedBlocks += block
                    }
                }
            }
        }

        if (placedBlocks.size < 2) return

        val shuffled = placedBlocks.shuffled()
        val a = shuffled[0]
        val b = shuffled[1]

        val typeA = a.type
        val dataA = a.data
        val typeB = b.type
        val dataB = b.data

        a.type = typeB
        a.data = dataB
        b.type = typeA
        b.data = dataA
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
        activeEndermen.values.forEach { enderman ->
            if (enderman.isValid && !enderman.isDead)
            {
                enderman.remove()
            }
        }
        activeEndermen.clear()
    }
}
