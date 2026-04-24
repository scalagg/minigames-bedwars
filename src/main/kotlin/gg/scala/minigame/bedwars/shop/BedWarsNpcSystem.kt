package gg.scala.minigame.bedwars.shop

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.scala.minigame.bedwars.shop.menu.BedWarsItemShopMenu
import gg.scala.minigame.bedwars.shop.menu.BedWarsUpgradeMenu
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.team.TeamIdentifier
import gg.tropic.practice.map.metadata.impl.MapShopKeeperMetadata
import gg.tropic.practice.map.metadata.impl.MapTeamUpgraderMetadata
import net.evilblock.cubed.entity.EntityHandler
import net.evilblock.cubed.entity.villager.VillagerEntity
import net.evilblock.cubed.util.CC
import org.bukkit.Location
import org.bukkit.entity.Player

/**
 * @author Limo
 * @date 2/13/2026
 */
class BedWarsNpcSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    private val spawnedEntities = mutableListOf<VillagerEntity>()
    private val shopEntitiesByTeam = mutableMapOf<String, MutableList<VillagerEntity>>()
    private val upgraderEntitiesByTeam = mutableMapOf<String, MutableList<VillagerEntity>>()

    private inner class BedWarsShopNpcEntity(
        location: Location
    ) : VillagerEntity(
        listOf("${CC.GREEN}${CC.BOLD}ITEM SHOP", "${CC.YELLOW}Right Click"),
        location
    )
    {
        init { persistent = false }

        override fun onRightClick(player: Player)
        {
            val game = GameService.byPlayer(player)
                ?: return

            if (game.identifier != lifecycle.game.identifier)
            {
                return
            }

            BedWarsItemShopMenu(lifecycle).openMenu(player)
        }
    }

    private inner class BedWarsUpgraderNpcEntity(
        location: Location,
        private val ownerTeam: TeamIdentifier?
    ) : VillagerEntity(
        listOf("${CC.AQUA}${CC.BOLD}TEAM UPGRADES", "${CC.YELLOW}Right Click"),
        location
    )
    {
        init { persistent = false }

        override fun onRightClick(player: Player)
        {
            val game = GameService.byPlayer(player)
                ?: return

            if (game.identifier != lifecycle.game.identifier)
            {
                return
            }

            handleUpgraderClick(player, ownerTeam)
        }
    }

    fun configure()
    {
        val world = lifecycle.game.arenaWorld
            ?: return

        lifecycle.game.map.metadata.metadata
            .filterIsInstance<MapShopKeeperMetadata>()
            .forEach { metadata ->
                metadata.locations.forEach { (group, locations) ->
                    locations.forEach { position ->
                        val spawnLoc = position.toLocation(world).add(0.5, 0.0, 0.5)
                        val npc = BedWarsShopNpcEntity(spawnLoc)

                        npc.initializeData()
                        npc.updateLines(listOf("${CC.GREEN}${CC.BOLD}ITEM SHOP", "${CC.YELLOW}Right Click"))
                        EntityHandler.trackEntity(npc)
                        spawnedEntities += npc
                        shopEntitiesByTeam.getOrPut(group.lowercase()) { mutableListOf() } += npc
                    }
                }
            }

        lifecycle.game.map.metadata.metadata
            .filterIsInstance<MapTeamUpgraderMetadata>()
            .forEach { metadata ->
                metadata.locations.forEach { (group, locations) ->
                    val team = TeamIdentifier.ID[group.uppercase()]
                    locations.forEach { position ->
                        val spawnLoc = position.toLocation(world).add(0.5, 0.0, 0.5)
                        val npc = BedWarsUpgraderNpcEntity(spawnLoc, team)

                        npc.initializeData()
                        npc.updateLines(listOf("${CC.AQUA}${CC.BOLD}TEAM UPGRADES", "${CC.YELLOW}Right Click"))
                        EntityHandler.trackEntity(npc)
                        spawnedEntities += npc
                        upgraderEntitiesByTeam.getOrPut(group.lowercase()) { mutableListOf() } += npc
                    }
                }
            }
    }

    fun cleanup()
    {
        spawnedEntities.forEach { entity ->
            entity.destroyForCurrentWatchers()
            EntityHandler.forgetEntity(entity)
        }
        spawnedEntities.clear()
        shopEntitiesByTeam.clear()
        upgraderEntitiesByTeam.clear()
    }

    fun removeShopNpcsForTeam(teamLabel: String)
    {
        val key = teamLabel.lowercase()
        val entities = shopEntitiesByTeam.remove(key) ?: return

        entities.forEach { entity ->
            entity.destroyForCurrentWatchers()
            EntityHandler.forgetEntity(entity)
            spawnedEntities.remove(entity)
        }
    }

    fun removeUpgraderNpcsForTeam(teamLabel: String)
    {
        val key = teamLabel.lowercase()
        val entities = upgraderEntitiesByTeam.remove(key) ?: return

        entities.forEach { entity ->
            entity.destroyForCurrentWatchers()
            EntityHandler.forgetEntity(entity)
            spawnedEntities.remove(entity)
        }
    }

    private fun handleUpgraderClick(player: Player, ownerTeam: TeamIdentifier?)
    {
        val playerTeam = lifecycle.game.getNullableTeam(player)
            ?.teamIdentifier
            ?: return

        BedWarsUpgradeMenu(lifecycle, playerTeam).openMenu(player)
    }
}
