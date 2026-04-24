package gg.scala.minigame.bedwars.shop

import gg.scala.minigame.bedwars.shared.shop.BedWarsAxeTier
import gg.scala.minigame.bedwars.shared.shop.BedWarsPickaxeTier

/**
 * @author Limo
 * @date 2/13/2026
 */
data class BedWarsPlayerShopState(
    var pickaxeLevel: Int = 0,
    var axeLevel: Int = 0,
    var hasShears: Boolean = false,
    var armorTier: BedWarsArmorTier = BedWarsArmorTier.Leather
)
{
    fun currentPickaxeTier() = BedWarsPickaxeTier.fromLevel(pickaxeLevel)
    fun nextPickaxeTier() = BedWarsPickaxeTier.nextFrom(pickaxeLevel)

    fun currentAxeTier() = BedWarsAxeTier.fromLevel(axeLevel)
    fun nextAxeTier() = BedWarsAxeTier.nextFrom(axeLevel)
}

/**
 * @author Limo
 * @date 2/13/2026
 */
enum class BedWarsArmorTier
{
    Leather,
    Chainmail,
    Iron,
    Diamond
}
