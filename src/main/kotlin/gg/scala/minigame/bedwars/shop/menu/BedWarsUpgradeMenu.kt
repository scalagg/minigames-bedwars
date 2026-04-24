package gg.scala.minigame.bedwars.shop.menu

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.scala.minigame.bedwars.shop.BedWarsShopEconomy.canAfford
import gg.scala.minigame.bedwars.shop.BedWarsShopEconomy.tryCharge
import gg.scala.minigame.bedwars.trap.BedWarsTrapQueueResult
import gg.scala.minigame.bedwars.trap.BedWarsTrapType
import gg.tropic.practice.currency.ShopCurrency
import gg.tropic.practice.games.team.TeamIdentifier
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player

/**
 * @author Limo
 * @date 2/13/2026
 */
class BedWarsUpgradeMenu(
    private val lifecycle: BedWarsLifecycle,
    private val team: TeamIdentifier
) : Menu("Upgrades & Traps")
{
    companion object
    {
        private data class UpgradeCosts(
            val sharpness: Int,
            val protection: IntArray,
            val haste: IntArray,
            val forge: IntArray,
            val healPool: Int
        )

        private val SOLO_DOUBLES_COSTS = UpgradeCosts(
            sharpness = 4,
            protection = intArrayOf(2, 4, 8, 16),
            haste = intArrayOf(2, 4),
            forge = intArrayOf(2, 4, 6, 8),
            healPool = 1
        )

        private val OTHER_MODE_COSTS = UpgradeCosts(
            sharpness = 8,
            protection = intArrayOf(5, 10, 20, 30),
            haste = intArrayOf(4, 6),
            forge = intArrayOf(4, 8, 12, 16),
            healPool = 3
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
        val state = lifecycle.teamState(team)
        val costs = currentCosts()
        val trapCost = lifecycle.nextTrapCost(team)

        buildPurchasableUpgrades(player, buttons, costs)
        buildPurchasableTraps(player, buttons, trapCost)
        buildSeparator(buttons)
        buildTrapQueue(buttons, state.queuedTraps.toList(), trapCost)

        return buttons
    }

    private fun isSoloOrDoubles() = lifecycle.configuration.mode.teamSize <= 2 &&
        lifecycle.configuration.mode.teamCount == 8

    private fun currentCosts() = if (isSoloOrDoubles()) SOLO_DOUBLES_COSTS else OTHER_MODE_COSTS

    private fun buildPurchasableUpgrades(player: Player, buttons: MutableMap<Int, Button>, costs: UpgradeCosts)
    {
        val state = lifecycle.teamState(team)

        val sharpCanBuy = !state.sharpness && player.canAfford(ShopCurrency.Diamond, costs.sharpness)
        val sharpNameColor = if (state.sharpness || sharpCanBuy) CC.GREEN else CC.RED
        val sharpAction = when
        {
            state.sharpness -> "${CC.GREEN}You already have this upgrade!"
            sharpCanBuy -> "${CC.YELLOW}Click to purchase!"
            else -> "${CC.RED}You don't have enough Diamonds!"
        }

        buttons[10] = ItemBuilder
            .of(Material.IRON_SWORD)
            .name("${sharpNameColor}Sharpened Swords")
            .setLore(
                listOf(
                    "${CC.GRAY}Your team permanently gains",
                    "${CC.GRAY}Sharpness I on all swords and",
                    "${CC.GRAY}axes!",
                    "",
                    "${CC.GRAY}Cost: ${CC.AQUA}${costs.sharpness} Diamonds",
                    "",
                    sharpAction
                )
            )
            .toButton { clicker, _ -> clicker?.let(::handleSharpnessPurchase) }

        val protectionCost = costs.protection.getOrNull(state.protectionLevel)
        val protectionCanBuy = protectionCost != null && player.canAfford(ShopCurrency.Diamond, protectionCost)
        val protectionNameColor = if (state.protectionLevel >= costs.protection.size || protectionCanBuy) CC.GREEN else CC.RED
        val protectionAction = when
        {
            state.protectionLevel >= costs.protection.size -> "${CC.GREEN}You already have this upgrade!"
            protectionCanBuy -> "${CC.YELLOW}Click to purchase!"
            else -> "${CC.RED}You don't have enough Diamonds!"
        }

        buttons[11] = ItemBuilder
            .of(Material.IRON_CHESTPLATE)
            .name("${protectionNameColor}Reinforced Armor ${romanTier(state.protectionLevel + 1)}")
            .setLore(
                mutableListOf<String>().apply {
                    add("${CC.GRAY}Your team permanently gains")
                    add("${CC.GRAY}Protection on all armor pieces!")
                    add("")
                    add("${tierIndicator(1, state.protectionLevel)}Tier 1: Protection I, ${CC.AQUA}${costs.protection[0]} Diamonds")
                    add("${tierIndicator(2, state.protectionLevel)}Tier 2: Protection II, ${CC.AQUA}${costs.protection[1]} Diamonds")
                    add("${tierIndicator(3, state.protectionLevel)}Tier 3: Protection III, ${CC.AQUA}${costs.protection[2]} Diamonds")
                    add("${tierIndicator(4, state.protectionLevel)}Tier 4: Protection IV, ${CC.AQUA}${costs.protection[3]} Diamonds")
                    add("")
                    add(protectionAction)
                }
            )
            .toButton { clicker, _ -> clicker?.let(::handleProtectionPurchase) }

        val hasteCost = costs.haste.getOrNull(state.hasteLevel)
        val hasteCanBuy = hasteCost != null && player.canAfford(ShopCurrency.Diamond, hasteCost)
        val hasteNameColor = if (state.hasteLevel >= costs.haste.size || hasteCanBuy) CC.GREEN else CC.RED
        val hasteAction = when
        {
            state.hasteLevel >= costs.haste.size -> "${CC.GREEN}You already have this upgrade!"
            hasteCanBuy -> "${CC.YELLOW}Click to purchase!"
            else -> "${CC.RED}You don't have enough Diamonds!"
        }

        buttons[12] = ItemBuilder
            .of(Material.GOLD_PICKAXE)
            .name("${hasteNameColor}Maniac Miner ${romanTier(state.hasteLevel + 1)}")
            .setLore(
                mutableListOf<String>().apply {
                    add("${CC.GRAY}All players on your team")
                    add("${CC.GRAY}permanently gain Haste.")
                    add("")
                    add("${tierIndicator(1, state.hasteLevel)}Tier 1: Haste I, ${CC.AQUA}${costs.haste[0]} Diamonds")
                    add("${tierIndicator(2, state.hasteLevel)}Tier 2: Haste II, ${CC.AQUA}${costs.haste[1]} Diamonds")
                    add("")
                    add(hasteAction)
                }
            )
            .toButton { clicker, _ -> clicker?.let(::handleHastePurchase) }

        val purchasedForgeTiers = (state.forgeLevel - 1).coerceAtLeast(0)
        val forgeCost = costs.forge.getOrNull(purchasedForgeTiers)
        val forgeCanBuy = forgeCost != null && player.canAfford(ShopCurrency.Diamond, forgeCost)
        val forgeNameColor = if (forgeCost == null || forgeCanBuy) CC.GREEN else CC.RED
        val forgeAction = when
        {
            forgeCost == null -> "${CC.GREEN}You already have this upgrade!"
            forgeCanBuy -> "${CC.YELLOW}Click to purchase!"
            else -> "${CC.RED}You don't have enough Diamonds!"
        }

        buttons[19] = ItemBuilder
            .of(Material.FURNACE)
            .name("${forgeNameColor}${forgeNameForLevel(state.forgeLevel)}")
            .setLore(
                mutableListOf<String>().apply {
                    val forgePurchased = (state.forgeLevel - 1).coerceAtLeast(0)
                    add("${CC.GRAY}Upgrade resource spawning on")
                    add("${CC.GRAY}your island.")
                    add("")
                    add("${tierIndicator(1, forgePurchased)}Tier 1: +50% Resources, ${CC.AQUA}${costs.forge[0]} Diamonds")
                    add("${tierIndicator(2, forgePurchased)}Tier 2: +100% Resources, ${CC.AQUA}${costs.forge[1]} Diamonds")
                    add("${tierIndicator(3, forgePurchased)}Tier 3: Spawn emeralds, ${CC.AQUA}${costs.forge[2]} Diamonds")
                    add("${tierIndicator(4, forgePurchased)}Tier 4: +200% Resources, ${CC.AQUA}${costs.forge[3]} Diamonds")
                    add("")
                    add(forgeAction)
                }
            )
            .toButton { clicker, _ -> clicker?.let(::handleForgePurchase) }

        val healCanBuy = !state.healPool && player.canAfford(ShopCurrency.Diamond, costs.healPool)
        val healNameColor = if (state.healPool || healCanBuy) CC.GREEN else CC.RED
        val healAction = when
        {
            state.healPool -> "${CC.GREEN}You already have this upgrade!"
            healCanBuy -> "${CC.YELLOW}Click to purchase!"
            else -> "${CC.RED}You don't have enough Diamonds!"
        }

        buttons[20] = ItemBuilder
            .of(Material.BEACON)
            .name("${healNameColor}Heal Pool")
            .setLore(
                listOf(
                    "${CC.GRAY}Creates a Regeneration field",
                    "${CC.GRAY}around your base!",
                    "",
                    "${CC.GRAY}Cost: ${CC.AQUA}${costs.healPool} ${if (costs.healPool == 1) "Diamond" else "Diamonds"}",
                    "",
                    healAction
                )
            )
            .toButton { clicker, _ -> clicker?.let(::handleHealPoolPurchase) }

        buttons[21] = ItemBuilder
            .of(Material.DRAGON_EGG)
            .name("${CC.RED}Dragon Buff")
            .setLore(
                listOf(
                    "${CC.GRAY}Your team will have 2 dragons",
                    "${CC.GRAY}instead of 1 during deathmatch!",
                    "",
                    "${CC.GRAY}Cost: ${CC.AQUA}5 Diamonds",
                    "",
                    "${CC.RED}This upgrade is currently unavailable."
                )
            )
            .toButton()
    }

    private fun buildPurchasableTraps(player: Player, buttons: MutableMap<Int, Button>, trapCost: Int)
    {
        val trapSlots = listOf(
            14 to BedWarsTrapType.ItsATrap,
            15 to BedWarsTrapType.CounterOffensive,
            16 to BedWarsTrapType.Alarm,
            23 to BedWarsTrapType.MinerFatigue
        )

        trapSlots.forEach { (slot, trapType) ->
            val queueState = lifecycle.trapQueueResultFor(team, trapType)
            val canBuy = queueState == BedWarsTrapQueueResult.Queued && player.canAfford(ShopCurrency.Diamond, trapCost)
            val action = when (queueState)
            {
                BedWarsTrapQueueResult.AlreadyQueued -> "${CC.RED}This trap is already queued!"
                BedWarsTrapQueueResult.QueueFull -> "${CC.RED}Trap queue is full!"
                BedWarsTrapQueueResult.Queued -> if (canBuy) "${CC.YELLOW}Click to purchase!" else "${CC.RED}You don't have enough Diamonds!"
            }

            val nameColor = if (canBuy) CC.GREEN else CC.RED

            buttons[slot] = ItemBuilder
                .of(trapType.icon)
                .name("${nameColor}${trapType.displayName}")
                .setLore(
                    listOf(
                        "${CC.GRAY}${trapType.shortDescription}",
                        "",
                        "${CC.GRAY}Cost: ${CC.AQUA}$trapCost ${if (trapCost == 1) "Diamond" else "Diamonds"}",
                        "",
                        action
                    )
                )
                .toButton { clicker, _ -> clicker?.let { handleTrapPurchase(it, trapType) } }
        }
    }

    private fun buildSeparator(buttons: MutableMap<Int, Button>)
    {
        for (slot in 27..35)
        {
            buttons[slot] = ItemBuilder
                .of(Material.STAINED_GLASS_PANE)
                .data(7)
                .name("${CC.D_GRAY}^ ${CC.GRAY}Purchasable")
                .addToLore("${CC.D_GRAY}v ${CC.GRAY}Traps Queue")
                .toButton()
        }
    }

    private fun buildTrapQueue(buttons: MutableMap<Int, Button>, queuedTraps: List<BedWarsTrapType>, nextTrapCost: Int)
    {
        val ordinals = arrayOf("first", "second", "third")

        for (index in 0 until 3)
        {
            val slot = 39 + index
            val queued = queuedTraps.getOrNull(index)

            if (queued != null)
            {
                buttons[slot] = ItemBuilder
                    .of(queued.icon)
                    .name("${CC.GREEN}Trap #${index + 1}: ${CC.WHITE}${queued.displayName}")
                    .setLore(
                        listOf(
                            "${CC.GRAY}${queued.shortDescription}",
                            "",
                            "${CC.GREEN}Trap #${index + 1} is active!"
                        )
                    )
                    .toButton()
                continue
            }

            buttons[slot] = ItemBuilder
                .of(Material.STAINED_GLASS)
                .data(8)
                .amount(index + 1)
                .name("${CC.RED}Trap #${index + 1}: No Trap!")
                .setLore(
                    listOf(
                        "${CC.GRAY}The ${ordinals[index]} enemy to walk into",
                        "${CC.GRAY}your base will trigger this trap!",
                        "",
                        "${CC.GRAY}Purchasing a trap will queue it",
                        "${CC.GRAY}here. Its cost scales based",
                        "${CC.GRAY}on queued traps.",
                        "",
                        "${CC.GRAY}Next trap: ${CC.AQUA}$nextTrapCost ${if (nextTrapCost == 1) "Diamond" else "Diamonds"}"
                    )
                )
                .toButton()
        }
    }

    private fun forgeNameForLevel(level: Int) = when (level)
    {
        1 -> "Iron Forge"
        2 -> "Golden Forge"
        3 -> "Emerald Forge"
        else -> "Molten Forge"
    }

    private fun tierIndicator(tier: Int, currentLevel: Int): String
    {
        return when
        {
            tier <= currentLevel -> "${CC.GREEN}"
            tier == currentLevel + 1 -> "${CC.YELLOW}"
            else -> "${CC.RED}"
        }
    }

    private fun romanTier(level: Int) = when (level)
    {
        1 -> "I"
        2 -> "II"
        3 -> "III"
        4 -> "IV"
        else -> level.toString()
    }

    private fun handleForgePurchase(player: Player)
    {
        val state = lifecycle.teamState(team)
        val costs = currentCosts().forge
        val purchasedTiers = (state.forgeLevel - 1).coerceAtLeast(0)
        val cost = costs.getOrNull(purchasedTiers)

        if (cost == null)
        {
            player.sendMessage("${CC.RED}You already have this upgrade.")
            player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
            return
        }

        if (!player.tryCharge(ShopCurrency.Diamond, cost))
        {
            player.sendMessage("${CC.RED}You don't have enough Diamonds!")
            player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
            return
        }

        state.forgeLevel += 1
        lifecycle.generatorSystem.upgradeTeamForge(team)
        lifecycle.notifyTeam(team, "${CC.GREEN}${player.name}${CC.GRAY} purchased ${CC.WHITE}${forgeNameForLevel(state.forgeLevel - 1)}${CC.GRAY}.")
        player.playSound(player.location, Sound.NOTE_PLING, 1.0f, 2.0f)
        openMenu(player)
    }

    private fun handleSharpnessPurchase(player: Player)
    {
        val state = lifecycle.teamState(team)
        val cost = currentCosts().sharpness

        if (state.sharpness)
        {
            player.sendMessage("${CC.RED}You already have this upgrade.")
            player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
            return
        }

        if (!player.tryCharge(ShopCurrency.Diamond, cost))
        {
            player.sendMessage("${CC.RED}You don't have enough Diamonds!")
            player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
            return
        }

        state.sharpness = true
        lifecycle.teamPlayers(team).forEach(lifecycle::applyTeamUpgrades)
        lifecycle.notifyTeam(team, "${CC.GREEN}${player.name}${CC.GRAY} purchased ${CC.WHITE}Sharpened Swords${CC.GRAY}.")
        player.playSound(player.location, Sound.NOTE_PLING, 1.0f, 2.0f)
        openMenu(player)
    }

    private fun handleProtectionPurchase(player: Player)
    {
        val state = lifecycle.teamState(team)
        val costs = currentCosts().protection
        val cost = costs.getOrNull(state.protectionLevel)

        if (cost == null)
        {
            player.sendMessage("${CC.RED}You already have this upgrade.")
            player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
            return
        }

        if (!player.tryCharge(ShopCurrency.Diamond, cost))
        {
            player.sendMessage("${CC.RED}You don't have enough Diamonds!")
            player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
            return
        }

        state.protectionLevel += 1
        lifecycle.teamPlayers(team).forEach(lifecycle::applyTeamUpgrades)
        lifecycle.notifyTeam(team, "${CC.GREEN}${player.name}${CC.GRAY} purchased ${CC.WHITE}Reinforced Armor ${romanTier(state.protectionLevel)}${CC.GRAY}.")
        player.playSound(player.location, Sound.NOTE_PLING, 1.0f, 2.0f)
        openMenu(player)
    }

    private fun handleHastePurchase(player: Player)
    {
        val state = lifecycle.teamState(team)
        val costs = currentCosts().haste
        val cost = costs.getOrNull(state.hasteLevel)

        if (cost == null)
        {
            player.sendMessage("${CC.RED}You already have this upgrade.")
            player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
            return
        }

        if (!player.tryCharge(ShopCurrency.Diamond, cost))
        {
            player.sendMessage("${CC.RED}You don't have enough Diamonds!")
            player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
            return
        }

        state.hasteLevel += 1
        lifecycle.teamPlayers(team).forEach(lifecycle::applyTeamUpgrades)
        lifecycle.notifyTeam(team, "${CC.GREEN}${player.name}${CC.GRAY} purchased ${CC.WHITE}Maniac Miner ${romanTier(state.hasteLevel)}${CC.GRAY}.")
        player.playSound(player.location, Sound.NOTE_PLING, 1.0f, 2.0f)
        openMenu(player)
    }

    private fun handleHealPoolPurchase(player: Player)
    {
        val state = lifecycle.teamState(team)
        val cost = currentCosts().healPool

        if (state.healPool)
        {
            player.sendMessage("${CC.RED}You already have this upgrade.")
            player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
            return
        }

        if (!player.tryCharge(ShopCurrency.Diamond, cost))
        {
            player.sendMessage("${CC.RED}You don't have enough Diamonds!")
            player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
            return
        }

        state.healPool = true
        lifecycle.notifyTeam(team, "${CC.GREEN}${player.name}${CC.GRAY} purchased ${CC.WHITE}Heal Pool${CC.GRAY}.")
        player.playSound(player.location, Sound.NOTE_PLING, 1.0f, 2.0f)
        openMenu(player)
    }

    private fun handleTrapPurchase(player: Player, trapType: BedWarsTrapType)
    {
        val trapCost = lifecycle.nextTrapCost(team)

        when (lifecycle.trapQueueResultFor(team, trapType))
        {
            BedWarsTrapQueueResult.AlreadyQueued ->
            {
                player.sendMessage("${CC.RED}This trap is already queued.")
                player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
                return
            }

            BedWarsTrapQueueResult.QueueFull ->
            {
                player.sendMessage("${CC.RED}Your trap queue is full.")
                player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
                return
            }

            BedWarsTrapQueueResult.Queued -> Unit
        }

        if (!player.tryCharge(ShopCurrency.Diamond, trapCost))
        {
            player.sendMessage("${CC.RED}You don't have enough Diamonds!")
            player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
            return
        }

        if (lifecycle.queueTrap(team, trapType) != BedWarsTrapQueueResult.Queued)
        {
            player.sendMessage("${CC.RED}Could not queue that trap right now.")
            player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
            return
        }

        lifecycle.notifyTeam(team, "${CC.GREEN}${player.name}${CC.GRAY} purchased ${CC.WHITE}${trapType.displayName}${CC.GRAY}.")
        player.playSound(player.location, Sound.NOTE_PLING, 1.0f, 2.0f)
        openMenu(player)
    }
}
