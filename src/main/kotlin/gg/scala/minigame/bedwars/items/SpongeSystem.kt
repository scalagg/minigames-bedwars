package gg.scala.minigame.bedwars.items

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import me.lucko.helper.Events
import me.lucko.helper.Helper
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.metadata.FixedMetadataValue

/**
 * @author Limo
 * @date 2/14/2026
 */
class SpongeSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val ABSORB_RADIUS = 5
    }

    fun configure()
    {
        Events
            .subscribe(BlockPlaceEvent::class.java)
            .filter { event ->
                event.block.type == Material.SPONGE
            }
            .filter { event ->
                val game = GameService.byPlayer(event.player)
                game?.identifier == lifecycle.game.identifier
            }
            .filter { _ ->
                lifecycle.game.state(GameState.Playing)
            }
            .handler { event ->
                val sponge = event.block
                val world = sponge.world
                val cx = sponge.x
                val cy = sponge.y
                val cz = sponge.z

                var absorbed = 0
                for (x in (cx - ABSORB_RADIUS)..(cx + ABSORB_RADIUS))
                {
                    for (y in (cy - ABSORB_RADIUS)..(cy + ABSORB_RADIUS))
                    {
                        for (z in (cz - ABSORB_RADIUS)..(cz + ABSORB_RADIUS))
                        {
                            val block = world.getBlockAt(x, y, z)
                            if (block.type == Material.WATER || block.type == Material.STATIONARY_WATER)
                            {
                                block.type = Material.AIR
                                absorbed++
                            }
                        }
                    }
                }

                if (absorbed > 0)
                {
                    sponge.data = 1.toByte()
                }
            }
            .bindWith(lifecycle)
    }
}
