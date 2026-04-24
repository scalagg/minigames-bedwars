package gg.scala.minigame.bedwars.shop.menu

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.scala.minigame.bedwars.shop.BedWarsArmorTier
import gg.scala.minigame.bedwars.shared.hotbar.BedWarsHotbarCategory
import gg.scala.minigame.bedwars.shared.hotbar.BedWarsHotbarLayout
import gg.scala.minigame.bedwars.shared.profile.BedWarsProfileService
import gg.scala.minigame.bedwars.shared.rotation.RotationalItemDefinition
import gg.scala.minigame.bedwars.shared.rotation.RotationalScheduleService
import gg.scala.minigame.bedwars.shared.shop.BedWarsShopCatalog
import gg.scala.minigame.bedwars.shared.shop.BedWarsShopCategory
import gg.scala.minigame.bedwars.shared.shop.BedWarsShopEntry
import gg.scala.minigame.bedwars.shared.shop.BedWarsShopPurchaseType
import gg.scala.minigame.bedwars.shared.shop.bedWarsQuickBuyLayoutResolved
import gg.scala.minigame.bedwars.shared.shop.setBedWarsQuickBuyLayout
import gg.scala.minigame.bedwars.shop.BedWarsShopEconomy.canAfford
import gg.scala.minigame.bedwars.shop.BedWarsShopEconomy.giveOrDrop
import gg.scala.minigame.bedwars.shop.BedWarsShopEconomy.tryCharge
import gg.scala.minigame.bedwars.team.BedWarsTeamPalette
import gg.tropic.practice.currency.ShopCurrency
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * @author Limo
 * @date 2/13/2026
 */
class BedWarsItemShopMenu(
    private val lifecycle: BedWarsLifecycle,
    private val category: BedWarsShopCategory = BedWarsShopCategory.QuickBuy
) : Menu("Shop" + "\u00A7r".repeat(category.ordinal))
{
    companion object
    {
        private val ITEM_SLOTS = intArrayOf(
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        )

        private data class ShopTab(
            val slot: Int,
            val icon: Material,
            val label: String,
            val category: BedWarsShopCategory? = null,
            val disabledMessage: String? = null
        )

        private val SHOP_TABS = listOf(
            ShopTab(0, Material.NETHER_STAR, "Quick Buy", BedWarsShopCategory.QuickBuy),
            ShopTab(1, Material.STAINED_CLAY, "Blocks", BedWarsShopCategory.Blocks),
            ShopTab(2, Material.GOLD_SWORD, "Melee", BedWarsShopCategory.Weapons),
            ShopTab(3, Material.CHAINMAIL_BOOTS, "Armor", BedWarsShopCategory.Armor),
            ShopTab(4, Material.STONE_PICKAXE, "Tools", BedWarsShopCategory.Tools),
            ShopTab(5, Material.BOW, "Ranged", BedWarsShopCategory.Ranged),
            ShopTab(6, Material.BREWING_STAND_ITEM, "Potions", BedWarsShopCategory.Potions),
            ShopTab(7, Material.TNT, "Utility", BedWarsShopCategory.Utility),
            ShopTab(
                slot = 8,
                icon = Material.REDSTONE_TORCH_ON,
                label = "Rotating Items",
                category = BedWarsShopCategory.Rotational
            )
        )
    }

    init
    {
        placeholder = false
    }

    override fun size(buttons: Map<Int, Button>) = 54

    override fun getButtons(player: Player): MutableMap<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        buildCategoryTabs(player, buttons)
        buildCategorySeparator(buttons)
        buildItemGrid(player, buttons)
        buildBottomButtons(player, buttons)

        return buttons
    }

    private fun buildCategoryTabs(player: Player, buttons: MutableMap<Int, Button>)
    {
        SHOP_TABS.forEach { tab ->
            val selected = tab.category == category

            val icon = ItemBuilder
                .of(tab.icon)
                .name(
                    if (tab.category == BedWarsShopCategory.QuickBuy)
                        "${CC.AQUA}${tab.label}"
                    else
                        "${CC.GREEN}${tab.label}"
                )
                .addToLore(
                    if (selected) "${CC.D_GRAY}Currently viewing" else "${CC.YELLOW}Click to view!"
                )
                .apply {
                    if (tab.category == BedWarsShopCategory.QuickBuy)
                    {
                        glow()
                    }
                }

            buttons[tab.slot] = icon.toButton { _, _ ->
                if (tab.category == null)
                {
                    player.sendMessage("${CC.RED}${tab.disabledMessage ?: "This category is unavailable."}")
                    player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
                    return@toButton
                }

                if (tab.category != category)
                {
                    BedWarsItemShopMenu(lifecycle, tab.category).openMenu(player)
                }
            }
        }
    }

    private fun buildCategorySeparator(buttons: MutableMap<Int, Button>)
    {
        val selectedSlot = SHOP_TABS
            .firstOrNull { it.category == category }
            ?.slot
            ?: 0

        for (index in 0..8)
        {
            buttons[9 + index] = ItemBuilder
                .of(Material.STAINED_GLASS_PANE)
                .data(if (index == selectedSlot) 5 else 7)
                .name("${CC.D_GRAY}^ ${CC.GRAY}Categories")
                .addToLore("${CC.D_GRAY}v ${CC.GRAY}Items")
                .toButton()
        }
    }

    private fun buildBottomButtons(player: Player, buttons: MutableMap<Int, Button>)
    {
        buttons[45] = ItemBuilder
            .of(Material.COMPASS)
            .name("${CC.GREEN}Tracker")
            .addToLore(
                "${CC.GRAY}Track enemy teams using",
                "${CC.GRAY}your compass!",
                "",
                "${CC.GRAY}Cost: ${CC.D_GREEN}2 Emeralds ${CC.GRAY}per use",
                "",
                "${CC.YELLOW}Click to open!"
            )
            .toButton { _, _ ->
                val team = lifecycle.game.getNullableTeam(player)
                    ?.teamIdentifier
                    ?: return@toButton

                lifecycle.trackerSystem.openTrackerMenu(player, team)
            }

        buttons[53] = ItemBuilder
            .of(Material.BLAZE_POWDER)
            .name("${CC.GREEN}Hotbar Manager")
            .addToLore(
                "${CC.GRAY}Customize preferred slots",
                "${CC.GRAY}for purchased items.",
                "",
                "${CC.YELLOW}Click to open!"
            )
            .toButton { _, _ ->
                BedWarsHotbarManagerMenu(lifecycle).openMenu(player)
            }
    }

    private fun entriesFor(player: Player): List<BedWarsShopEntry?>
    {
        if (category == BedWarsShopCategory.Rotational)
        {
            return RotationalScheduleService.activeItems().map { def ->
                BedWarsShopEntry(
                    id = "rotational_${def.id}",
                    displayName = def.displayName,
                    category = BedWarsShopCategory.Rotational,
                    icon = def.icon,
                    currency = def.currency,
                    cost = def.cost,
                    amount = def.amount,
                    data = def.iconData,
                    quickBuyEligible = false,
                    purchaseType = BedWarsShopPurchaseType.Rotational
                )
            }
        }

        if (category != BedWarsShopCategory.QuickBuy)
        {
            return BedWarsShopCatalog.categoryEntries(category)
                .take(ITEM_SLOTS.size)
        }

        val profile = BedWarsProfileService.find(player)
        val layout = profile
            ?.bedWarsQuickBuyLayoutResolved()
            ?: BedWarsShopCatalog.defaultQuickBuyLayout.toMutableList()

        return layout
            .take(ITEM_SLOTS.size)
            .map { entryId -> BedWarsShopCatalog.byId[entryId] }
    }

    private fun buildItemGrid(player: Player, buttons: MutableMap<Int, Button>)
    {
        val entries = entriesFor(player)

        entries.forEachIndexed { index, entry ->
            val slot = ITEM_SLOTS[index]

            if (entry == null)
            {
                if (category == BedWarsShopCategory.QuickBuy)
                {
                    buttons[slot] = ItemBuilder
                        .of(Material.STAINED_GLASS_PANE)
                        .data(14)
                        .name("${CC.RED}No Item")
                        .addToLore(
                            "${CC.GRAY}Shift-click items in other",
                            "${CC.GRAY}categories to add them here."
                        )
                        .toButton()
                }
                return@forEachIndexed
            }

            val quickBuyIndex = if (category == BedWarsShopCategory.QuickBuy) index else null

            buttons[slot] = object : Button()
            {
                override fun getButtonItem(p0: Player) = buildEntryItem(p0, entry)

                override fun clicked(clicker: Player, slot: Int, clickType: ClickType, view: InventoryView)
                {
                    if (clickType.isShiftClick && handleQuickBuyShiftClick(clicker, entry, quickBuyIndex))
                    {
                        return
                    }

                    val (_, costReference) = displayCost(entry, clicker)
                    handlePurchase(clicker, entry, costReference)
                }
            }
        }
    }

    private fun handleQuickBuyShiftClick(
        player: Player,
        entry: BedWarsShopEntry,
        quickBuyIndex: Int?
    ): Boolean
    {
        val profile = BedWarsProfileService.find(player)
        if (profile == null)
        {
            player.sendMessage("${CC.RED}Your profile is still loading.")
            return true
        }

        val layout = profile.bedWarsQuickBuyLayoutResolved()

        if (category == BedWarsShopCategory.QuickBuy)
        {
            if (quickBuyIndex == null || quickBuyIndex !in layout.indices)
            {
                return true
            }

            if (layout[quickBuyIndex] == BedWarsShopCatalog.EMPTY_SLOT_ID)
            {
                return true
            }

            layout[quickBuyIndex] = BedWarsShopCatalog.EMPTY_SLOT_ID
            profile.setBedWarsQuickBuyLayout(layout)
            profile.save()

            player.sendMessage("${CC.GREEN}Removed ${CC.WHITE}${entry.displayName}${CC.GREEN} from Quick Buy.")
            player.playSound(player.location, Sound.NOTE_PLING, 1.0f, 2.0f)
            openMenu(player)
            return true
        }

        if (!entry.quickBuyEligible)
        {
            player.sendMessage("${CC.RED}This item cannot be added to Quick Buy.")
            player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
            return true
        }

        if (layout.contains(entry.id))
        {
            player.sendMessage("${CC.RED}That item is already in your Quick Buy.")
            player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
            return true
        }

        val firstEmpty = layout.indexOf(BedWarsShopCatalog.EMPTY_SLOT_ID)
        if (firstEmpty == -1)
        {
            player.sendMessage("${CC.RED}Your Quick Buy is full. Remove an item first.")
            player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
            return true
        }

        layout[firstEmpty] = entry.id
        profile.setBedWarsQuickBuyLayout(layout)
        profile.save()

        player.sendMessage("${CC.GREEN}Added ${CC.WHITE}${entry.displayName}${CC.GREEN} to Quick Buy.")
        player.playSound(player.location, Sound.NOTE_PLING, 1.0f, 2.0f)
        openMenu(player)
        return true
    }

    private fun isSoloOrDoubles() = lifecycle.configuration.mode.teamSize <= 2 &&
        lifecycle.configuration.mode.teamCount == 8

    private fun modeAdjustedCost(entry: BedWarsShopEntry): Int
    {
        if (isSoloOrDoubles())
        {
            return entry.cost
        }

        return when (entry.id)
        {
            "diamond_sword" -> 3
            "tnt" -> 8
            "water_bucket" -> 6
            "sponge_4" -> 6
            else -> entry.cost
        }
    }

    private fun costString(currency: ShopCurrency, cost: Int): String
    {
        val label = if (cost == 1) currency.displaySingular else currency.displayPlural
        return "${currency.color}$cost $label"
    }

    private fun displayCost(entry: BedWarsShopEntry, player: Player): Pair<String, Pair<ShopCurrency, Int>?>
    {
        if (entry.purchaseType == BedWarsShopPurchaseType.PermanentPickaxe)
        {
            val nextTier = lifecycle.playerShopState(player).nextPickaxeTier()
                ?: return "${CC.GREEN}MAXED" to null

            return costString(nextTier.purchaseCurrency, nextTier.purchaseCost) to
                (nextTier.purchaseCurrency to nextTier.purchaseCost)
        }

        if (entry.purchaseType == BedWarsShopPurchaseType.PermanentAxe)
        {
            val nextTier = lifecycle.playerShopState(player).nextAxeTier()
                ?: return "${CC.GREEN}MAXED" to null

            return costString(nextTier.purchaseCurrency, nextTier.purchaseCost) to
                (nextTier.purchaseCurrency to nextTier.purchaseCost)
        }

        if (entry.purchaseType == BedWarsShopPurchaseType.PermanentShears && lifecycle.playerShopState(player).hasShears)
        {
            return "${CC.GREEN}UNLOCKED" to null
        }

        if (entry.purchaseType == BedWarsShopPurchaseType.ArmorChainmail && lifecycle.playerShopState(player).armorTier >= BedWarsArmorTier.Chainmail)
        {
            return "${CC.GREEN}UNLOCKED" to null
        }

        if (entry.purchaseType == BedWarsShopPurchaseType.ArmorIron && lifecycle.playerShopState(player).armorTier >= BedWarsArmorTier.Iron)
        {
            return "${CC.GREEN}UNLOCKED" to null
        }

        if (entry.purchaseType == BedWarsShopPurchaseType.ArmorDiamond && lifecycle.playerShopState(player).armorTier >= BedWarsArmorTier.Diamond)
        {
            return "${CC.GREEN}UNLOCKED" to null
        }

        val cost = modeAdjustedCost(entry)
        return costString(entry.currency, cost) to (entry.currency to cost)
    }

    private fun swordTier(material: Material) = when (material)
    {
        Material.WOOD_SWORD -> 1
        Material.STONE_SWORD -> 2
        Material.IRON_SWORD -> 3
        Material.DIAMOND_SWORD -> 4
        else -> 0
    }

    private fun strongestSwordTier(player: Player) = player.inventory.contents
        .filterNotNull()
        .maxOfOrNull { stack -> swordTier(stack.type) }
        ?: 0

    private fun validatePurchase(player: Player, entry: BedWarsShopEntry): String?
    {
        if (entry.purchaseType == BedWarsShopPurchaseType.Standard && entry.icon.name.endsWith("_SWORD"))
        {
            val newTier = swordTier(entry.icon)
            if (newTier <= strongestSwordTier(player))
            {
                return "${CC.RED}You already have an equal or better sword!"
            }
        }

        return null
    }

    private fun removeLowerSwords(player: Player, newTier: Int)
    {
        player.inventory.contents.forEachIndexed { index, item ->
            if (item != null && swordTier(item.type) in 1 until newTier)
            {
                player.inventory.setItem(index, null)
            }
        }
    }

    private fun preferredCategory(entry: BedWarsShopEntry): BedWarsHotbarCategory? = when (entry.purchaseType)
    {
        BedWarsShopPurchaseType.PermanentPickaxe -> BedWarsHotbarCategory.Pickaxe
        BedWarsShopPurchaseType.PermanentAxe -> BedWarsHotbarCategory.Axe
        BedWarsShopPurchaseType.PermanentShears -> BedWarsHotbarCategory.Shears
        else -> BedWarsHotbarLayout.forShopCategory(entry.category)
    }

    private fun givePurchasedItem(player: Player, entry: BedWarsShopEntry, item: ItemStack)
    {
        val preferred = preferredCategory(entry)
        if (preferred == null)
        {
            player.giveOrDrop(item)
            return
        }

        lifecycle.givePreferredOrDrop(player, preferred, item)
    }

    private fun buildStandardItem(player: Player, entry: BedWarsShopEntry): ItemStack
    {
        if (entry.id == "wool_16")
        {
            val team = lifecycle.game.getNullableTeam(player)?.teamIdentifier
            val woolData = team?.let(BedWarsTeamPalette::woolData)?.toShort() ?: entry.data

            return ItemBuilder
                .of(Material.WOOL)
                .amount(entry.amount)
                .data(woolData)
                .build()
        }

        val item = ItemBuilder
            .of(entry.icon)
            .amount(entry.amount)
            .data(entry.data)

        if (entry.icon.name.endsWith("_SWORD"))
        {
            item.setUnbreakable(true)
        }

        return item.build()
    }

    private fun buildPotionFor(type: BedWarsShopPurchaseType): ItemStack
    {
        return when (type)
        {
            BedWarsShopPurchaseType.SpeedPotion ->
            {
                val item = ItemStack(Material.POTION, 1)
                val meta = item.itemMeta as org.bukkit.inventory.meta.PotionMeta
                meta.addCustomEffect(PotionEffect(PotionEffectType.SPEED, 20 * 45, 1), true)
                meta.displayName = "${CC.WHITE}Speed II Potion (45 seconds)"
                item.itemMeta = meta
                item
            }

            BedWarsShopPurchaseType.JumpPotion ->
            {
                val item = ItemStack(Material.POTION, 1)
                val meta = item.itemMeta as org.bukkit.inventory.meta.PotionMeta
                meta.addCustomEffect(PotionEffect(PotionEffectType.JUMP, 20 * 45, 4), true)
                meta.displayName = "${CC.WHITE}Jump V Potion (45 seconds)"
                item.itemMeta = meta
                item
            }

            BedWarsShopPurchaseType.InvisibilityPotion ->
            {
                val item = ItemStack(Material.POTION, 1)
                val meta = item.itemMeta as org.bukkit.inventory.meta.PotionMeta
                meta.addCustomEffect(PotionEffect(PotionEffectType.INVISIBILITY, 20 * 30, 0), true)
                meta.displayName = "${CC.WHITE}Invisibility Potion (30 seconds)"
                item.itemMeta = meta
                item
            }

            else -> ItemBuilder.of(Material.POTION).build()
        }
    }

    private fun applyPurchase(player: Player, entry: BedWarsShopEntry)
    {
        when (entry.purchaseType)
        {
            BedWarsShopPurchaseType.Standard ->
            {
                val purchasedItem = buildStandardItem(player, entry)
                if (purchasedItem.type.name.endsWith("_SWORD"))
                {
                    removeLowerSwords(player, swordTier(purchasedItem.type))
                }

                givePurchasedItem(player, entry, purchasedItem)
            }

            BedWarsShopPurchaseType.PermanentPickaxe ->
            {
                lifecycle.playerShopState(player).pickaxeLevel += 1
                lifecycle.applyPermanentTools(player)
            }

            BedWarsShopPurchaseType.PermanentAxe ->
            {
                lifecycle.playerShopState(player).axeLevel += 1
                lifecycle.applyPermanentTools(player)
            }

            BedWarsShopPurchaseType.PermanentShears ->
            {
                lifecycle.playerShopState(player).hasShears = true
                lifecycle.applyPermanentTools(player)
            }

            BedWarsShopPurchaseType.ArmorChainmail ->
            {
                lifecycle.playerShopState(player).armorTier = BedWarsArmorTier.Chainmail
                lifecycle.applyArmorForCurrentState(player)
            }

            BedWarsShopPurchaseType.ArmorIron ->
            {
                lifecycle.playerShopState(player).armorTier = BedWarsArmorTier.Iron
                lifecycle.applyArmorForCurrentState(player)
            }

            BedWarsShopPurchaseType.ArmorDiamond ->
            {
                lifecycle.playerShopState(player).armorTier = BedWarsArmorTier.Diamond
                lifecycle.applyArmorForCurrentState(player)
            }

            BedWarsShopPurchaseType.SpeedPotion,
            BedWarsShopPurchaseType.JumpPotion,
            BedWarsShopPurchaseType.InvisibilityPotion ->
            {
                givePurchasedItem(player, entry, buildPotionFor(entry.purchaseType))
            }

            BedWarsShopPurchaseType.KnockbackStick ->
            {
                givePurchasedItem(
                    player,
                    entry,
                    ItemBuilder.of(Material.STICK)
                        .name("${CC.WHITE}Stick (Knockback I)")
                        .enchant(Enchantment.KNOCKBACK, 1)
                        .build()
                )
            }

            BedWarsShopPurchaseType.WaterBucket ->
            {
                givePurchasedItem(player, entry, ItemStack(Material.WATER_BUCKET, 1))
            }

            BedWarsShopPurchaseType.BridgeEgg ->
            {
                givePurchasedItem(
                    player,
                    entry,
                    ItemBuilder.of(Material.EGG)
                        .name("${CC.WHITE}Bridge Egg")
                        .build()
                )
            }

            BedWarsShopPurchaseType.Sponge ->
            {
                givePurchasedItem(player, entry, ItemStack(Material.SPONGE, entry.amount))
            }

            BedWarsShopPurchaseType.CompactPop ->
            {
                givePurchasedItem(
                    player,
                    entry,
                    ItemBuilder.of(Material.TRAPPED_CHEST)
                        .name("${CC.WHITE}Compact Pop-up Tower")
                        .build()
                )
            }

            BedWarsShopPurchaseType.DreamDefender ->
            {
                givePurchasedItem(
                    player,
                    entry,
                    ItemBuilder.of(Material.MONSTER_EGG)
                        .data(99)
                        .name("${CC.WHITE}Dream Defender")
                        .build()
                )
            }

            BedWarsShopPurchaseType.BowPower ->
            {
                givePurchasedItem(
                    player,
                    entry,
                    ItemBuilder.of(Material.BOW)
                        .setUnbreakable(true)
                        .enchant(Enchantment.ARROW_DAMAGE, 1)
                        .build()
                )
            }

            BedWarsShopPurchaseType.BowPunch ->
            {
                givePurchasedItem(
                    player,
                    entry,
                    ItemBuilder.of(Material.BOW)
                        .setUnbreakable(true)
                        .enchant(Enchantment.ARROW_DAMAGE, 1)
                        .enchant(Enchantment.ARROW_KNOCKBACK, 1)
                        .build()
                )
            }

            BedWarsShopPurchaseType.MagicMilk ->
            {
                givePurchasedItem(
                    player,
                    entry,
                    ItemBuilder.of(Material.MILK_BUCKET)
                        .name("${CC.WHITE}Magic Milk")
                        .build()
                )
            }

            BedWarsShopPurchaseType.Bedbug ->
            {
                givePurchasedItem(
                    player,
                    entry,
                    ItemBuilder.of(Material.SNOW_BALL)
                        .name("${CC.WHITE}Bedbug")
                        .build()
                )
            }

            BedWarsShopPurchaseType.Rotational ->
            {
                applyRotationalPurchase(player, entry)
            }
        }

        lifecycle.applyTeamUpgrades(player)
    }

    private fun rotationalDefinition(entry: BedWarsShopEntry): RotationalItemDefinition?
    {
        val id = entry.id.removePrefix("rotational_")
        return RotationalItemDefinition.entries.firstOrNull { it.id == id }
    }

    private fun applyRotationalPurchase(player: Player, entry: BedWarsShopEntry)
    {
        val def = rotationalDefinition(entry)
        if (def == null)
        {
            player.giveOrDrop(ItemBuilder.of(entry.icon).amount(entry.amount).data(entry.data).build())
            return
        }

        when (def)
        {
            RotationalItemDefinition.Sharpness2 ->
            {
                val sword = player.inventory.contents.firstOrNull { item ->
                    item != null && item.type.name.endsWith("_SWORD")
                }
                if (sword == null)
                {
                    player.sendMessage("${CC.RED}You need a sword to apply this enchantment!")
                    return
                }
                sword.addEnchantment(Enchantment.DAMAGE_ALL, 2)
            }

            RotationalItemDefinition.FeatherFalling ->
            {
                val boots = player.inventory.boots
                if (boots == null)
                {
                    player.sendMessage("${CC.RED}You need boots to apply this enchantment!")
                    return
                }
                boots.addUnsafeEnchantment(Enchantment.PROTECTION_FALL, 4)
            }

            RotationalItemDefinition.ShearUpgrades ->
            {
                val shears = player.inventory.contents.firstOrNull { item ->
                    item != null && item.type == Material.SHEARS
                }
                if (shears == null)
                {
                    player.sendMessage("${CC.RED}You need shears to apply this upgrade!")
                    return
                }
                shears.addUnsafeEnchantment(Enchantment.DIG_SPEED, 3)
            }

            RotationalItemDefinition.DevastatorBow ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.BOW)
                        .name("${CC.WHITE}Devastator Bow")
                        .setUnbreakable(true)
                        .enchant(Enchantment.ARROW_DAMAGE, 3)
                        .build()
                )
            }

            RotationalItemDefinition.SugarCookie ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.COOKIE)
                        .name("${CC.WHITE}Super Cookie")
                        .build()
                )
            }

            RotationalItemDefinition.ThrowableTNT ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.TNT)
                        .name("${CC.WHITE}Throwable TNT")
                        .build()
                )
            }

            RotationalItemDefinition.MegaTNT ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.TNT)
                        .name("${CC.WHITE}Mega TNT")
                        .build()
                )
            }

            RotationalItemDefinition.MiracleOfTheStars ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.NETHER_STAR)
                        .name("${CC.WHITE}Miracle of the Stars")
                        .build()
                )
            }

            RotationalItemDefinition.UnstableTeleportationDevice ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.EYE_OF_ENDER)
                        .name("${CC.WHITE}Unstable TP Device")
                        .build()
                )
            }

            RotationalItemDefinition.Sugarkin ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.NETHER_STAR)
                        .name("${CC.WHITE}Sugarkin")
                        .build()
                )
            }

            RotationalItemDefinition.IceBridge ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.ICE)
                        .name("${CC.WHITE}Ice Bridge")
                        .build()
                )
            }

            RotationalItemDefinition.FestivityMine ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.CARPET)
                        .data(7)
                        .name("${CC.WHITE}Festivity Mine")
                        .build()
                )
            }

            RotationalItemDefinition.ChickenTurret ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.MONSTER_EGG)
                        .data(93)
                        .name("${CC.WHITE}Chicken Turret")
                        .build()
                )
            }

            RotationalItemDefinition.TimeWarpPearl ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.ENDER_PEARL)
                        .name("${CC.WHITE}Time Warp Pearl")
                        .build()
                )
            }

            RotationalItemDefinition.Snowman ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.PUMPKIN)
                        .name("${CC.WHITE}Snowman")
                        .build()
                )
            }

            RotationalItemDefinition.BridgeZapper ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.SHEARS)
                        .name("${CC.WHITE}Bridge Zapper")
                        .build()
                )
            }

            RotationalItemDefinition.BlockZapper ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.BLAZE_ROD)
                        .name("${CC.WHITE}Block Zapper")
                        .build()
                )
            }

            RotationalItemDefinition.LuckyChest ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.ENDER_CHEST)
                        .name("${CC.WHITE}Lucky Chest")
                        .build()
                )
            }

            RotationalItemDefinition.BlackHole ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.ENDER_PORTAL_FRAME)
                        .name("${CC.WHITE}Black Hole")
                        .build()
                )
            }

            RotationalItemDefinition.CharlieTheUnicorn ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.WHEAT)
                        .name("${CC.WHITE}Charlie the Unicorn")
                        .build()
                )
            }

            RotationalItemDefinition.PortableShopkeeper ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.CHEST)
                        .name("${CC.WHITE}Portable Shopkeeper")
                        .build()
                )
            }

            RotationalItemDefinition.IllusionTrap ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.TRIPWIRE_HOOK)
                        .name("${CC.WHITE}Illusion Trap")
                        .build()
                )
            }

            RotationalItemDefinition.BlockShuffler ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.MONSTER_EGG)
                        .data(58)
                        .name("${CC.WHITE}Block Shuffler")
                        .build()
                )
            }

            RotationalItemDefinition.FinalReviveBeacon ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(Material.BEACON)
                        .name("${CC.WHITE}Final Revive Beacon")
                        .build()
                )
            }

            else ->
            {
                givePurchasedItem(
                    player, entry,
                    ItemBuilder.of(def.icon)
                        .amount(def.amount)
                        .data(def.iconData)
                        .name("${CC.WHITE}${def.displayName}")
                        .build()
                )
            }
        }
    }

    private fun entryDisplayName(player: Player, entry: BedWarsShopEntry): String
    {
        if (entry.purchaseType == BedWarsShopPurchaseType.PermanentPickaxe)
        {
            val nextTier = lifecycle.playerShopState(player).nextPickaxeTier()
            return nextTier?.displayName ?: "Diamond Pickaxe"
        }

        if (entry.purchaseType == BedWarsShopPurchaseType.PermanentAxe)
        {
            val nextTier = lifecycle.playerShopState(player).nextAxeTier()
            return nextTier?.displayName ?: "Diamond Axe"
        }

        return entry.displayName
    }

    private fun resolveDisplayIcon(player: Player, entry: BedWarsShopEntry): ItemStack
    {
        if (entry.id == "wool_16")
        {
            val team = lifecycle.game.getNullableTeam(player)?.teamIdentifier
            val woolData = team?.let(BedWarsTeamPalette::woolData)?.toShort() ?: entry.data
            return ItemBuilder.of(Material.WOOL).amount(entry.amount).data(woolData).build()
        }

        if (entry.purchaseType == BedWarsShopPurchaseType.PermanentPickaxe)
        {
            val nextTier = lifecycle.playerShopState(player).nextPickaxeTier()
            val material = nextTier?.material ?: Material.DIAMOND_PICKAXE
            return ItemBuilder.of(material).build()
        }

        if (entry.purchaseType == BedWarsShopPurchaseType.PermanentAxe)
        {
            val nextTier = lifecycle.playerShopState(player).nextAxeTier()
            val material = nextTier?.material ?: Material.DIAMOND_AXE
            return ItemBuilder.of(material).build()
        }

        if (entry.purchaseType == BedWarsShopPurchaseType.BowPower)
        {
            return ItemBuilder.of(Material.BOW)
                .enchant(Enchantment.ARROW_DAMAGE, 1)
                .build()
        }

        if (entry.purchaseType == BedWarsShopPurchaseType.BowPunch)
        {
            return ItemBuilder.of(Material.BOW)
                .enchant(Enchantment.ARROW_DAMAGE, 1)
                .enchant(Enchantment.ARROW_KNOCKBACK, 1)
                .build()
        }

        if (entry.purchaseType == BedWarsShopPurchaseType.KnockbackStick)
        {
            return ItemBuilder.of(Material.STICK)
                .enchant(Enchantment.KNOCKBACK, 1)
                .build()
        }

        if (entry.purchaseType == BedWarsShopPurchaseType.DreamDefender)
        {
            return ItemBuilder.of(Material.MONSTER_EGG)
                .data(99)
                .build()
        }

        if (entry.purchaseType == BedWarsShopPurchaseType.CompactPop)
        {
            return ItemBuilder.of(Material.TRAPPED_CHEST).build()
        }

        return ItemBuilder.of(entry.icon)
            .amount(entry.amount)
            .data(entry.data)
            .build()
    }

    private fun buildDescription(entry: BedWarsShopEntry): List<String>
    {
        val lines = mutableListOf<String>()

        when (entry.purchaseType)
        {
            BedWarsShopPurchaseType.PermanentPickaxe,
            BedWarsShopPurchaseType.PermanentAxe ->
            {
                lines += "${CC.GRAY}This is an upgradable item."
                lines += "${CC.GRAY}It loses 1 tier upon death."
                lines += "${CC.GRAY}You always respawn with it."
            }

            BedWarsShopPurchaseType.PermanentShears ->
            {
                lines += "${CC.GRAY}Great to get rid of wool."
                lines += "${CC.GRAY}You will always spawn with these."
            }

            BedWarsShopPurchaseType.KnockbackStick ->
                lines += "${CC.GRAY}Stick with Knockback I."

            BedWarsShopPurchaseType.BridgeEgg ->
            {
                lines += "${CC.GRAY}Creates a bridge where"
                lines += "${CC.GRAY}the egg travels."
            }

            BedWarsShopPurchaseType.CompactPop ->
                lines += "${CC.GRAY}Place a compact pop-up defense."

            BedWarsShopPurchaseType.DreamDefender ->
                lines += "${CC.GRAY}Summons an Iron Golem defender."

            BedWarsShopPurchaseType.Bedbug ->
                lines += "${CC.GRAY}Spawns a silverfish on impact."

            BedWarsShopPurchaseType.Rotational ->
            {
                val def = rotationalDefinition(entry)
                if (def != null)
                {
                    def.description.forEach { line ->
                        lines += "${CC.GRAY}$line"
                    }
                }
            }

            BedWarsShopPurchaseType.WaterBucket ->
            {
                lines += "${CC.GRAY}Great to slow down enemies."
                lines += "${CC.GRAY}Can also protect against TNT."
            }

            BedWarsShopPurchaseType.Sponge ->
                lines += "${CC.GRAY}Great for soaking up water."

            BedWarsShopPurchaseType.MagicMilk ->
                lines += "${CC.GRAY}Avoid triggering traps for 30s."

            BedWarsShopPurchaseType.BowPower ->
                lines += "${CC.GRAY}Bow with Power I."

            BedWarsShopPurchaseType.BowPunch ->
                lines += "${CC.GRAY}Bow with Power I and Punch I."

            BedWarsShopPurchaseType.ArmorChainmail,
            BedWarsShopPurchaseType.ArmorIron,
            BedWarsShopPurchaseType.ArmorDiamond ->
                lines += "${CC.GRAY}Permanent armor upgrade."

            else ->
            {
                when (entry.id)
                {
                    "wool_16" ->
                    {
                        lines += "${CC.GRAY}Great for bridging across islands."
                        lines += "${CC.GRAY}Turns into your team's color."
                    }

                    "clay_16" -> lines += "${CC.GRAY}Basic block to defend your bed."
                    "glass_4" -> lines += "${CC.GRAY}Immune to explosions."
                    "endstone_12" -> lines += "${CC.GRAY}Solid block to defend your bed."
                    "wood_16" -> lines += "${CC.GRAY}Strong against pickaxes."
                    "obsidian_4" -> lines += "${CC.GRAY}Extreme bed protection."
                    "fireball" -> lines += "${CC.GRAY}Right-click to launch!"
                    "tnt" -> lines += "${CC.GRAY}Instantly ignites."
                    "ender_pearl" -> lines += "${CC.GRAY}The quickest way to invade bases."
                    "gapple" -> lines += "${CC.GRAY}Well-rounded healing."
                }
            }
        }

        return lines
    }

    private fun buildEntryItem(player: Player, entry: BedWarsShopEntry): ItemStack
    {
        val displayName = entryDisplayName(player, entry)
        val icon = resolveDisplayIcon(player, entry)
        val (costText, costReference) = displayCost(entry, player)

        val canBuy = costReference != null && player.canAfford(costReference.first, costReference.second)
        val owned = costReference == null

        val lore = mutableListOf<String>()
        lore += "${CC.GRAY}Cost: $costText"

        val description = buildDescription(entry)
        if (description.isNotEmpty())
        {
            lore += ""
            lore += description
        }

        lore += ""
        if (category == BedWarsShopCategory.QuickBuy)
        {
            lore += "${CC.AQUA}Shift-Click to remove from Quick Buy"
        } else if (entry.quickBuyEligible)
        {
            lore += "${CC.AQUA}Shift-Click to add to Quick Buy"
        }

        lore += if (owned)
        {
            "${CC.GREEN}You already have this item!"
        } else if (canBuy)
        {
            "${CC.YELLOW}Click to purchase!"
        } else
        {
            val currency = costReference!!.first
            val amount = costReference.second
            val label = if (amount == 1) currency.displaySingular else currency.displayPlural
            "${CC.RED}You don't have enough ${currency.color}$label${CC.RED}!"
        }

        val nameColor = if (owned || canBuy) CC.GREEN else CC.RED

        return ItemBuilder.copyOf(icon)
            .name("$nameColor$displayName")
            .setLore(lore)
            .addFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS)
            .build()
    }

    private fun handlePurchase(
        player: Player,
        entry: BedWarsShopEntry,
        costReference: Pair<ShopCurrency, Int>?
    )
    {
        val validation = validatePurchase(player, entry)
        if (validation != null)
        {
            player.sendMessage(validation)
            player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
            return
        }

        if (costReference == null)
        {
            player.sendMessage("${CC.RED}You already own this item.")
            player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
            return
        }

        val (currency, cost) = costReference
        if (!player.tryCharge(currency, cost))
        {
            val label = if (cost == 1) currency.displaySingular else currency.displayPlural
            player.sendMessage("${CC.RED}You don't have enough ${currency.color}$label${CC.RED}!")
            player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
            return
        }

        applyPurchase(player, entry)

        player.sendMessage("${CC.GREEN}You purchased ${CC.WHITE}${entryDisplayName(player, entry)}${CC.GREEN}!")
        player.playSound(player.location, Sound.NOTE_PLING, 1.0f, 2.0f)
        openMenu(player)
    }
}
