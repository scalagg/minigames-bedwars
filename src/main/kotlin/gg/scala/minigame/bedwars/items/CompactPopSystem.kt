package gg.scala.minigame.bedwars.items

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.scala.minigame.bedwars.team.BedWarsTeamPalette
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import me.lucko.helper.Events
import me.lucko.helper.Helper
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.metadata.FixedMetadataValue

/**
 * @author Limo
 * @date 2/14/2026
 */
class CompactPopSystem(
    private val lifecycle: BedWarsLifecycle
)
{

    fun configure()
    {
        Events
            .subscribe(PlayerInteractEvent::class.java)
            .filter { event ->
                event.action == Action.RIGHT_CLICK_BLOCK || event.action == Action.RIGHT_CLICK_AIR
            }
            .filter { event ->
                val item = event.item ?: return@filter false
                item.type == Material.TRAPPED_CHEST &&
                    item.itemMeta?.displayName?.contains("Compact Pop-up Tower") == true
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

                val woolData = BedWarsTeamPalette.woolData(team).toByte()
                val baseBlock = player.location.block.getRelative(0, -1, 0)
                val direction = getCardinalDirection(player.location.yaw)

                consumeOneInHand(player)
                buildTower(baseBlock, direction, woolData, player)
            }
            .bindWith(lifecycle)
    }

    private fun buildTower(
        baseBlock: Block,
        direction: Direction,
        woolData: Byte,
        player: Player
    )
    {
        val blocks = getTowerBlocks(direction)
        val plugin = Helper.hostPlugin()
        val loc = baseBlock.location
        val counter = intArrayOf(0)

        val task = Schedulers.sync().runRepeating({ schedulerTask ->
            loc.world.playSound(loc, Sound.CHICKEN_EGG_POP, 1.0f, 0.5f)

            if (counter[0] >= blocks.size)
            {
                schedulerTask.close()
                return@runRepeating
            }

            placeBlock(baseBlock, blocks[counter[0]], woolData, player, plugin)

            if (counter[0] + 1 >= blocks.size)
            {
                schedulerTask.close()
                return@runRepeating
            }

            placeBlock(baseBlock, blocks[counter[0] + 1], woolData, player, plugin)
            counter[0] += 2
        }, 0L, 1L)

        task.bindWith(lifecycle)
    }

    private fun placeBlock(
        baseBlock: Block,
        entry: BlockEntry,
        woolData: Byte,
        player: Player,
        plugin: org.bukkit.plugin.Plugin
    )
    {
        val relative = baseBlock.getRelative(entry.x, entry.y, entry.z)
        if (relative.type != Material.AIR) return

        if (entry.ladder)
        {
            relative.type = Material.LADDER
            @Suppress("DEPRECATION")
            relative.data = entry.ladderData.toByte()
        } else
        {
            relative.type = Material.WOOL
            @Suppress("DEPRECATION")
            relative.data = woolData
        }

        relative.setMetadata("placed", FixedMetadataValue(plugin, player.uniqueId))
        lifecycle.game.placedBlocks.add(relative.location.toVector())
    }

    private fun getCardinalDirection(yaw: Float): Direction
    {
        val normalized = ((yaw % 360) + 360) % 360
        return when
        {
            normalized >= 315 || normalized < 45 -> Direction.SOUTH
            normalized >= 45 && normalized < 135 -> Direction.WEST
            normalized >= 135 && normalized < 225 -> Direction.NORTH
            else -> Direction.EAST
        }
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

    private fun getTowerBlocks(direction: Direction): List<BlockEntry>
    {
        return when (direction)
        {
            Direction.NORTH -> northBlocks()
            Direction.SOUTH -> southBlocks()
            Direction.EAST -> eastBlocks()
            Direction.WEST -> westBlocks()
        }
    }

    private fun northBlocks(): List<BlockEntry>
    {
        val b = mutableListOf<BlockEntry>()

        for (dy in 0..2)
        {
            b += w(-1, dy, -2); b += w(-2, dy, -1); b += w(-2, dy, 0)
            b += w(-1, dy, 1); b += w(0, dy, 1); b += w(1, dy, 1)
            b += w(2, dy, 0); b += w(2, dy, -1); b += w(1, dy, -2)
            b += l(0, dy, 0, 2)
        }

        for (dy in 3..4)
        {
            b += w(0, dy, -2)
            b += w(-1, dy, -2); b += w(-2, dy, -1); b += w(-2, dy, 0)
            b += w(-1, dy, 1); b += w(0, dy, 1); b += w(1, dy, 1)
            b += w(2, dy, 0); b += w(2, dy, -1); b += w(1, dy, -2)
            b += l(0, dy, 0, 2)
        }

        b += w(-2, 5, 1); b += w(-2, 5, 0); b += w(-2, 5, -1); b += w(-2, 5, -2)
        b += w(-1, 5, 1); b += w(-1, 5, 0); b += w(-1, 5, -1); b += w(-1, 5, -2)
        b += w(0, 5, 1); b += w(0, 5, -1); b += w(0, 5, -2)
        b += w(1, 5, 1); b += l(0, 5, 0, 2)
        b += w(1, 5, 0); b += w(1, 5, -1); b += w(1, 5, -2)
        b += w(2, 5, 1); b += w(2, 5, 0); b += w(2, 5, -1); b += w(2, 5, -2)

        b += w(-3, 5, -2); b += w(-3, 6, -2); b += w(-3, 7, -2)
        b += w(-3, 6, -1); b += w(-3, 6, 0)
        b += w(-3, 5, 1); b += w(-3, 6, 1); b += w(-3, 7, 1)
        b += w(-2, 5, 2); b += w(-2, 6, 2); b += w(-2, 7, 2)
        b += w(-1, 6, 2); b += w(0, 5, 2); b += w(0, 6, 2); b += w(0, 7, 2)
        b += w(1, 6, 2); b += w(2, 5, 2); b += w(2, 6, 2); b += w(2, 7, 2)
        b += w(3, 5, -2); b += w(3, 6, -2); b += w(3, 7, -2)
        b += w(3, 6, -1); b += w(3, 6, 0)
        b += w(3, 5, 1); b += w(3, 6, 1); b += w(3, 7, 1)
        b += w(-2, 5, -3); b += w(-2, 6, -3); b += w(-2, 7, -3)
        b += w(-1, 6, -3); b += w(0, 5, -3); b += w(0, 6, -3); b += w(0, 7, -3)
        b += w(1, 6, -3); b += w(2, 5, -3); b += w(2, 6, -3); b += w(2, 7, -3)
        return b
    }

    private fun southBlocks(): List<BlockEntry>
    {
        val b = mutableListOf<BlockEntry>()
        for (dy in 0..2)
        {
            b += w(1, dy, 2); b += w(2, dy, 1); b += w(2, dy, 0)
            b += w(1, dy, -1); b += w(0, dy, -1); b += w(-1, dy, -1)
            b += w(-2, dy, 0); b += w(-2, dy, 1); b += w(-1, dy, 2)
            b += l(0, dy, 0, 3)
        }
        for (dy in 3..4)
        {
            b += w(0, dy, 2)
            b += w(1, dy, 2); b += w(2, dy, 1); b += w(2, dy, 0)
            b += w(1, dy, -1); b += w(0, dy, -1); b += w(-1, dy, -1)
            b += w(-2, dy, 0); b += w(-2, dy, 1); b += w(-1, dy, 2)
            b += l(0, dy, 0, 3)
        }
        b += w(2, 5, -1); b += w(2, 5, 0); b += w(2, 5, 1); b += w(2, 5, 2)
        b += w(1, 5, -1); b += w(1, 5, 0); b += w(1, 5, 1); b += w(1, 5, 2)
        b += w(0, 5, -1); b += w(0, 5, 1); b += w(0, 5, 2)
        b += w(-1, 5, -1); b += l(0, 5, 0, 3)
        b += w(-1, 5, 0); b += w(-1, 5, 1); b += w(-1, 5, 2)
        b += w(-2, 5, -1); b += w(-2, 5, 0); b += w(-2, 5, 1); b += w(-2, 5, 2)
        b += w(3, 5, 2); b += w(3, 6, 2); b += w(3, 7, 2)
        b += w(3, 6, 1); b += w(3, 6, 0)
        b += w(3, 5, -1); b += w(3, 6, -1); b += w(3, 7, -1)
        b += w(2, 5, -2); b += w(2, 6, -2); b += w(2, 7, -2)
        b += w(1, 6, -2); b += w(0, 5, -2); b += w(0, 6, -2); b += w(0, 7, -2)
        b += w(-1, 6, -2); b += w(-2, 5, -2); b += w(-2, 6, -2); b += w(-2, 7, -2)
        b += w(-3, 5, 2); b += w(-3, 6, 2); b += w(-3, 7, 2)
        b += w(-3, 6, 1); b += w(-3, 6, 0)
        b += w(-3, 5, -1); b += w(-3, 6, -1); b += w(-3, 7, -1)
        b += w(2, 5, 3); b += w(2, 6, 3); b += w(2, 7, 3)
        b += w(1, 6, 3); b += w(0, 5, 3); b += w(0, 6, 3); b += w(0, 7, 3)
        b += w(-1, 6, 3); b += w(-2, 5, 3); b += w(-2, 6, 3); b += w(-2, 7, 3)
        return b
    }

    private fun eastBlocks(): List<BlockEntry>
    {
        val b = mutableListOf<BlockEntry>()
        for (dy in 0..2)
        {
            b += w(2, dy, -1); b += w(1, dy, -2); b += w(0, dy, -2)
            b += w(-1, dy, -1); b += w(-1, dy, 0); b += w(-1, dy, 1)
            b += w(0, dy, 2); b += w(1, dy, 2); b += w(2, dy, 1)
            b += l(0, dy, 0, 5)
        }
        for (dy in 3..4)
        {
            b += w(2, dy, 0)
            b += w(2, dy, -1); b += w(1, dy, -2); b += w(0, dy, -2)
            b += w(-1, dy, -1); b += w(-1, dy, 0); b += w(-1, dy, 1)
            b += w(0, dy, 2); b += w(1, dy, 2); b += w(2, dy, 1)
            b += l(0, dy, 0, 5)
        }
        b += w(-1, 5, -2); b += w(0, 5, -2); b += w(1, 5, -2); b += w(2, 5, -2)
        b += w(-1, 5, -1); b += w(0, 5, -1); b += w(1, 5, -1); b += w(2, 5, -1)
        b += w(-1, 5, 0); b += w(1, 5, 0); b += w(2, 5, 0)
        b += w(-1, 5, 1); b += l(0, 5, 0, 5)
        b += w(0, 5, 1); b += w(1, 5, 1); b += w(2, 5, 1)
        b += w(-1, 5, 2); b += w(0, 5, 2); b += w(1, 5, 2); b += w(2, 5, 2)
        b += w(2, 5, -3); b += w(2, 6, -3); b += w(2, 7, -3)
        b += w(1, 6, -3); b += w(0, 6, -3)
        b += w(-1, 5, -3); b += w(-1, 6, -3); b += w(-1, 7, -3)
        b += w(-2, 5, -2); b += w(-2, 6, -2); b += w(-2, 7, -2)
        b += w(-2, 6, -1); b += w(-2, 5, 0); b += w(-2, 6, 0); b += w(-2, 7, 0)
        b += w(-2, 6, 1); b += w(-2, 5, 2); b += w(-2, 6, 2); b += w(-2, 7, 2)
        b += w(2, 5, 3); b += w(2, 6, 3); b += w(2, 7, 3)
        b += w(1, 6, 3); b += w(0, 6, 3)
        b += w(-1, 5, 3); b += w(-1, 6, 3); b += w(-1, 7, 3)
        b += w(3, 5, -2); b += w(3, 6, -2); b += w(3, 7, -2)
        b += w(3, 6, -1); b += w(3, 5, 0); b += w(3, 6, 0); b += w(3, 7, 0)
        b += w(3, 6, 1); b += w(3, 5, 2); b += w(3, 6, 2); b += w(3, 7, 2)
        return b
    }

    private fun westBlocks(): List<BlockEntry>
    {
        val b = mutableListOf<BlockEntry>()
        for (dy in 0..2)
        {
            b += w(-2, dy, 1); b += w(-1, dy, 2); b += w(0, dy, 2)
            b += w(1, dy, 1); b += w(1, dy, 0); b += w(1, dy, -1)
            b += w(0, dy, -2); b += w(-1, dy, -2); b += w(-2, dy, -1)
            b += l(0, dy, 0, 4)
        }
        for (dy in 3..4)
        {
            b += w(-2, dy, 0)
            b += w(-2, dy, 1); b += w(-1, dy, 2); b += w(0, dy, 2)
            b += w(1, dy, 1); b += w(1, dy, 0); b += w(1, dy, -1)
            b += w(0, dy, -2); b += w(-1, dy, -2); b += w(-2, dy, -1)
            b += l(0, dy, 0, 4)
        }
        b += w(1, 5, 2); b += w(0, 5, 2); b += w(-1, 5, 2); b += w(-2, 5, 2)
        b += w(1, 5, 1); b += w(0, 5, 1); b += w(-1, 5, 1); b += w(-2, 5, 1)
        b += w(1, 5, 0); b += w(-1, 5, 0); b += w(-2, 5, 0)
        b += w(1, 5, -1); b += w(0, 5, -1); b += w(-1, 5, -1); b += w(-2, 5, -1)
        b += l(0, 5, 0, 4)
        b += w(1, 5, -2); b += w(0, 5, -2); b += w(-1, 5, -2); b += w(-2, 5, -2)
        b += w(-2, 5, 3); b += w(-2, 6, 3); b += w(-2, 7, 3)
        b += w(-1, 6, 3); b += w(0, 6, 3)
        b += w(1, 5, 3); b += w(1, 6, 3); b += w(1, 7, 3)
        b += w(2, 5, 2); b += w(2, 6, 2); b += w(2, 7, 2)
        b += w(2, 6, 1); b += w(2, 5, 0); b += w(2, 6, 0); b += w(2, 7, 0)
        b += w(2, 6, -1); b += w(2, 5, -2); b += w(2, 6, -2); b += w(2, 7, -2)
        b += w(-2, 5, -3); b += w(-2, 6, -3); b += w(-2, 7, -3)
        b += w(-1, 6, -3); b += w(0, 6, -3)
        b += w(1, 5, -3); b += w(1, 6, -3); b += w(1, 7, -3)
        b += w(-3, 5, 2); b += w(-3, 6, 2); b += w(-3, 7, 2)
        b += w(-3, 6, 1); b += w(-3, 5, 0); b += w(-3, 6, 0); b += w(-3, 7, 0)
        b += w(-3, 6, -1); b += w(-3, 5, -2); b += w(-3, 6, -2); b += w(-3, 7, -2)
        return b
    }

    private fun w(x: Int, y: Int, z: Int) = BlockEntry(x, y, z, false, 0)
    private fun l(x: Int, y: Int, z: Int, data: Int) = BlockEntry(x, y, z, true, data)

    private data class BlockEntry(
        val x: Int,
        val y: Int,
        val z: Int,
        val ladder: Boolean,
        val ladderData: Int
    )

    private enum class Direction
    {
        NORTH, SOUTH, EAST, WEST
    }
}
