package gg.scala.minigame.bedwars.shop

import gg.tropic.practice.currency.ShopCurrency
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * @author Limo
 * @date 2/13/2026
 */
object BedWarsShopEconomy
{
    fun Player.currencyAmount(currency: ShopCurrency) = inventory.contents
        .filterNotNull()
        .filter { stack -> stack.type == currency.material }
        .sumOf { stack -> stack.amount }

    fun Player.canAfford(currency: ShopCurrency, cost: Int) =
        currencyAmount(currency) >= cost

    fun Player.tryCharge(currency: ShopCurrency, cost: Int): Boolean
    {
        if (!canAfford(currency, cost))
        {
            return false
        }

        currency.removeFrom(this, cost)
        updateInventory()
        return true
    }

    fun Player.giveOrDrop(item: ItemStack)
    {
        val remainder = inventory.addItem(item)
        remainder.values.forEach { extra ->
            world.dropItemNaturally(location, extra)
        }
        updateInventory()
    }
}
