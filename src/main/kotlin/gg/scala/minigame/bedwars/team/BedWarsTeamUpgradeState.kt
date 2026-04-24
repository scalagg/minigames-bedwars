package gg.scala.minigame.bedwars.team

import gg.scala.minigame.bedwars.trap.BedWarsTrapType

/**
 * @author Limo
 * @date 2/13/2026
 */
data class BedWarsTeamUpgradeState(
    var forgeLevel: Int = 1,
    var sharpness: Boolean = false,
    var protectionLevel: Int = 0,
    var hasteLevel: Int = 0,
    var healPool: Boolean = false,
    val queuedTraps: MutableList<BedWarsTrapType> = mutableListOf()
)
