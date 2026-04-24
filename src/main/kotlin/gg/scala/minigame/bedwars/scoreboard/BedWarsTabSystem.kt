package gg.scala.minigame.bedwars.scoreboard

import gg.scala.basics.plugin.tablist.TabListService
import gg.scala.commons.tablist.adapter.populator.DefaultPlayersProvider
import net.evilblock.cubed.util.CC
import gg.scala.commons.tablist.adapter.populator.PlayerTablistPopulator
import gg.scala.commons.tablist.manage.TablistManager
import gg.scala.commons.tablist.util.PacketUtil
import gg.scala.commons.tablist.util.TablistEntryHelper
import gg.scala.commons.tablist.util.Util
import gg.scala.commons.tablist.util.data.TablistEntry
import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import gg.tropic.practice.games.event.GameCompleteEvent
import gg.tropic.practice.games.event.GameStartEvent
import gg.tropic.practice.games.event.PlayerJoinGameEvent
import gg.tropic.practice.games.event.PlayerRespawnEvent
import gg.tropic.practice.minigame.event.functionality.MiniGamePlayerDeathEvent
import gg.tropic.practice.minigame.event.PlayerMiniGameRejoinWithTokenEvent
import gg.tropic.practice.visuals.TeamVisuals
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scoreboard.DisplaySlot

/**
 * @author Limo
 * @date 2/19/2026
 */
class BedWarsTabSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val HEALTH_OBJECTIVE_NAME = "bw_health"
    }

    private val bedWarsProvider = BedWarsPlayersProvider(lifecycle)

    fun configure()
    {
        setupTabOverrides()
        registerEvents()
        startPeriodicSync()
    }

    private fun setupTabOverrides()
    {

        
        TabListService.provideFooterOverride { player ->
            val game = GameService.byPlayer(player) ?: return@provideFooterOverride null
            val bwLifecycle = game.miniGameLifecycle as? BedWarsLifecycle
                ?: return@provideFooterOverride null

            val kills = bwLifecycle.playerKills.getOrDefault(player.uniqueId, 0)
            val finalKills = bwLifecycle.playerFinalKills.getOrDefault(player.uniqueId, 0)
            val bedsBroken = bwLifecycle.playerBedsBroken.getOrDefault(player.uniqueId, 0)

            val defaultFooter = TabListService.localConfig()?.footer ?: emptyList()

            listOf(
                "",
                "${CC.WHITE}Kills: ${CC.GREEN}$kills ${CC.GRAY}| " +
                    "${CC.WHITE}Final Kills: ${CC.GREEN}$finalKills ${CC.GRAY}| " +
                    "${CC.WHITE}Beds Broken: ${CC.GREEN}$bedsBroken"
            ) + defaultFooter
        }
    }

    private fun registerEvents()
    {

        Events
            .subscribe(PlayerJoinGameEvent::class.java)
            .filter { it.game.identifier == lifecycle.game.identifier }
            .handler { event ->
                Schedulers.sync().runLater({
                    if (!event.player.isOnline) return@runLater
                    applyBedWarsProvider(event.player)
                    removeNonGamePlayers(event.player)
                    addGamePlayersFor(event.player)
                    announcePlayerToGame(event.player)
                }, 2L)
            }
            .bindWith(lifecycle)

        Events
            .subscribe(PlayerMiniGameRejoinWithTokenEvent::class.java)
            .filter { it.game === lifecycle }
            .handler { event ->
                Schedulers.sync().runLater({
                    if (!event.player.isOnline) return@runLater
                    applyBedWarsProvider(event.player)
                    removeNonGamePlayers(event.player)
                    addGamePlayersFor(event.player)
                    announcePlayerToGame(event.player)
                    if (lifecycle.game.state(GameState.Playing))
                    {
                        applyHealthObjective(event.player)
                        updateAllHealthScores()
                    }
                }, 2L)
            }
            .bindWith(lifecycle)

        Events
            .subscribe(GameStartEvent::class.java)
            .filter { it.game.identifier == lifecycle.game.identifier }
            .handler {
                Schedulers.sync().runLater({
                    val players = lifecycle.game.toBukkitPlayers().filterNotNull()
                    players.forEach { player ->
                        applyHealthObjective(player)
                    }
                    fullTabSync()
                }, 2L)
            }
            .bindWith(lifecycle)

        Events
            .subscribe(MiniGamePlayerDeathEvent::class.java)
            .filter { it.game === lifecycle }
            .handler { event ->
                Schedulers.sync().runLater({
                    removePlayerFromAliveTabs(event.player)
                    forceRefreshAllProviders()
                    updateAllHealthScores()
                }, 2L)
            }
            .bindWith(lifecycle)

        Events
            .subscribe(PlayerRespawnEvent::class.java)
            .filter { it.game.identifier == lifecycle.game.identifier }
            .handler { event ->
                Schedulers.sync().runLater({
                    if (!event.player.isOnline) return@runLater
                    addPlayerToAllTabs(event.player)
                    applyHealthObjective(event.player)
                    forceRefreshAllProviders()
                    updateAllHealthScores()

                    Schedulers.sync().runLater({
                        if (!event.player.isOnline) return@runLater
                        addPlayerToAllTabs(event.player)
                        ensurePlayerVisible(event.player)
                    }, 10L)
                }, 2L)
            }
            .bindWith(lifecycle)

        Events
            .subscribe(EntityDamageEvent::class.java)
            .filter { it.entity is Player }
            .handler { event ->
                val player = event.entity as Player
                val game = GameService.byPlayer(player) ?: return@handler
                if (game.identifier != lifecycle.game.identifier) return@handler

                Schedulers.sync().runLater({
                    if (player.isOnline) updateAllHealthScores()
                }, 1L)
            }
            .bindWith(lifecycle)

        Events
            .subscribe(EntityRegainHealthEvent::class.java)
            .filter { it.entity is Player }
            .handler { event ->
                val player = event.entity as Player
                val game = GameService.byPlayer(player) ?: return@handler
                if (game.identifier != lifecycle.game.identifier) return@handler

                Schedulers.sync().runLater({
                    if (player.isOnline) updateAllHealthScores()
                }, 1L)
            }
            .bindWith(lifecycle)

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler { event ->
                val objective = event.player.scoreboard
                    .getObjective(HEALTH_OBJECTIVE_NAME)
                objective?.unregister()

                Schedulers.sync().runLater({
                    forceRefreshAllProviders()
                }, 1L)
            }
            .bindWith(lifecycle)

        Events
            .subscribe(GameCompleteEvent::class.java)
            .filter { it.game.identifier == lifecycle.game.identifier }
            .handler { cleanup() }
            .bindWith(lifecycle)
    }

    private fun startPeriodicSync()
    {
        Schedulers
            .sync()
            .runRepeating({ _ ->
                forceRefreshAllProviders()

                val gamePlayers = lifecycle.game.toPlayers()
                val gameViewers = lifecycle.game.toBukkitPlayers().filterNotNull()

                gameViewers.forEach { viewer ->
                    val viewerIsSpectator = GameService.isSpectating(viewer)

                    val toRemove = mutableListOf<TablistEntry>()
                    for (online in Bukkit.getOnlinePlayers())
                    {
                        if (online.uniqueId == viewer.uniqueId) continue
                        if (online.uniqueId !in gamePlayers)
                        {
                            toRemove.add(
                                TablistEntry.builder()
                                    .userId(online.uniqueId)
                                    .name(online.name)
                                    .build()
                            )
                        }
                    }
                    if (toRemove.isNotEmpty())
                    {
                        Util.sendPacket(viewer, PacketUtil.createTablistPacket(
                            PacketUtil.TablistPacketAction.REMOVE_PLAYER, toRemove
                        ))
                    }

                    if (lifecycle.game.state(GameState.Playing))
                    {
                        val toUpdate = mutableListOf<TablistEntry>()
                        gameViewers.forEach inner@{ target ->
                            if (target.uniqueId == viewer.uniqueId) return@inner
                            val targetIsSpectator = GameService.isSpectating(target)
                            if (targetIsSpectator && !viewerIsSpectator) return@inner

                            toUpdate.add(buildDisplayNameEntry(target))
                        }
                        if (toUpdate.isNotEmpty())
                        {
                            Util.sendPacket(viewer, PacketUtil.createTablistPacket(
                                PacketUtil.TablistPacketAction.UPDATE_DISPLAY_NAME, toUpdate
                            ))
                        }
                    }
                }
            }, 5L, 5L)
            .bindWith(lifecycle)
    }

    private fun toJson(text: String): String
    {
        val escaped = text.replace("\\", "\\\\").replace("\"", "\\\"")
        return "{\"text\":\"$escaped\"}"
    }

    private fun formatDisplayName(player: Player): String
    {
        val team = lifecycle.game.getNullableTeam(player)
        if (team == null) return toJson(player.name)

        val teamColor = TeamVisuals.toChatColor(team.teamIdentifier)
        val readable = TeamVisuals.toReadable(teamColor)
        val letter = readable.first().uppercaseChar()
        return toJson("$teamColor[$letter] ${player.name}")
    }

    private fun buildPlayerEntry(player: Player): TablistEntry
    {
        return TablistEntryHelper.forPlayer(player)
            .displayName(formatDisplayName(player))
            .ping(1)
            .build()
    }

    private fun buildDisplayNameEntry(player: Player): TablistEntry
    {
        return TablistEntry.builder()
            .userId(player.uniqueId)
            .name(player.name)
            .displayName(formatDisplayName(player))
            .build()
    }

    private fun removeNonGamePlayers(viewer: Player)
    {
        val gamePlayers = lifecycle.game.toPlayers()
        val toRemove = mutableListOf<TablistEntry>()

        for (online in Bukkit.getOnlinePlayers())
        {
            if (online.uniqueId == viewer.uniqueId) continue
            if (online.uniqueId !in gamePlayers)
            {
                toRemove.add(
                    TablistEntry.builder()
                        .userId(online.uniqueId)
                        .name(online.name)
                        .build()
                )
            }
        }

        if (toRemove.isNotEmpty())
        {
            Util.sendPacket(viewer, PacketUtil.createTablistPacket(
                PacketUtil.TablistPacketAction.REMOVE_PLAYER, toRemove
            ))
        }
    }

    private fun addGamePlayersFor(viewer: Player)
    {
        val toAdd = mutableListOf<TablistEntry>()
        val viewerIsSpectator = GameService.isSpectating(viewer)

        lifecycle.game.toBukkitPlayers()
            .filterNotNull()
            .filter { it.uniqueId != viewer.uniqueId }
            .forEach { target ->
                if (lifecycle.game.state(GameState.Playing))
                {
                    val targetIsSpectator = GameService.isSpectating(target)
                    if (targetIsSpectator && !viewerIsSpectator) return@forEach
                }
                toAdd.add(buildPlayerEntry(target))
            }

        if (toAdd.isNotEmpty())
        {
            Util.sendPacket(viewer, PacketUtil.createTablistPacket(
                PacketUtil.TablistPacketAction.ADD_PLAYER, toAdd
            ))
        }
    }

    private fun announcePlayerToGame(newPlayer: Player)
    {
        val entry = buildPlayerEntry(newPlayer)
        val packet = PacketUtil.createTablistPacket(
            PacketUtil.TablistPacketAction.ADD_PLAYER, entry
        )

        lifecycle.game.toBukkitPlayers()
            .filterNotNull()
            .filter { it.uniqueId != newPlayer.uniqueId }
            .forEach { viewer ->
                Util.sendPacket(viewer, packet)
            }
    }

    private fun removePlayerFromAliveTabs(deadPlayer: Player)
    {
        val entry = TablistEntry.builder()
            .userId(deadPlayer.uniqueId)
            .name(deadPlayer.name)
            .build()

        val packet = PacketUtil.createTablistPacket(
            PacketUtil.TablistPacketAction.REMOVE_PLAYER, entry
        )

        lifecycle.game.toBukkitPlayers()
            .filterNotNull()
            .filter { it.uniqueId != deadPlayer.uniqueId }
            .filter { !GameService.isSpectating(it) }
            .forEach { viewer ->
                Util.sendPacket(viewer, packet)
            }
    }

    private fun addPlayerToAllTabs(respawnedPlayer: Player)
    {
        val entry = buildPlayerEntry(respawnedPlayer)
        val packet = PacketUtil.createTablistPacket(
            PacketUtil.TablistPacketAction.ADD_PLAYER, entry
        )

        lifecycle.game.toBukkitPlayers()
            .filterNotNull()
            .filter { it.uniqueId != respawnedPlayer.uniqueId }
            .forEach { viewer ->
                Util.sendPacket(viewer, packet)
            }
    }

    private fun ensurePlayerVisible(respawnedPlayer: Player)
    {
        val aliveViewers = lifecycle.game.toBukkitPlayers()
            .filterNotNull()
            .filter { it.uniqueId != respawnedPlayer.uniqueId }
            .filter { !GameService.isSpectating(it) }

        aliveViewers.forEach { viewer ->
            viewer.hidePlayer(respawnedPlayer)
        }

        Schedulers.sync().runLater({
            aliveViewers.filter { it.isOnline }.forEach { viewer ->
                viewer.showPlayer(respawnedPlayer)
            }

            Schedulers.sync().runLater({
                if (!respawnedPlayer.isOnline) return@runLater
                val entry = buildDisplayNameEntry(respawnedPlayer)
                val packet = PacketUtil.createTablistPacket(
                    PacketUtil.TablistPacketAction.UPDATE_DISPLAY_NAME, entry
                )
                aliveViewers.filter { it.isOnline }.forEach { viewer ->
                    Util.sendPacket(viewer, packet)
                }
            }, 1L)
        }, 1L)
    }

    private fun fullTabSync()
    {
        lifecycle.game.toBukkitPlayers()
            .filterNotNull()
            .forEach { viewer ->
                removeNonGamePlayers(viewer)
                addGamePlayersFor(viewer)
            }
    }

    private fun applyBedWarsProvider(player: Player)
    {
        val manager = TablistManager.getIfInitialized().orElse(null) ?: return
        val handle = manager.getTablist(player) ?: return
        val populator = handle.tablistPopulator

        if (populator is PlayerTablistPopulator)
        {
            populator.provider = bedWarsProvider
            populator.requireUpdate()
        }
    }

    private fun forceRefreshAllProviders()
    {
        val manager = TablistManager.getIfInitialized().orElse(null) ?: return

        lifecycle.game.toBukkitPlayers()
            .filterNotNull()
            .forEach { player ->
                val handle = manager.getTablist(player) ?: return@forEach
                val populator = handle.tablistPopulator

                if (populator is PlayerTablistPopulator)
                {
                    populator.onUpdate()
                }
            }
    }

    private fun applyHealthObjective(player: Player)
    {
        val scoreboard = player.scoreboard

        if (scoreboard.getObjective(HEALTH_OBJECTIVE_NAME) != null) return

        val objective = scoreboard.registerNewObjective(
            HEALTH_OBJECTIVE_NAME, "dummy"
        )
        objective.displaySlot = DisplaySlot.PLAYER_LIST
    }

    private fun updateAllHealthScores()
    {
        val allPlayers = lifecycle.game.toBukkitPlayers().filterNotNull()
        val alivePlayers = allPlayers.filter { !GameService.isSpectating(it) }
        val spectatingPlayers = allPlayers.filter { GameService.isSpectating(it) }

        allPlayers.forEach { viewer ->
            val objective = viewer.scoreboard
                .getObjective(HEALTH_OBJECTIVE_NAME) ?: return@forEach

            alivePlayers.forEach { target ->
                objective.getScore(target.name).score = Math.ceil(target.health).toInt()
            }

            spectatingPlayers.forEach { target ->
                viewer.scoreboard.resetScores(target.name)
            }
        }
    }

    fun cleanup()
    {
        val manager = TablistManager.getIfInitialized().orElse(null)
        val gamePlayers = lifecycle.game.toBukkitPlayers().filterNotNull()

        gamePlayers.forEach { player ->
            val objective = player.scoreboard
                .getObjective(HEALTH_OBJECTIVE_NAME)
            objective?.unregister()

            if (manager != null)
            {
                val handle = manager.getTablist(player) ?: return@forEach
                val populator = handle.tablistPopulator

                if (populator is PlayerTablistPopulator &&
                    populator.provider is BedWarsPlayersProvider)
                {
                    populator.provider = DefaultPlayersProvider.getInstance()
                    populator.requireUpdate()
                }
            }
        }

        val allEntries = Bukkit.getOnlinePlayers().map { player ->
            TablistEntryHelper.forPlayer(player)
                .displayName(toJson(player.displayName))
                .ping(1)
                .build()
        }

        if (allEntries.isNotEmpty())
        {
            val packet = PacketUtil.createTablistPacket(
                PacketUtil.TablistPacketAction.ADD_PLAYER, allEntries
            )
            gamePlayers.forEach { player ->
                Util.sendPacket(player, packet)
            }
        }
    }
}
