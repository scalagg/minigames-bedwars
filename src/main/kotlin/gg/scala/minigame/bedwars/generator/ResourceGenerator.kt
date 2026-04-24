package gg.scala.minigame.bedwars.generator

import org.bukkit.Location
import org.bukkit.Material
import gg.tropic.practice.games.team.TeamIdentifier

/**
 * @author Limo
 * @date 2/13/2026
 */
enum class ResourceGeneratorType(
    val material: Material,
    val startingIntervalSeconds: Long,
    val minimumIntervalSeconds: Long,
    val upgradeStepSeconds: Long,
    val maxNearbyItems: Int
)
{
    Iron(
        material = Material.IRON_INGOT,
        startingIntervalSeconds = 2L,
        minimumIntervalSeconds = 1L,
        upgradeStepSeconds = 1L,
        maxNearbyItems = 48
    ),
    Gold(
        material = Material.GOLD_INGOT,
        startingIntervalSeconds = 4L,
        minimumIntervalSeconds = 2L,
        upgradeStepSeconds = 1L,
        maxNearbyItems = 16
    ),
    Diamond(
        material = Material.DIAMOND,
        startingIntervalSeconds = 30L,
        minimumIntervalSeconds = 8L,
        upgradeStepSeconds = 4L,
        maxNearbyItems = 8
    ),
    Emerald(
        material = Material.EMERALD,
        startingIntervalSeconds = 45L,
        minimumIntervalSeconds = 12L,
        upgradeStepSeconds = 5L,
        maxNearbyItems = 6
    )
}

/**
 * @author Limo
 * @date 2/13/2026
 */
data class ResourceGenerator(
    val type: ResourceGeneratorType,
    val location: Location,
    val ownerTeam: TeamIdentifier? = null,
    var level: Int = 1,
    var intervalSeconds: Long = type.startingIntervalSeconds,
    var nextDropAt: Long = System.currentTimeMillis() + (type.startingIntervalSeconds * 1000L)
)
{
    fun shouldDrop(now: Long) = now >= nextDropAt

    fun markDropped(now: Long)
    {
        nextDropAt = now + (intervalSeconds * 1000L)
    }

    fun upgrade()
    {
        if (type.upgradeStepSeconds <= 0L)
        {
            return
        }

        level += 1
        intervalSeconds = (intervalSeconds - type.upgradeStepSeconds)
            .coerceAtLeast(type.minimumIntervalSeconds)
    }

    fun maxOut()
    {
        while (intervalSeconds > type.minimumIntervalSeconds)
        {
            upgrade()
        }
    }
}
