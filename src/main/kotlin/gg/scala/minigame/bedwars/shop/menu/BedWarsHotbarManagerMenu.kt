package gg.scala.minigame.bedwars.shop.menu

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.scala.minigame.bedwars.shared.hotbar.BedWarsHotbarCategory
import gg.scala.minigame.bedwars.shared.hotbar.BedWarsHotbarLayout
import gg.scala.minigame.bedwars.shared.profile.BedWarsProfile
import gg.scala.minigame.bedwars.shared.profile.BedWarsProfileService
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * @author Limo
 * @date 2/18/2026
 */
class BedWarsHotbarManagerMenu(
    private val lifecycle: BedWarsLifecycle
) : Menu("Hotbar Manager")
{
    companion object
    {
        private val categoryOrder = BedWarsHotbarCategory.configurable

        private val categorySlots = intArrayOf(10, 11, 12, 13, 14, 15, 16, 17)
        private val hotbarSlots = intArrayOf(45, 46, 47, 48, 49, 50, 51, 52, 53)

        private const val dividerSlot = 22
        private const val goBackSlot = 30
        private const val removeSlot = 31
        private const val resetSlot = 32
    }

    init
    {
        placeholder = true
        updateAfterClick = true
        keepBottomMechanics = false
    }

    override fun size(buttons: Map<Int, Button>) = 54

    private fun saveLayout(profile: BedWarsProfile, layout: MutableMap<String, Int>)
    {
        profile.updateHotbarLayout(layout)
        profile.save()
    }

    private fun stripName(item: ItemStack?): String
    {
        val raw = item
            ?.itemMeta
            ?.displayName
            ?: return ""

        return ChatColor
            .stripColor(raw)
            ?.trim()
            ?: ""
    }

    private fun categoryFromItem(item: ItemStack?): BedWarsHotbarCategory?
    {
        if (item == null || item.type == Material.AIR)
        {
            return null
        }

        val name = stripName(item)
        if (name.isNotEmpty())
        {
            BedWarsHotbarCategory.entries.firstOrNull { category ->
                category.displayName.equals(name, true)
            }?.let { match ->
                return match
            }
        }

        return BedWarsHotbarCategory.entries
            .firstOrNull { category -> category.icon == item.type }
            ?.takeIf { item.itemMeta?.hasDisplayName() == true }
    }

    private fun isRemoveCursor(item: ItemStack?): Boolean
    {
        if (item == null || item.type != Material.BARRIER)
        {
            return false
        }

        return stripName(item)
            .equals("Remove From Slot", true)
    }

    private fun hotbarIndexForMenuSlot(slot: Int): Int?
    {
        val index = hotbarSlots.indexOf(slot)
        return if (index == -1) null else index
    }

    private fun categoryDescription() = listOf(
        "${CC.GRAY}Drag this to a hotbar slot below to",
        "${CC.GRAY}favor that slot when purchasing an",
        "${CC.GRAY}item in this category or on spawn.",
        "",
        "${CC.YELLOW}Click to drag!"
    )

    private fun createCategoryCursor(category: BedWarsHotbarCategory) = ItemBuilder
        .of(category.icon)
        .name("${CC.GREEN}${category.displayName}")
        .setLore(categoryDescription())
        .build()

    private fun createRemoveCursor() = ItemBuilder
        .of(Material.BARRIER)
        .name("${CC.RED}Remove From Slot")
        .addToLore(
            "${CC.GRAY}Drag this to a hotbar slot below",
            "${CC.GRAY}to remove that slot preference."
        )
        .build()

    private fun strippedLore(item: ItemStack?): List<String>
    {
        val lore = item
            ?.itemMeta
            ?.lore
            ?: return emptyList()

        return lore.map { line ->
            ChatColor.stripColor(line)
                ?.trim()
                ?: ""
        }
    }

    private fun isTransientCursorItem(item: ItemStack?): Boolean
    {
        if (item == null || item.type == Material.AIR)
        {
            return false
        }

        val strippedName = stripName(item)
        if (strippedName.equals("Remove From Slot", true))
        {
            return strippedLore(item) == listOf(
                "Drag this to a hotbar slot below",
                "to remove that slot preference."
            )
        }

        val isCategoryName = BedWarsHotbarCategory.entries
            .any { category ->
                category.displayName.equals(strippedName, true)
            }

        if (!isCategoryName)
        {
            return false
        }

        return strippedLore(item) == listOf(
            "Drag this to a hotbar slot below to",
            "favor that slot when purchasing an",
            "item in this category or on spawn.",
            "",
            "Click to drag!"
        )
    }

    override fun acceptsInsertedItem(player: Player, itemStack: ItemStack, slot: Int): Boolean
    {
        val hotbarIndex = hotbarIndexForMenuSlot(slot)
            ?: return false

        val profile = BedWarsProfileService.find(player)
            ?: return false

        var layout = profile.resolveHotbarLayout().toMutableMap()

        if (isRemoveCursor(itemStack))
        {
            val assigned = BedWarsHotbarLayout.categoryAtSlot(layout, hotbarIndex)
                ?: return true

            layout = BedWarsHotbarLayout.removeCategory(layout, assigned)
            saveLayout(profile, layout)
            return true
        }

        val category = categoryFromItem(itemStack)
            ?: return false

        layout = BedWarsHotbarLayout.assignCategoryToSlot(layout, category, hotbarIndex)
        saveLayout(profile, layout)
        return true
    }

    override fun acceptsDraggedItems(player: Player, items: Map<Int, ItemStack>): Boolean
    {
        if (items.isEmpty())
        {
            return false
        }

        val hotbarIndexes = items.keys
            .mapNotNull(::hotbarIndexForMenuSlot)

        if (hotbarIndexes.size != items.size)
        {
            return false
        }

        val profile = BedWarsProfileService.find(player)
            ?: return false

        var layout = profile.resolveHotbarLayout().toMutableMap()

        val firstItem = items.values.firstOrNull()
            ?: return false

        if (isRemoveCursor(firstItem))
        {
            hotbarIndexes.forEach { hotbarIndex ->
                val assigned = BedWarsHotbarLayout.categoryAtSlot(layout, hotbarIndex)
                    ?: return@forEach

                layout = BedWarsHotbarLayout.removeCategory(layout, assigned)
            }

            saveLayout(profile, layout)
            return true
        }

        val category = categoryFromItem(firstItem)
            ?: return false

        val targetSlot = hotbarIndexes.maxOrNull()
            ?: return false

        layout = BedWarsHotbarLayout.assignCategoryToSlot(layout, category, targetSlot)
        saveLayout(profile, layout)
        return true
    }

    override fun onClose(player: Player, manualClose: Boolean)
    {
        var cleaned = false

        player.inventory.contents.forEachIndexed { index, item ->
            if (!isTransientCursorItem(item))
            {
                return@forEachIndexed
            }

            player.inventory.setItem(index, null)
            cleaned = true
        }

        if (cleaned)
        {
            player.updateInventory()
        }
    }

    override fun getButtons(player: Player): MutableMap<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()
        val profile = BedWarsProfileService.find(player)

        if (profile == null)
        {
            buttons[22] = ItemBuilder
                .of(Material.REDSTONE)
                .name("${CC.RED}Your profile is still loading")
                .addToLore("${CC.GRAY}Please wait a moment and try again.")
                .toButton()
            return buttons
        }

        val layout = profile.resolveHotbarLayout()

        categoryOrder.forEachIndexed { index, category ->
            buttons[categorySlots[index]] = ItemBuilder
                .of(category.icon)
                .name("${CC.GREEN}${category.displayName}")
                .setLore(categoryDescription())
                .toButton { _, _ ->
                    player.openInventory.cursor = createCategoryCursor(category)
                    player.updateInventory()
                }
        }

        buttons[dividerSlot] = ItemBuilder
            .of(Material.STAINED_GLASS_PANE)
            .data(7)
            .name("${CC.GRAY}↑ Categories")
            .addToLore("${CC.GRAY}↓ Hotbar")
            .toButton()

        hotbarSlots.forEachIndexed { hotbarIndex, menuSlot ->
            val assigned = BedWarsHotbarLayout.categoryAtSlot(layout, hotbarIndex)

            if (assigned == null)
            {
                buttons[menuSlot] = ItemBuilder
                    .of(Material.STAINED_GLASS_PANE)
                    .data(7)
                    .name(" ")
                    .toButton()
                return@forEachIndexed
            }

            buttons[menuSlot] = ItemBuilder
                .of(assigned.icon)
                .name("${CC.GREEN}${assigned.displayName}")
                .addToLore(
                    "${CC.GRAY}${assigned.displayName} items will prioritize this slot!",
                    "",
                    "${CC.YELLOW}Click to remove!"
                )
                .toButton { _, _ ->
                    val updated = BedWarsHotbarLayout.removeCategory(
                        layout.toMutableMap(),
                        assigned
                    )
                    saveLayout(profile, updated)
                    openMenu(player)
                }
        }

        buttons[goBackSlot] = ItemBuilder
            .of(Material.ARROW)
            .name("${CC.GREEN}Go Back")
            .addToLore("${CC.GRAY}To Item Shop")
            .toButton { _, _ ->
                BedWarsItemShopMenu(lifecycle).openMenu(player)
            }

        buttons[removeSlot] = ItemBuilder
            .of(Material.BARRIER)
            .name("${CC.RED}Remove From Slot")
            .addToLore(
                "${CC.GRAY}Drag this to a hotbar slot below",
                "${CC.GRAY}to remove that slot preference."
            )
            .toButton { _, _ ->
                player.openInventory.cursor = createRemoveCursor()
                player.updateInventory()
            }

        buttons[resetSlot] = ItemBuilder
            .of(Material.BARRIER)
            .name("${CC.RED}Reset to Default")
            .addToLore("${CC.GRAY}Reset your hotbar to the default.")
            .toButton { _, _ ->
                saveLayout(profile, BedWarsHotbarLayout.defaultLayout.toMutableMap())
                openMenu(player)
            }

        return buttons
    }
}
