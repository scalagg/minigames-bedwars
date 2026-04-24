package gg.scala.minigame.bedwars.generator

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameState
import gg.tropic.practice.games.team.TeamIdentifier
import gg.tropic.practice.map.metadata.impl.MapGenMetadata
import me.lucko.helper.Schedulers
import net.evilblock.cubed.entity.EntityHandler
import net.evilblock.cubed.entity.hologram.updating.UpdatingHologramEntity
import net.evilblock.cubed.util.CC
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack
import org.bukkit.util.EulerAngle
import org.bukkit.util.Vector

/**
 * @author Limo
 * @date 2/13/2026
 */
class ResourceGeneratorSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    private val generators = mutableListOf<ResourceGenerator>()
    private val generatorHolograms = mutableMapOf<ResourceGenerator, UpdatingHologramEntity>()
    private val spinningArmorStands = mutableMapOf<ResourceGenerator, ArmorStand>()

    fun totalGenerators() = generators.size

    fun configure()
    {
        loadGeneratorsFromMetadata()
        createGeneratorHolograms()
        createSpinningItems()

        Schedulers
            .sync()
            .runRepeating({ _ ->
                if (!lifecycle.game.state(GameState.Playing))
                {
                    return@runRepeating
                }

                val world = lifecycle.game.arenaWorld
                    ?: return@runRepeating

                val now = System.currentTimeMillis()
                generators.forEach { generator ->
                    if (!generator.shouldDrop(now))
                    {
                        return@forEach
                    }

                    val nearby = world.getNearbyEntities(generator.location, 1.25, 1.25, 1.25)
                        .filterIsInstance<Item>()
                        .count { entity ->
                            entity.itemStack.type == generator.type.material
                        }

                    if (nearby < generator.type.maxNearbyItems)
                    {
                        world.dropItem(
                            generator.location,
                            ItemStack(generator.type.material, 1)
                        ).apply {
                            velocity = Vector(0.0, 0.0, 0.0)
                            pickupDelay = 0
                        }
                    }

                    generator.markDropped(now)
                }
            }, 20L, 20L)
            .bindWith(lifecycle)

        Schedulers
            .sync()
            .runRepeating({ _ ->
                spinningArmorStands.values.forEach { stand ->
                    if (stand.isDead)
                    {
                        return@forEach
                    }

                    val loc = stand.location
                    loc.yaw += 5.0f
                    stand.teleport(loc)
                }
            }, 1L, 1L)
            .bindWith(lifecycle)
    }

    fun upgrade(type: ResourceGeneratorType)
    {
        generators
            .filter { generator -> generator.type == type }
            .forEach(ResourceGenerator::upgrade)
    }

    fun maxOut(type: ResourceGeneratorType)
    {
        generators
            .filter { generator -> generator.type == type }
            .forEach(ResourceGenerator::maxOut)
    }

    fun maxOutAll()
    {
        generators.forEach(ResourceGenerator::maxOut)
    }

    fun upgradeTeamForge(team: TeamIdentifier)
    {
        generators
            .filter { generator ->
                generator.ownerTeam == team &&
                    (generator.type == ResourceGeneratorType.Iron || generator.type == ResourceGeneratorType.Gold)
            }
            .forEach(ResourceGenerator::upgrade)
    }

    fun cleanup()
    {
        generatorHolograms.values.forEach { hologram ->
            hologram.destroyForCurrentWatchers()
            EntityHandler.forgetEntity(hologram)
        }
        generatorHolograms.clear()

        spinningArmorStands.values.forEach { stand ->
            if (!stand.isDead)
            {
                stand.remove()
            }
        }
        spinningArmorStands.clear()
    }

    private fun createGeneratorHolograms()
    {
        generators
            .filter { it.type == ResourceGeneratorType.Diamond || it.type == ResourceGeneratorType.Emerald }
            .forEach { generator ->
                val hologramLoc = generator.location.clone().add(0.0, 1.8, 0.0)

                val hologram = object : UpdatingHologramEntity(
                    text = generatorTitle(generator),
                    location = hologramLoc
                )
                {
                    override fun getNewLines(): List<String>
                    {
                        val typeColor = generatorColor(generator)
                        val typeName = generator.type.name
                        val now = System.currentTimeMillis()
                        val secondsLeft = ((generator.nextDropAt - now) / 1000L).coerceAtLeast(0)

                        return listOf(
                            "$typeColor${CC.BOLD}$typeName",
                            "${CC.YELLOW}Tier ${toRoman(generator.level)}",
                            "${CC.YELLOW}Spawns in ${CC.RED}${secondsLeft}s"
                        )
                    }

                    override fun getTickInterval(): Long = 400L 
                }

                hologram.persistent = false
                hologram.initializeData()
                hologram.forceUpdate()

                EntityHandler.trackEntity(hologram)
                generatorHolograms[generator] = hologram
            }
    }

    private fun createSpinningItems()
    {
        val world = lifecycle.game.arenaWorld
            ?: return

        generators
            .filter { it.type == ResourceGeneratorType.Diamond || it.type == ResourceGeneratorType.Emerald }
            .forEach { generator ->
                val spawnLoc = generator.location.clone().add(0.0, 0.4, 0.0)
                val blockMaterial = when (generator.type)
                {
                    ResourceGeneratorType.Diamond -> Material.DIAMOND_BLOCK
                    ResourceGeneratorType.Emerald -> Material.EMERALD_BLOCK
                    else -> return@forEach
                }

                val stand = world.spawn(spawnLoc, ArmorStand::class.java)
                stand.isVisible = false
                stand.setGravity(false)
                stand.isSmall = false
                stand.setBasePlate(false)
                stand.isMarker = true
                stand.helmet = ItemStack(blockMaterial)
                stand.headPose = EulerAngle(0.0, 0.0, 0.0)

                spinningArmorStands[generator] = stand
            }
    }

    private fun generatorColor(generator: ResourceGenerator): String
    {
        return if (generator.type == ResourceGeneratorType.Diamond) "${CC.AQUA}" else "${CC.D_GREEN}"
    }

    private fun generatorTitle(generator: ResourceGenerator): String
    {
        val color = generatorColor(generator)
        return "$color${CC.BOLD}${generator.type.name}"
    }

    private fun toRoman(level: Int): String = when (level)
    {
        1 -> "I"
        2 -> "II"
        3 -> "III"
        4 -> "IV"
        5 -> "V"
        else -> level.toString()
    }

    private fun loadGeneratorsFromMetadata()
    {
        val world = lifecycle.game.arenaWorld
            ?: return

        val mapGenMetadata = lifecycle.game.map.metadata.metadata
            .filterIsInstance<MapGenMetadata>()

        mapGenMetadata.forEach { metadata ->
            metadata.locations.forEach { (group, positions) ->
                val groupID = group.lowercase()

                when (groupID)
                {
                    "iron" -> positions.forEach { position ->
                        generators += ResourceGenerator(
                            ResourceGeneratorType.Iron,
                            position.toLocation(world).add(0.0, 0.4, 0.0),
                            ownerTeam = null
                        )
                    }

                    "gold" -> positions.forEach { position ->
                        generators += ResourceGenerator(
                            ResourceGeneratorType.Gold,
                            position.toLocation(world).add(0.0, 0.4, 0.0),
                            ownerTeam = null
                        )
                    }

                    "diamond" -> positions.forEach { position ->
                        generators += ResourceGenerator(
                            ResourceGeneratorType.Diamond,
                            position.toLocation(world).add(0.0, 0.4, 0.0),
                            ownerTeam = null
                        )
                    }

                    "emerald" -> positions.forEach { position ->
                        generators += ResourceGenerator(
                            ResourceGeneratorType.Emerald,
                            position.toLocation(world).add(0.0, 0.4, 0.0),
                            ownerTeam = null
                        )
                    }

                    else ->
                    {
                        val team = TeamIdentifier.ID[groupID.uppercase()]
                            ?: return@forEach

                        positions.forEach { position ->
                            generators += ResourceGenerator(
                                ResourceGeneratorType.Iron,
                                position.toLocation(world).add(0.0, 0.4, 0.0),
                                ownerTeam = team
                            )

                            generators += ResourceGenerator(
                                ResourceGeneratorType.Gold,
                                position.toLocation(world).add(0.0, 0.4, 0.0),
                                ownerTeam = team
                            )
                        }
                    }
                }
            }
        }
    }
}
