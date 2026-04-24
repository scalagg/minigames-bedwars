package gg.scala.minigame.bedwars.items.menu

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.scala.minigame.bedwars.items.TrackerSystem
import gg.scala.minigame.bedwars.shop.BedWarsShopEconomy.canAfford
import gg.scala.minigame.bedwars.shop.BedWarsShopEconomy.tryCharge
import gg.scala.minigame.bedwars.team.BedWarsTeamPalette
import gg.tropic.practice.currency.ShopCurrency
import gg.tropic.practice.games.team.TeamIdentifier
import gg.tropic.practice.visuals.TeamVisuals
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player

/**
 * @author Limo
 * @date 2/18/2026
 */
class TrackerTeamMenu(
    private val lifecycle: BedWarsLifecycle,
    private val trackerSystem: TrackerSystem,
    private val viewerTeam: TeamIdentifier
) : Menu("Tracker")
{
    companion object
    {
        private val COST = ShopCurrency.Emerald
        private const val COST_AMOUNT = 2
    }

    init
    {
        placeholder = true
    }

    override fun size(buttons: Map<Int, Button>) = 27

    override fun getButtons(player: Player): MutableMap<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        val aliveEnemyTeams = lifecycle.game.teams
            .filter { team -> team.teamIdentifier != viewerTeam }
            .filter { team ->
                lifecycle.bedTracker.isBedAlive(team.teamIdentifier) ||
                    team.nonSpectators().isNotEmpty()
            }
            .sortedBy { it.teamIdentifier.priority() }

        if (aliveEnemyTeams.isEmpty())
        {
            buttons[13] = ItemBuilder
                .of(Material.BARRIER)
                .name("${CC.RED}No teams to track!")
                .addToLore("${CC.GRAY}All enemy teams have been eliminated.")
                .toButton()
            return buttons
        }

        val startSlot = when (aliveEnemyTeams.size)
        {
            1 -> 13
            2 -> 12
            3 -> 12
            4 -> 11
            5 -> 11
            6 -> 10
            7 -> 10
            else -> 10
        }

        aliveEnemyTeams.forEachIndexed { index, team ->
            val teamId = team.teamIdentifier
            val teamColor = TeamVisuals.toChatColor(teamId)
            val readable = TeamVisuals.toReadable(teamColor)
            val woolData = BedWarsTeamPalette.woolData(teamId)
            val alivePlayers = team.nonSpectators().size
            val currentlyTracking = trackerSystem.trackedTeams[player.uniqueId] == teamId
            val canAfford = player.canAfford(COST, COST_AMOUNT)

            val lore = mutableListOf<String>()
            lore += "${CC.GRAY}Players alive: ${CC.WHITE}$alivePlayers"
            lore += ""

            if (currentlyTracking)
            {
                lore += "${CC.GREEN}Currently tracking!"
            } else
            {
                lore += "${CC.GRAY}Cost: ${COST.color}$COST_AMOUNT ${COST.displayPlural}"
                lore += ""
                if (canAfford)
                {
                    lore += "${CC.YELLOW}Click to track!"
                } else
                {
                    lore += "${CC.RED}You don't have enough ${COST.color}${COST.displayPlural}${CC.RED}!"
                }
            }

            val builder = ItemBuilder
                .of(Material.WOOL)
                .data(woolData.toShort())
                .name("$teamColor$readable Team")
                .setLore(lore)

            if (currentlyTracking)
            {
                builder.enchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1)
                builder.addFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
            }

            buttons[startSlot + index] = builder.toButton { _, _ ->
                if (currentlyTracking)
                {
                    player.sendMessage("${CC.GRAY}You are already tracking $teamColor$readable Team${CC.GRAY}!")
                    player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
                    return@toButton
                }

                if (!player.tryCharge(COST, COST_AMOUNT))
                {
                    val label = COST.displayPlural
                    player.sendMessage("${CC.RED}You don't have enough ${COST.color}$label${CC.RED}!")
                    player.playSound(player.location, Sound.NOTE_BASS, 1.0f, 0.7f)
                    return@toButton
                }

                trackerSystem.trackedTeams[player.uniqueId] = teamId
                player.sendMessage("${CC.GREEN}Tracking $teamColor$readable Team${CC.GREEN}!")
                player.playSound(player.location, Sound.NOTE_PLING, 1.0f, 2.0f)
                player.closeInventory()
            }
        }

        return buttons
    }
}
