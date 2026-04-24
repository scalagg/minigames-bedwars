package gg.scala.minigame.bedwars.trap

import org.bukkit.Material

/**
 * @author Limo
 * @date 2/13/2026
 */
enum class BedWarsTrapType(
    val displayName: String,
    val icon: Material,
    val shortDescription: String
)
{
    ItsATrap(
        displayName = "It's a Trap!",
        icon = Material.TRIPWIRE_HOOK,
        shortDescription = "Inflicts Blindness and Slowness for 8 seconds."
    ),
    Alarm(
        displayName = "Alarm Trap",
        icon = Material.REDSTONE_TORCH_ON,
        shortDescription = "Reveals invisible players as well as their name and team."
    ),
    MinerFatigue(
        displayName = "Miner Fatigue Trap",
        icon = Material.IRON_PICKAXE,
        shortDescription = "Inflict Mining Fatigue for 10 seconds."
    ),
    CounterOffensive(
        displayName = "Counter-Offensive Trap",
        icon = Material.FEATHER,
        shortDescription = "Grants Speed II and Jump Boost II for 15 seconds to allied players near your base."
    )
}

/**
 * @author Limo
 * @date 2/13/2026
 */
enum class BedWarsTrapQueueResult
{
    Queued,
    QueueFull,
    AlreadyQueued
}
