package gg.scala.minigame.bedwars

import gg.scala.minigame.bedwars.bed.BedTracker
import gg.scala.minigame.bedwars.chest.EnderChestSystem
import gg.scala.minigame.bedwars.chest.TeamChestSystem
import gg.scala.minigame.bedwars.cosmetic.CosmeticExecutor
import gg.scala.minigame.bedwars.items.MagicMilkSystem
import gg.scala.minigame.bedwars.items.BedBugSystem
import gg.scala.minigame.bedwars.items.BridgeEggSystem
import gg.scala.minigame.bedwars.items.DreamDefenderSystem
import gg.scala.minigame.bedwars.items.CompactPopSystem
import gg.scala.minigame.bedwars.items.SpongeSystem
import gg.scala.minigame.bedwars.items.FireballSystem
import gg.scala.minigame.bedwars.items.TrackerSystem
import gg.scala.minigame.bedwars.items.rotational.SugarCookieSystem
import gg.scala.minigame.bedwars.items.rotational.ThrowableTntSystem
import gg.scala.minigame.bedwars.items.rotational.MegaTntSystem
import gg.scala.minigame.bedwars.items.rotational.MiracleOfTheStarsSystem
import gg.scala.minigame.bedwars.items.rotational.UnstableTeleportationSystem
import gg.scala.minigame.bedwars.items.rotational.SugarkinSystem
import gg.scala.minigame.bedwars.items.rotational.IceBridgeSystem
import gg.scala.minigame.bedwars.items.rotational.FestivityMineSystem
import gg.scala.minigame.bedwars.items.rotational.ChickenTurretSystem
import gg.scala.minigame.bedwars.items.rotational.TimeWarpPearlSystem
import gg.scala.minigame.bedwars.items.rotational.SnowmanSystem
import gg.scala.minigame.bedwars.items.rotational.BridgeZapperSystem
import gg.scala.minigame.bedwars.items.rotational.BlockZapperSystem
import gg.scala.minigame.bedwars.items.rotational.LuckyChestSystem
import gg.scala.minigame.bedwars.items.rotational.BlackHoleSystem
import gg.scala.minigame.bedwars.items.rotational.CharlieTheUnicornSystem
import gg.scala.minigame.bedwars.items.rotational.PortableShopkeeperSystem
import gg.scala.minigame.bedwars.items.rotational.IllusionTrapSystem
import gg.scala.minigame.bedwars.items.rotational.BlockShufflerSystem
import gg.scala.minigame.bedwars.items.rotational.FinalReviveBeaconSystem
import gg.scala.minigame.bedwars.events.BedWarsGameEndEvent
import gg.scala.minigame.bedwars.events.BedDestructionGameEvent
import gg.scala.minigame.bedwars.events.DiamondGeneratorUpgradeEvent
import gg.scala.minigame.bedwars.events.EmeraldGeneratorUpgradeEvent
import gg.scala.minigame.bedwars.events.OvertimeGeneratorsGameEvent
import gg.scala.minigame.bedwars.generator.ResourceGeneratorSystem
import gg.scala.minigame.bedwars.shop.BedWarsNpcSystem
import gg.scala.minigame.bedwars.shop.BedWarsArmorTier
import gg.scala.minigame.bedwars.shop.BedWarsPlayerShopState
import gg.scala.minigame.bedwars.shared.hotbar.BedWarsHotbarCategory
import gg.scala.minigame.bedwars.shared.hotbar.BedWarsHotbarLayout
import gg.scala.minigame.bedwars.shared.profile.BedWarsProfileService
import gg.scala.minigame.bedwars.shared.profile.BedWarsStatistics
import gg.scala.minigame.bedwars.shared.profile.BedWarsTrackedStatisticType
import gg.scala.minigame.bedwars.team.BedWarsTeamPalette
import gg.scala.minigame.bedwars.team.BedWarsTeamUpgradeState
import gg.scala.minigame.bedwars.trap.BedWarsTrapQueueResult
import gg.scala.minigame.bedwars.trap.BedWarsTrapType
import gg.scala.minigame.bedwars.shared.BedWarsGameConfiguration
import gg.scala.minigame.bedwars.shared.BedWarsMode
import gg.scala.minigame.bedwars.shared.BedWarsTypeProvider
import gg.scala.game.extensions.profile.CorePlayerProfileService
import gg.scala.minigame.bedwars.chat.BedWarsTeamChatChannel
import gg.scala.minigame.bedwars.scoreboard.BedWarsTabSystem
import gg.scala.minigame.bedwars.shared.rotation.RotationalScheduleService
import gg.scala.minigame.bedwars.scoreboard.WaveAnimationTitle
import gg.tropic.practice.games.GameReport
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import gg.tropic.practice.games.enterSpectatorAfterLifeEnd
import gg.tropic.practice.games.event.GameCompleteEvent
import gg.tropic.practice.games.event.GameStartEvent
import gg.tropic.practice.games.event.PlayerJoinGameEvent
import gg.tropic.practice.games.event.PlayerRespawnEvent
import gg.tropic.practice.games.player.SpectatingPlayerProvider
import gg.tropic.practice.games.tasks.lifecycle.PlayerRespawnTask.Companion.startDelayedRespawn
import gg.tropic.practice.games.team.TeamIdentifier
import gg.tropic.practice.map.metadata.impl.MapGenMetadata
import gg.tropic.practice.minigame.AbstractMiniGameGameImpl
import gg.tropic.practice.minigame.MiniGameEvent
import gg.tropic.practice.minigame.MiniGameLifecycle
import gg.tropic.practice.minigame.MiniGameScoreboard
import gg.tropic.practice.minigame.MiniGameTypeMetadata
import gg.tropic.practice.minigame.event.functionality.MiniGameDestroyBedEvent
import gg.tropic.practice.minigame.event.functionality.MiniGamePlayerDeathEvent
import gg.tropic.practice.minigame.event.PlayerMiniGameRejoinWithTokenEvent
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.terminable.Terminable
import me.lucko.helper.terminable.composite.CompositeTerminable
import gg.tropic.practice.minigame.spectators.SpectatorSettingsService
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.time.TimeUtil
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.time.Duration
import java.util.LinkedList
import java.util.UUID
import gg.tropic.practice.visuals.TeamVisuals
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import gg.tropic.practice.games.damage.EliminationCause

/**
 * @author Limo
 * @date 2/13/2026
 */
class BedWarsLifecycle(
    override val configuration: BedWarsGameConfiguration,
    override val game: AbstractMiniGameGameImpl<BedWarsGameConfiguration>
) : MiniGameLifecycle<BedWarsGameConfiguration>, CompositeTerminable by CompositeTerminable.create()
{
    private val gameEvents = LinkedList<MiniGameEvent>().apply {
        this += DiamondGeneratorUpgradeEvent(
            lifecycle = this@BedWarsLifecycle,
            description = "Diamond II",
            duration = Duration.ofMinutes(6)
        )
        this += EmeraldGeneratorUpgradeEvent(
            lifecycle = this@BedWarsLifecycle,
            description = "Emerald II",
            duration = Duration.ofMinutes(6)
        )
        this += DiamondGeneratorUpgradeEvent(
            lifecycle = this@BedWarsLifecycle,
            description = "Diamond III",
            duration = Duration.ofMinutes(5)
        )
        this += EmeraldGeneratorUpgradeEvent(
            lifecycle = this@BedWarsLifecycle,
            description = "Emerald III",
            duration = Duration.ofMinutes(5)
        )
        this += BedDestructionGameEvent(
            lifecycle = this@BedWarsLifecycle,
            description = "Bed Destruction",
            duration = Duration.ofMinutes(6)
        )
        this += OvertimeGeneratorsGameEvent(
            lifecycle = this@BedWarsLifecycle,
            description = "Overtime",
            duration = Duration.ofMinutes(4)
        )
        this += BedWarsGameEndEvent(
            lifecycle = this@BedWarsLifecycle,
            description = "Game End",
            duration = Duration.ofMinutes(10)
        )
    }

    override val events: List<MiniGameEvent>
        get() = gameEvents

    val bedTracker = BedTracker(this)
    val generatorSystem = ResourceGeneratorSystem(this)
    val npcSystem = BedWarsNpcSystem(this)
    val teamChestSystem = TeamChestSystem(this)
    val enderChestSystem = EnderChestSystem(this)
    val magicMilkSystem = MagicMilkSystem(this)
    val bridgeEggSystem = BridgeEggSystem(this)
    val dreamDefenderSystem = DreamDefenderSystem(this)
    val compactPopSystem = CompactPopSystem(this)
    val spongeSystem = SpongeSystem(this)
    val fireballSystem = FireballSystem(this)
    val bedBugSystem = BedBugSystem(this)
    val trackerSystem = TrackerSystem(this)
    val sugarCookieSystem = SugarCookieSystem(this)
    val throwableTntSystem = ThrowableTntSystem(this)
    val megaTntSystem = MegaTntSystem(this)
    val miracleOfTheStarsSystem = MiracleOfTheStarsSystem(this)
    val unstableTeleportationSystem = UnstableTeleportationSystem(this)
    val sugarkinSystem = SugarkinSystem(this)
    val iceBridgeSystem = IceBridgeSystem(this)
    val festivityMineSystem = FestivityMineSystem(this)
    val chickenTurretSystem = ChickenTurretSystem(this)
    val timeWarpPearlSystem = TimeWarpPearlSystem(this)
    val snowmanSystem = SnowmanSystem(this)
    val bridgeZapperSystem = BridgeZapperSystem(this)
    val blockZapperSystem = BlockZapperSystem(this)
    val luckyChestSystem = LuckyChestSystem(this)
    val blackHoleSystem = BlackHoleSystem(this)
    val charlieTheUnicornSystem = CharlieTheUnicornSystem(this)
    val portableShopkeeperSystem = PortableShopkeeperSystem(this)
    val illusionTrapSystem = IllusionTrapSystem(this)
    val blockShufflerSystem = BlockShufflerSystem(this)
    val finalReviveBeaconSystem = FinalReviveBeaconSystem(this)
    val cosmeticExecutor = CosmeticExecutor(this)
    val tabSystem = BedWarsTabSystem(this)
    val teamChatChannel = BedWarsTeamChatChannel(this)
    val teamUpgradeStates = mutableMapOf<TeamIdentifier, BedWarsTeamUpgradeState>()
        .apply {
            game.teams.forEach { team ->
                put(team.teamIdentifier, BedWarsTeamUpgradeState())
            }
        }
    val playerShopStates = mutableMapOf<UUID, BedWarsPlayerShopState>()
    private val trapTriggerCooldowns = mutableMapOf<String, Long>()
    private val startupIssues = mutableListOf<String>()

    val playerKills = mutableMapOf<UUID, Int>()
    val playerFinalKills = mutableMapOf<UUID, Int>()
    val playerBedsBroken = mutableMapOf<UUID, Int>()

    override val typeConfiguration: MiniGameTypeMetadata = BedWarsTypeProvider.provide()
    override val scoreboard: MiniGameScoreboard = BedWarsScoreboard(game)

    override fun configure()
    {
        bedTracker.configure()
        generatorSystem.configure()
        npcSystem.configure()
        teamChestSystem.configure()
        enderChestSystem.configure()
        magicMilkSystem.configure()
        bridgeEggSystem.configure()
        dreamDefenderSystem.configure()
        compactPopSystem.configure()
        spongeSystem.configure()
        fireballSystem.configure()
        bedBugSystem.configure()
        trackerSystem.configure()
        sugarCookieSystem.configure()
        throwableTntSystem.configure()
        megaTntSystem.configure()
        miracleOfTheStarsSystem.configure()
        unstableTeleportationSystem.configure()
        sugarkinSystem.configure()
        iceBridgeSystem.configure()
        festivityMineSystem.configure()
        chickenTurretSystem.configure()
        timeWarpPearlSystem.configure()
        snowmanSystem.configure()
        bridgeZapperSystem.configure()
        blockZapperSystem.configure()
        luckyChestSystem.configure()
        blackHoleSystem.configure()
        charlieTheUnicornSystem.configure()
        portableShopkeeperSystem.configure()
        illusionTrapSystem.configure()
        blockShufflerSystem.configure()
        finalReviveBeaconSystem.configure()
        cosmeticExecutor.configure()
        tabSystem.configure()
        teamChatChannel.configure()
        RotationalScheduleService.subscribe()
        bind(Terminable { npcSystem.cleanup() })
        bind(Terminable { generatorSystem.cleanup() })
        bind(Terminable { dreamDefenderSystem.cleanup() })
        bind(Terminable { bedBugSystem.cleanup() })
        bind(Terminable { chickenTurretSystem.cleanup() })
        bind(Terminable { snowmanSystem.cleanup() })
        bind(Terminable { blackHoleSystem.cleanup() })
        bind(Terminable { charlieTheUnicornSystem.cleanup() })
        bind(Terminable { portableShopkeeperSystem.cleanup() })
        bind(Terminable { blockShufflerSystem.cleanup() })
        bind(Terminable { tabSystem.cleanup() })
        startupIssues += validateMapSetup()

        Events
            .subscribe(GameStartEvent::class.java)
            .filter { it.game.identifier == game.identifier }
            .handler {
                if (startupIssues.isNotEmpty())
                {
                    game.sendMessage(
                        "",
                        "${CC.RED}${CC.BOLD}Map Validation Failed",
                        *startupIssues
                            .map { issue -> "${CC.GRAY}- ${CC.RED}$issue" }
                            .toTypedArray(),
                        ""
                    )
                    game.complete(null, reason = "bedwars map validation failed")
                    return@handler
                }

                
                Schedulers
                    .sync()
                    .runLater({
                        game.toBukkitPlayers()
                            .filterNotNull()
                            .forEach(::applyStarterLoadout)
                    }, 1L)
            }
            .bindWith(this)

        Events
            .subscribe(PlayerRespawnEvent::class.java)
            .filter { it.game.identifier == game.identifier }
            .handler {
                applyStarterLoadout(it.player)

                GameService.audiences.player(it.player).showTitle(
                    Title.title(
                        Component.text("RESPAWNED!")
                            .color(NamedTextColor.GREEN)
                            .decorate(TextDecoration.BOLD),
                        Component.empty(),
                        Title.Times.times(
                            Duration.ZERO,
                            Duration.ofMillis(1500),
                            Duration.ofMillis(500)
                        )
                    )
                )
            }
            .bindWith(this)

        Events
            .subscribe(PlayerJoinGameEvent::class.java)
            .filter { it.game.identifier == game.identifier }
            .filter { game.state(GameState.Playing) }
            .handler {
                applyStarterLoadout(it.player)
            }
            .bindWith(this)

        Events
            .subscribe(PlayerMiniGameRejoinWithTokenEvent::class.java)
            .filter { it.game === this@BedWarsLifecycle }
            .handler {
                applyStarterLoadout(it.player)
            }
            .bindWith(this)

        Events
            .subscribe(PlayerInteractEvent::class.java)
            .filter { event ->
                GameService.byPlayer(event.player)?.identifier == game.identifier &&
                    !GameService.isSpectating(event.player)
            }
            .filter { event ->
                event.action.name.contains("RIGHT_CLICK")
            }
            .filter { event ->
                event.item?.type == Material.COMPASS
            }
            .handler { event ->
                event.isCancelled = true

                val team = game.getNullableTeam(event.player)
                    ?.teamIdentifier
                    ?: return@handler

                trackerSystem.openTrackerMenu(event.player, team)
            }
            .bindWith(this)

        Events
            .subscribe(MiniGameDestroyBedEvent::class.java)
            .filter { it.game === this@BedWarsLifecycle }
            .handler { event ->
                handleBedDestroy(event)
            }
            .bindWith(this)

        Events
            .subscribe(MiniGamePlayerDeathEvent::class.java)
            .filter { it.game === this@BedWarsLifecycle }
            .handler { event ->
                handlePlayerDeath(event)
            }
            .bindWith(this)

        Events
            .subscribe(GameCompleteEvent::class.java)
            .filter { it.game.identifier == game.identifier }
            .handler { event ->
                handleGameComplete(event)
            }
            .bindWith(this)

        Events
            .subscribe(gg.tropic.practice.minigame.event.PlayerMiniGameDisconnectMidGameEvent::class.java)
            .filter { it.game.identifier == game.identifier }
            .handler {

                Schedulers
                    .sync()
                    .runLater({
                        if (game.state(GameState.Playing))
                        {
                            checkForWinner()
                        }
                    }, 2L)
            }
            .bindWith(this)

        Schedulers
            .sync()
            .runRepeating({ _ ->
                if (!game.state(GameState.Playing))
                {
                    return@runRepeating
                }

                val world = game.arenaWorld
                    ?: return@runRepeating

                teamUpgradeStates
                    .filter { (_, state) -> state.healPool }
                    .forEach { (team, _) ->
                        val spawn = game.map.findSpawnLocationMatchingTeam(team)
                            ?.toLocation(world)
                            ?: return@forEach

                        teamPlayers(team).forEach { player ->
                            if (player.location.world != world)
                            {
                                return@forEach
                            }

                            if (player.location.distanceSquared(spawn) <= 36.0)
                            {
                                player.addPotionEffect(
                                    PotionEffect(
                                        PotionEffectType.REGENERATION,
                                        80,
                                        0,
                                        true,
                                        false
                                    )
                                )
                            }
                        }
                    }
            }, 20L, 20L)
            .bindWith(this)

        Schedulers
            .sync()
            .runRepeating({ _ ->
                checkAndTriggerTraps()
            }, 20L, 20L)
            .bindWith(this)

        Events
            .subscribe(GameStartEvent::class.java)
            .filter { it.game.identifier == game.identifier }
            .handler {
                Schedulers
                    .sync()
                    .runLater({
                        game.toBukkitPlayers()
                            .filterNotNull()
                            .forEach(::applyBedWarsXpBar)
                    }, 2L)
            }
            .bindWith(this)

        Events
            .subscribe(PlayerRespawnEvent::class.java)
            .filter { it.game.identifier == game.identifier }
            .handler {
                applyBedWarsXpBar(it.player)
            }
            .bindWith(this)

        Events
            .subscribe(org.bukkit.event.player.PlayerDropItemEvent::class.java)
            .filter { event ->
                GameService.byPlayer(event.player)?.identifier == game.identifier
            }
            .filter { event ->
                event.itemDrop.itemStack.type == Material.WOOD_SWORD
            }
            .handler { event ->
                event.isCancelled = true
            }
            .bindWith(this)

        Events
            .subscribe(org.bukkit.event.inventory.InventoryClickEvent::class.java)
            .filter { event ->
                event.whoClicked is Player &&
                    GameService.byPlayer(event.whoClicked as Player)?.identifier == game.identifier
            }
            .handler { event ->
                val slot = event.rawSlot
                val isArmorSlot = slot in 5..8

                if (isArmorSlot)
                {
                    event.isCancelled = true
                    return@handler
                }

                if (event.isShiftClick && event.currentItem != null)
                {
                    val itemType = event.currentItem!!.type
                    val isArmor = itemType.name.endsWith("_HELMET") ||
                        itemType.name.endsWith("_CHESTPLATE") ||
                        itemType.name.endsWith("_LEGGINGS") ||
                        itemType.name.endsWith("_BOOTS")

                    if (isArmor)
                    {
                        event.isCancelled = true
                    }
                }
            }
            .bindWith(this)

        Events
            .subscribe(org.bukkit.event.entity.EntityDamageEvent::class.java)
            .filter { event ->
                event.entity is Player &&
                    GameService.byPlayer(event.entity as Player)?.identifier == game.identifier
            }
            .filter { event ->
                event.cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                    event.cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
            }
            .handler { event ->
                if (event.damage > 1.0)
                {
                    event.damage = 1.0
                }
            }
            .bindWith(this)

        Events
            .subscribe(org.bukkit.event.player.PlayerBucketEmptyEvent::class.java)
            .filter { event ->
                GameService.byPlayer(event.player)?.identifier == game.identifier
            }
            .handler { event ->
                Schedulers.sync().runLater({
                    event.player.itemInHand = null
                }, 1L)
            }
            .bindWith(this)
    }

    private fun TeamIdentifier.coloredDisplay(): String
    {
        val color = TeamVisuals.toChatColor(this)
        return "$color${TeamVisuals.toReadable(color)}"
    }

    private fun applyBedWarsXpBar(player: Player)
    {
        val profile = CorePlayerProfileService.find(player)
            ?: return

        val levelData = profile.getGameLevelData("bedwars")
        val levelInfo = profile.getGameLevelInfo("bedwars")

        player.level = levelData.level

        val fraction = if (levelInfo.xpRequiredForNext > 0)
        {
            (levelInfo.currentXP.toFloat() / levelInfo.xpRequiredForNext.toFloat()).coerceIn(0f, 0.99f)
        } else
        {
            0f
        }

        player.exp = fraction
    }

    private fun validateMapSetup(): List<String>
    {
        val issues = mutableListOf<String>()
        val expectedTeams = game.teams
            .map { team -> team.teamIdentifier }

        if (game.teams.size != configuration.mode.teamCount)
        {
            issues += "Mode expects ${configuration.mode.teamCount} teams, but match has ${game.teams.size}."
        }

        if (configuration.maximumPlayers != configuration.mode.maxPlayers())
        {
            issues += "Mode max players mismatch (${configuration.maximumPlayers} vs ${configuration.mode.maxPlayers()})."
        }

        val teamsOverCapacity = game.teams
            .filter { team -> team.players.size > configuration.mode.teamSize }
        if (teamsOverCapacity.isNotEmpty())
        {
            issues += "One or more teams exceed team size ${configuration.mode.teamSize}."
        }

        if (expectedTeams.isEmpty())
        {
            issues += "No teams were generated for this expectation."
            return issues
        }

        val missingSpawns = expectedTeams
            .filter { team -> game.map.findSpawnLocationMatchingTeam(team) == null }
        if (missingSpawns.isNotEmpty())
        {
            issues += "Missing team spawns for: ${missingSpawns.joinToString(", ") { it.label }}"
        }

        val missingBeds = expectedTeams
            .filter { team -> !bedTracker.hasTrackedBed(team) }
        if (missingBeds.isNotEmpty())
        {
            issues += "Missing beds for: ${missingBeds.joinToString(", ") { it.label }}"
        }

        val hasGenerators = game.map.metadata.metadata
            .filterIsInstance<MapGenMetadata>()
            .isNotEmpty() && generatorSystem.totalGenerators() > 0
        if (!hasGenerators)
        {
            issues += "No resource generators found in map metadata."
        }

        return issues
    }

    fun teamState(team: TeamIdentifier) = teamUpgradeStates
        .getOrPut(team) { BedWarsTeamUpgradeState() }

    fun teamPlayers(team: TeamIdentifier): List<Player>
    {
        val gameTeam = game.teams
            .firstOrNull { entry -> entry.teamIdentifier == team }
            ?: return emptyList()

        return gameTeam.players
            .mapNotNull(org.bukkit.Bukkit::getPlayer)
    }

    fun notifyTeam(team: TeamIdentifier, message: String)
    {
        teamPlayers(team).forEach { player ->
            player.sendMessage(message)
        }
    }

    fun nextTrapCost(team: TeamIdentifier) = when (teamState(team).queuedTraps.size)
    {
        0 -> 1
        1 -> 2
        2 -> 4
        else -> 8
    }

    fun trapQueueResultFor(team: TeamIdentifier, trapType: BedWarsTrapType): BedWarsTrapQueueResult
    {
        val state = teamState(team)
        if (state.queuedTraps.size >= 3)
        {
            return BedWarsTrapQueueResult.QueueFull
        }

        if (state.queuedTraps.contains(trapType))
        {
            return BedWarsTrapQueueResult.AlreadyQueued
        }

        return BedWarsTrapQueueResult.Queued
    }

    fun queueTrap(team: TeamIdentifier, trapType: BedWarsTrapType): BedWarsTrapQueueResult
    {
        val validation = trapQueueResultFor(team, trapType)
        if (validation != BedWarsTrapQueueResult.Queued)
        {
            return validation
        }

        teamState(team).queuedTraps += trapType
        return BedWarsTrapQueueResult.Queued
    }

    fun playerShopState(player: Player) = playerShopStates
        .getOrPut(player.uniqueId) { BedWarsPlayerShopState() }

    private fun playerHotbarLayout(player: Player) = BedWarsProfileService
        .find(player)
        ?.resolveHotbarLayout()
        ?: BedWarsHotbarLayout.defaultLayout.toMutableMap()

    fun preferredHotbarSlot(player: Player, category: BedWarsHotbarCategory): Int?
    {
        return playerHotbarLayout(player)[category.id]
    }

    private fun giveOrDrop(player: Player, item: ItemStack)
    {
        val remainder = player.inventory.addItem(item)
        remainder.values.forEach { extra ->
            player.world.dropItemNaturally(player.location, extra)
        }
    }

    private fun placePreferredOrAdd(player: Player, category: BedWarsHotbarCategory, item: ItemStack)
    {
        val preferredSlot = preferredHotbarSlot(player, category)
        if (preferredSlot == null)
        {
            giveOrDrop(player, item)
            return
        }

        val existing = player.inventory.getItem(preferredSlot)
        if (existing == null || existing.type == Material.AIR)
        {
            player.inventory.setItem(preferredSlot, item)
            return
        }

        if (existing.isSimilar(item) && existing.amount < existing.maxStackSize)
        {
            val transferable = minOf(item.amount, existing.maxStackSize - existing.amount)
            existing.amount += transferable
            player.inventory.setItem(preferredSlot, existing)

            val remaining = item.amount - transferable
            if (remaining > 0)
            {
                giveOrDrop(
                    player,
                    ItemBuilder.copyOf(item)
                        .amount(remaining)
                        .build()
                )
            }

            return
        }

        player.inventory.setItem(preferredSlot, item)
        giveOrDrop(player, existing)
    }

    fun givePreferredOrDrop(player: Player, category: BedWarsHotbarCategory, item: ItemStack)
    {
        placePreferredOrAdd(player, category, item)
        player.updateInventory()
    }

    private fun applyEnchantmentIfHigher(item: ItemStack?, enchantment: Enchantment, level: Int)
    {
        if (item == null)
        {
            return
        }

        val current = item.getEnchantmentLevel(enchantment)
        if (current < level)
        {
            item.addUnsafeEnchantment(enchantment, level)
        }
    }

    fun applyTeamUpgrades(player: Player)
    {
        val team = game.getNullableTeam(player)
            ?.teamIdentifier
            ?: return

        val state = teamState(team)
        if (state.sharpness)
        {
            player.inventory.contents
                .filterNotNull()
                .filter { item -> item.type.name.endsWith("_SWORD") }
                .forEach { item ->
                    applyEnchantmentIfHigher(item, Enchantment.DAMAGE_ALL, 1)
                }
        }

        if (state.protectionLevel > 0)
        {
            val helmet = player.inventory.helmet
            val chestplate = player.inventory.chestplate
            val leggings = player.inventory.leggings
            val boots = player.inventory.boots

            applyEnchantmentIfHigher(helmet, Enchantment.PROTECTION_ENVIRONMENTAL, state.protectionLevel)
            applyEnchantmentIfHigher(chestplate, Enchantment.PROTECTION_ENVIRONMENTAL, state.protectionLevel)
            applyEnchantmentIfHigher(leggings, Enchantment.PROTECTION_ENVIRONMENTAL, state.protectionLevel)
            applyEnchantmentIfHigher(boots, Enchantment.PROTECTION_ENVIRONMENTAL, state.protectionLevel)

            player.inventory.helmet = helmet
            player.inventory.chestplate = chestplate
            player.inventory.leggings = leggings
            player.inventory.boots = boots
        }

        player.removePotionEffect(PotionEffectType.FAST_DIGGING)
        if (state.hasteLevel > 0)
        {
            player.addPotionEffect(
                PotionEffect(
                    PotionEffectType.FAST_DIGGING,
                    Int.MAX_VALUE,
                    state.hasteLevel - 1,
                    true,
                    false
                )
            )
        }

        player.updateInventory()
    }

    fun applyPermanentTools(player: Player)
    {
        val state = playerShopState(player)
        val pickaxeTier = state.currentPickaxeTier()
        val axeTier = state.currentAxeTier()

        player.inventory.contents.forEachIndexed { slot, item ->
            if (item == null)
            {
                return@forEachIndexed
            }

            if (item.type.name.endsWith("_PICKAXE") || item.type.name.endsWith("_AXE") || item.type == Material.SHEARS)
            {
                player.inventory.setItem(slot, null)
            }
        }

        if (pickaxeTier != null)
        {
            placePreferredOrAdd(
                player,
                BedWarsHotbarCategory.Pickaxe,
                ItemBuilder
                    .of(pickaxeTier.material)
                    .setUnbreakable(true)
                    .build()
            )
        }

        if (axeTier != null)
        {
            placePreferredOrAdd(
                player,
                BedWarsHotbarCategory.Axe,
                ItemBuilder
                    .of(axeTier.material)
                    .setUnbreakable(true)
                    .build()
            )
        }

        if (state.hasShears)
        {
            placePreferredOrAdd(
                player,
                BedWarsHotbarCategory.Shears,
                ItemBuilder
                    .of(Material.SHEARS)
                    .setUnbreakable(true)
                    .build()
            )
        }

        player.updateInventory()
    }

    private fun applyArmorTier(player: Player, armorTier: BedWarsArmorTier, dyedArmorColor: org.bukkit.Color)
    {

        player.inventory.helmet = ItemBuilder
            .of(Material.LEATHER_HELMET)
            .color(dyedArmorColor)
            .setUnbreakable(true)
            .build()
        player.inventory.chestplate = ItemBuilder
            .of(Material.LEATHER_CHESTPLATE)
            .color(dyedArmorColor)
            .setUnbreakable(true)
            .build()

        when (armorTier)
        {
            BedWarsArmorTier.Leather ->
            {
                player.inventory.leggings = ItemBuilder
                    .of(Material.LEATHER_LEGGINGS)
                    .color(dyedArmorColor)
                    .setUnbreakable(true)
                    .build()
                player.inventory.boots = ItemBuilder
                    .of(Material.LEATHER_BOOTS)
                    .color(dyedArmorColor)
                    .setUnbreakable(true)
                    .build()
            }

            BedWarsArmorTier.Chainmail ->
            {
                player.inventory.leggings = ItemBuilder.of(Material.CHAINMAIL_LEGGINGS).setUnbreakable(true).build()
                player.inventory.boots = ItemBuilder.of(Material.CHAINMAIL_BOOTS).setUnbreakable(true).build()
            }

            BedWarsArmorTier.Iron ->
            {
                player.inventory.leggings = ItemBuilder.of(Material.IRON_LEGGINGS).setUnbreakable(true).build()
                player.inventory.boots = ItemBuilder.of(Material.IRON_BOOTS).setUnbreakable(true).build()
            }

            BedWarsArmorTier.Diamond ->
            {
                player.inventory.leggings = ItemBuilder.of(Material.DIAMOND_LEGGINGS).setUnbreakable(true).build()
                player.inventory.boots = ItemBuilder.of(Material.DIAMOND_BOOTS).setUnbreakable(true).build()
            }
        }
    }

    fun applyArmorForCurrentState(player: Player)
    {
        val team = game.getNullableTeam(player)
            ?.teamIdentifier
            ?: return

        val teamColor = TeamVisuals.toChatColor(team)
        val armorColor = TeamVisuals.toArmorColor(teamColor)
        val state = playerShopState(player)

        applyArmorTier(player, state.armorTier, armorColor)
        applyTeamUpgrades(player)
        player.updateInventory()
    }

    private fun checkAndTriggerTraps()
    {
        if (!game.state(GameState.Playing))
        {
            return
        }

        val world = game.arenaWorld
            ?: return

        val now = System.currentTimeMillis()
        trapTriggerCooldowns.entries.removeIf { (_, until) -> until <= now }

        val players = game.toBukkitPlayers()
            .filterNotNull()

        teamUpgradeStates.forEach { (defendingTeam, state) ->
            val queuedTrap = state.queuedTraps.firstOrNull()
                ?: return@forEach

            val anchors = mutableListOf<org.bukkit.Location>()
            game.map.findSpawnLocationMatchingTeam(defendingTeam)
                ?.toLocation(world)
                ?.let(anchors::add)
            anchors += bedTracker.bedLocations(defendingTeam, world)

            if (anchors.isEmpty())
            {
                return@forEach
            }

            val intruder = players.firstOrNull { player ->
                if (SpectatingPlayerProvider.spectating(player))
                {
                    return@firstOrNull false
                }

                val intruderTeam = game.getNullableTeam(player)
                    ?.teamIdentifier
                    ?: return@firstOrNull false

                if (intruderTeam == defendingTeam)
                {
                    return@firstOrNull false
                }

                if (player.location.world != world)
                {
                    return@firstOrNull false
                }

                val inTrapRegion = anchors.any { anchor ->
                    player.location.distanceSquared(anchor) <= 64.0
                }
                if (!inTrapRegion)
                {
                    return@firstOrNull false
                }

                val cooldownKey = "${defendingTeam.label}:${player.uniqueId}"
                val cooldownUntil = trapTriggerCooldowns[cooldownKey] ?: 0L
                if (now < cooldownUntil)
                {
                    return@firstOrNull false
                }

                !magicMilkSystem.hasImmunity(player.uniqueId)
            } ?: return@forEach

            state.queuedTraps.removeAt(0)
            trapTriggerCooldowns["${defendingTeam.label}:${intruder.uniqueId}"] = now + 10_000L
            triggerTrap(defendingTeam, intruder, queuedTrap)
        }
    }

    private fun triggerTrap(defendingTeam: TeamIdentifier, intruder: Player, trapType: BedWarsTrapType)
    {
        notifyTeam(
            defendingTeam,
            "${CC.RED}${CC.BOLD}${trapType.displayName} was set off!"
        )

        teamPlayers(defendingTeam).forEach { defender ->
            defender.playSound(defender.location, Sound.NOTE_PLING, 1.0f, 1.4f)

            GameService.audiences.player(defender).showTitle(
                Title.title(
                    Component
                        .text("TRAP TRIGGERED!")
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD),
                    Component
                        .text("Your ${trapType.displayName} has been set off!")
                        .color(NamedTextColor.WHITE),
                    Title.Times.times(
                        Duration.ofMillis(100L),
                        Duration.ofMillis(2000L),
                        Duration.ofMillis(500L)
                    )
                )
            )
        }

        when (trapType)
        {
            BedWarsTrapType.ItsATrap ->
            {
                intruder.removePotionEffect(PotionEffectType.INVISIBILITY)
                intruder.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 20 * 8, 0, true, false))
                intruder.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 20 * 8, 1, true, false))
            }

            BedWarsTrapType.Alarm ->
            {
                intruder.removePotionEffect(PotionEffectType.INVISIBILITY)
                intruder.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 20 * 2, 0, true, false))
            }

            BedWarsTrapType.MinerFatigue ->
            {
                intruder.removePotionEffect(PotionEffectType.INVISIBILITY)
                intruder.addPotionEffect(PotionEffect(PotionEffectType.SLOW_DIGGING, 20 * 10, 1, true, false))
            }

            BedWarsTrapType.CounterOffensive ->
            {
                teamPlayers(defendingTeam).forEach { defender ->
                    defender.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 20 * 10, 0, true, false))
                    defender.addPotionEffect(PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 10, 0, true, false))
                }
            }
        }
    }

    private fun handleGameComplete(event: GameCompleteEvent)
    {
        val report = event.game.report
            ?: return

        sendGameSummary(report)

        game.toBukkitPlayers()
            .filterNotNull()
            .forEach { player ->
                val profile = CorePlayerProfileService.find(player)
                    ?: return@forEach

                val isWinner = report.winners.contains(player.uniqueId)
                var totalCoins = 0L

                if (isWinner)
                {
                    totalCoins += 100
                } else
                {
                    totalCoins += 25
                }

                val kills = playerKills.getOrDefault(player.uniqueId, 0)
                val finalKills = playerFinalKills.getOrDefault(player.uniqueId, 0)
                val bedsBroken = playerBedsBroken.getOrDefault(player.uniqueId, 0)

                totalCoins += kills * 5L
                totalCoins += finalKills * 10L
                totalCoins += bedsBroken * 15L

                if (totalCoins > 0)
                {
                    profile.addGameBalance(
                        currencyId = "bedwars_coins",
                        amount = totalCoins
                    ) { feedback ->
                        player.sendMessage(feedback)
                    }

                    profile.addGameExperience(
                        levelSystemId = "bedwars",
                        amount = totalCoins
                    ) { feedback ->
                        player.sendMessage(feedback)
                    }
                }
            }
    }

    private fun sendGameSummary(report: GameReport)
    {
        val separator = "${CC.GRAY}${CC.STRIKE_THROUGH}${" ".repeat(53)}"

        val winnerUUID = report.winners.firstOrNull()
        val winnerTeamId = winnerUUID?.let { uuid ->
            game.teams
                .firstOrNull { team -> team.players.contains(uuid) }
                ?.teamIdentifier
        }
        val winnerDisplay = if (winnerTeamId != null)
        {
            "${winnerTeamId.coloredDisplay()}${CC.WHITE} - Winners!"
        } else
        {
            "${CC.GRAY}No Winner"
        }

        val topKillers = playerKills.entries
            .sortedByDescending { it.value }
            .take(3)

        val topFinalKillers = playerFinalKills.entries
            .sortedByDescending { it.value }
            .take(3)

        val topBedBreakers = playerBedsBroken.entries
            .sortedByDescending { it.value }
            .take(3)

        fun formatLeaderboard(title: String, entries: List<Map.Entry<UUID, Int>>): List<String>
        {
            val lines = mutableListOf<String>()
            lines += "${CC.GOLD}$title"

            if (entries.isEmpty())
            {
                lines += "${CC.GRAY}  None"
                return lines
            }

            entries.forEachIndexed { index, entry ->
                val name = org.bukkit.Bukkit.getOfflinePlayer(entry.key).name ?: "Unknown"
                val teamId = game.teams
                    .firstOrNull { team -> team.players.contains(entry.key) }
                    ?.teamIdentifier
                val color = teamId?.let(TeamVisuals::toChatColor) ?: CC.GRAY
                lines += "${CC.YELLOW}  ${index + 1}. $color$name${CC.GRAY} - ${CC.GREEN}${entry.value}"
            }

            return lines
        }

        val summary = mutableListOf<String>()
        summary += separator
        summary += "${CC.B_YELLOW}                   BED WARS"
        summary += ""
        summary += "              $winnerDisplay"
        summary += ""
        summary += formatLeaderboard("Kills", topKillers)
        summary += formatLeaderboard("Final Kills", topFinalKillers)
        summary += formatLeaderboard("Beds Broken", topBedBreakers)
        summary += ""
        summary += separator

        game.toBukkitPlayers()
            .filterNotNull()
            .forEach { player ->
                summary.forEach(player::sendMessage)
            }
    }

    private fun applyStarterLoadout(player: Player)
    {
        if (!game.state(GameState.Playing))
        {
            return
        }

        val team = game.getNullableTeam(player)
            ?.teamIdentifier
            ?: return

        val teamColor = TeamVisuals.toChatColor(team)
        val armorColor = TeamVisuals.toArmorColor(teamColor)
        val state = playerShopState(player)

        player.gameMode = GameMode.SURVIVAL
        player.inventory.clear()
        applyArmorTier(player, state.armorTier, armorColor)

        placePreferredOrAdd(
            player,
            BedWarsHotbarCategory.Melee,
            ItemBuilder.of(Material.WOOD_SWORD).setUnbreakable(true).build()
        )

        placePreferredOrAdd(
            player,
            BedWarsHotbarCategory.Compass,
            ItemBuilder
                .of(Material.COMPASS)
                .name("${CC.GREEN}Tracker ${CC.GRAY}(Right Click)")
                .build()
        )
        applyPermanentTools(player)
        applyTeamUpgrades(player)
    }

    private fun handleBedDestroy(event: MiniGameDestroyBedEvent)
    {
        event.event.isCancelled = true
        if (!game.state(GameState.Playing))
        {
            return
        }

        if (!bedTracker.isBedAlive(event.teamDestroyed))
        {
            event.destroyedBy.sendMessage("${CC.RED}That bed is already destroyed!")
            return
        }

        val destroyingTeam = game.getNullableTeam(event.destroyedBy)
            ?.teamIdentifier
            ?: return

        if (destroyingTeam == event.teamDestroyed)
        {
            event.destroyedBy.sendMessage("${CC.RED}You cannot break your own bed!")
            return
        }

        if (!bedTracker.destroy(event.teamDestroyed))
        {
            return
        }

        playerBedsBroken.merge(event.destroyedBy.uniqueId, 1, Int::plus)

        BedWarsStatistics.increment(
            player = event.destroyedBy.uniqueId,
            mode = configuration.mode,
            type = BedWarsTrackedStatisticType.BedsBroken
        )

        game.playSound(Sound.ENDERDRAGON_GROWL, 1.0f)
        game.sendMessage(
            "",
            "${CC.B_WHITE}BED DESTRUCTION ${CC.GRAY}> ${event.teamDestroyed.coloredDisplay()} Bed ${CC.GRAY}was destroyed by ${TeamVisuals.toChatColor(destroyingTeam)}${event.destroyedBy.name}${CC.GRAY}!",
            ""
        )

        val destroyedTeam = game.teams
            .firstOrNull { team -> team.teamIdentifier == event.teamDestroyed }
            ?: return

        val bedDestroyTitle = Title.title(
            Component.text("BED DESTROYED!")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD),
            Component.text("You will no longer respawn!")
                .color(NamedTextColor.WHITE),
            Title.Times.times(
                Duration.ZERO,
                Duration.ofMillis(2500),
                Duration.ofMillis(500)
            )
        )

        destroyedTeam.nonSpectators().forEach { member ->
            GameService.audiences.player(member).showTitle(bedDestroyTitle)
        }

        if (destroyedTeam.nonSpectators().isEmpty())
        {
            game.sendMessage(
                "",
                "${CC.B_WHITE}TEAM ELIMINATED ${CC.GRAY}> ${event.teamDestroyed.coloredDisplay()} Team ${CC.RED}has been eliminated!",
                ""
            )
            checkForWinner()
        }
    }

    private fun buildDeathMessage(
        event: MiniGamePlayerDeathEvent,
        victimColor: org.bukkit.ChatColor,
        isFinalKill: Boolean
    ): String
    {
        val killer = event.killer as? Player
        val cause = event.eliminationCause
        val hasKiller = killer != null && killer.uniqueId != event.player.uniqueId

        val killerColor = if (hasKiller)
        {
            val killerTeam = game.getNullableTeam(killer!!)?.teamIdentifier
            killerTeam?.let(TeamVisuals::toChatColor) ?: CC.GRAY
        } else ""

        if (hasKiller)
        {
            val killerTeamId = game.getNullableTeam(killer!!)?.teamIdentifier
            val victimTeamId = game.getNullableTeam(event.player)?.teamIdentifier

            if (killerTeamId != null && victimTeamId != null)
            {
                val customMsg = cosmeticExecutor.buildCustomKillMessage(
                    killer, event.player, killerTeamId, victimTeamId
                )
                if (customMsg != null)
                {
                    return if (isFinalKill) "$customMsg ${CC.AQUA}${CC.BOLD}FINAL KILL!" else customMsg
                }
            }
        }

        val base = when
        {
            cause == EliminationCause.VOID_DAMAGE && hasKiller ->
                "$victimColor${event.player.name} ${CC.GRAY}was knocked into the void by $killerColor${killer!!.name}${CC.GRAY}."

            cause == EliminationCause.VOID_DAMAGE ->
                "$victimColor${event.player.name} ${CC.GRAY}fell into the void."

            cause == EliminationCause.EXPLOSION && hasKiller ->
                "$victimColor${event.player.name} ${CC.GRAY}was hit off by a love bomb from $killerColor${killer!!.name}${CC.GRAY}."

            cause == EliminationCause.EXPLOSION ->
                "$victimColor${event.player.name} ${CC.GRAY}was hit off by a bomb."

            cause == EliminationCause.PROJECTILE_BY_PLAYER && hasKiller ->
                "$victimColor${event.player.name} ${CC.GRAY}was shot by $killerColor${killer!!.name}${CC.GRAY}!"

            cause == EliminationCause.FALL_DAMAGE && hasKiller ->
                "$victimColor${event.player.name} ${CC.GRAY}was pushed off by $killerColor${killer!!.name}${CC.GRAY}."

            cause == EliminationCause.FALL_DAMAGE ->
                "$victimColor${event.player.name} ${CC.GRAY}fell to their death."

            cause == EliminationCause.FIRE_DAMAGE || cause == EliminationCause.LAVA_DAMAGE ->
                if (hasKiller) "$victimColor${event.player.name} ${CC.GRAY}was burnt to a " +
                    "crisp whilst fighting $killerColor${killer!!.name}${CC.GRAY}."
                else "$victimColor${event.player.name} ${CC.GRAY}burned to death."

            hasKiller ->
                "$victimColor${event.player.name} ${CC.GRAY}was killed by $killerColor${killer!!.name}${CC.GRAY}."

            else ->
                "$victimColor${event.player.name} ${CC.GRAY}died."
        }

        return if (isFinalKill) "$base ${CC.AQUA}${CC.BOLD}FINAL KILL!" else base
    }

    private fun handlePlayerDeath(event: MiniGamePlayerDeathEvent)
    {
        if (!game.state(GameState.Playing))
        {
            return
        }

        val team = game.getNullableTeam(event.player)
            ?: return

        val teamIdentifier = team.teamIdentifier

        val resourceMaterials = setOf(
            Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND, Material.EMERALD
        )

        val collectedResources = mutableMapOf<Material, Int>()
        event.player.inventory.contents?.forEach { item ->
            if (item != null && item.type in resourceMaterials)
            {
                collectedResources.merge(item.type, item.amount, Int::plus)
            }
        }

        val killer = event.killer as? Player
        if (killer != null && killer.uniqueId != event.player.uniqueId && collectedResources.isNotEmpty())
        {
            collectedResources.forEach { (material, amount) ->
                val stack = org.bukkit.inventory.ItemStack(material, amount)
                val overflow = killer.inventory.addItem(stack)
                overflow.values.forEach { leftover ->
                    killer.world.dropItemNaturally(killer.location, leftover)
                }
            }

            val summary = collectedResources.entries.joinToString("${CC.GRAY}, ") { (mat, amt) ->
                val name = when (mat)
                {
                    Material.IRON_INGOT -> "${CC.WHITE}Iron"
                    Material.GOLD_INGOT -> "${CC.GOLD}Gold"
                    Material.DIAMOND -> "${CC.AQUA}Diamond"
                    Material.EMERALD -> "${CC.GREEN}Emerald"
                    else -> mat.name
                }
                "$name${CC.GRAY} x$amt"
            }

            killer.sendMessage("${CC.GREEN}+${CC.GRAY} You received $summary${CC.GRAY} from ${event.player.name}!")
            event.player.sendMessage("${CC.RED}You lost your resources to ${killer.name}!")
        }

        event.drops.clear()

        val victimColor = TeamVisuals.toChatColor(teamIdentifier)

        if (bedTracker.isBedAlive(teamIdentifier))
        {
            val state = playerShopState(event.player)
            if (state.pickaxeLevel > 1)
            {
                state.pickaxeLevel -= 1
            }

            if (state.axeLevel > 1)
            {
                state.axeLevel -= 1
            }

            if (killer != null && killer.uniqueId != event.player.uniqueId)
            {
                playerKills.merge(killer.uniqueId, 1, Int::plus)
            }

            game.sendMessage(
                buildDeathMessage(event, victimColor, isFinalKill = false)
            )

            game.startDelayedRespawn(event.player)
            return
        }

        Schedulers
            .sync()
            .runLater({
                if (!event.player.isOnline)
                {
                    return@runLater
                }

                game.enterSpectatorAfterLifeEnd(event.player, final = true)

                if (killer != null && killer.uniqueId != event.player.uniqueId)
                {
                    SpectatorSettingsService.startTracking(event.player, killer, firstPerson = false)
                }
            }, 2L)
        playerShopStates.remove(event.player.uniqueId)

        if (killer != null && killer.uniqueId != event.player.uniqueId)
        {
            playerFinalKills.merge(killer.uniqueId, 1, Int::plus)
            playerKills.merge(killer.uniqueId, 1, Int::plus)

            BedWarsStatistics.increment(
                player = killer.uniqueId,
                mode = configuration.mode,
                type = BedWarsTrackedStatisticType.FinalKills
            )

            GameService.runFinalDeathEffectsFor(
                killer = killer,
                killed = event.player,
                game = game,
                invasive = false
            )
        }

        BedWarsStatistics.increment(
            player = event.player.uniqueId,
            mode = configuration.mode,
            type = BedWarsTrackedStatisticType.FinalDeaths
        )

        game.sendMessage(
            buildDeathMessage(event, victimColor, isFinalKill = true)
        )

        if (team.nonSpectators().isEmpty())
        {
            game.sendMessage(
                "",
                "${CC.B_WHITE}TEAM ELIMINATED ${CC.GRAY}> ${teamIdentifier.coloredDisplay()} Team ${CC.RED}has been eliminated!",
                ""
            )
            checkForWinner()
        }
    }

    private fun checkForWinner()
    {
        val remainingTeams = game.teams.filter { team ->
            bedTracker.isBedAlive(team.teamIdentifier) || team.nonSpectators().isNotEmpty()
        }

        if (remainingTeams.isEmpty())
        {
            game.complete(null)
            return
        }

        if (remainingTeams.size == 1)
        {
            game.complete(remainingTeams.first())
        }
    }

    inner class BedWarsScoreboard(
        override val game: AbstractMiniGameGameImpl<BedWarsGameConfiguration>
    ) : MiniGameScoreboard
    {
        override fun titleFor(player: Player) = WaveAnimationTitle.getCurrentTitle()

        private fun modeDisplay(): String
        {
            if (configuration.mode == BedWarsMode.FOURS)
            {
                return "4v4"
            }

            return configuration.mode.name
                .lowercase()
                .replaceFirstChar { char -> char.uppercaseChar() }
        }

        private fun eventDisplay(description: String): String
        {
            return if (description.length > 14)
            {
                description.substring(0, 14)
            } else
            {
                description
            }
        }

        private fun appendTeamStatuses(player: Player, board: MutableList<String>)
        {
            val viewerTeam = game.getNullableTeam(player)?.teamIdentifier

            game.teams
                .sortedBy { team -> team.teamIdentifier.priority() }
                .take(configuration.mode.teamCount)
                .forEach { team ->
                    val identifier = team.teamIdentifier
                    val color = TeamVisuals.toChatColor(identifier)
                    val readable = TeamVisuals.toReadable(color)
                    val letter = readable.first().uppercaseChar()
                    val alivePlayers = team.nonSpectators().size
                    val bedAlive = bedTracker.isBedAlive(identifier)
                    val isYou = identifier == viewerTeam

                    val status = when
                    {
                        alivePlayers <= 0 -> "${CC.RED}\u2717"
                        bedAlive -> "${CC.GREEN}\u2713"
                        else -> "${CC.YELLOW}$alivePlayers"
                    }

                    val youTag = if (isYou) " ${CC.GRAY}(YOU)" else ""
                    board += "$color$letter${CC.WHITE} ${TeamVisuals.toReadable(color)} $status$youTag"
                }
        }

        override fun createWaitingScoreboardFor(player: Player, board: MutableList<String>)
        {
            board += ""
            board += "${CC.WHITE}Map: ${CC.GREEN}${game.map.displayName}"
            board += "${CC.WHITE}Players: ${CC.GREEN}${game.allNonSpectators().size}${CC.GRAY}/${configuration.maximumPlayers}"
            board += ""
            board += "${CC.WHITE}Mode: ${CC.AQUA}${modeDisplay()}"
            board += "${CC.WHITE}Waiting..."
        }

        override fun createStartingScoreboardFor(player: Player, board: MutableList<String>)
        {
            board += ""
            board += "${CC.WHITE}Map: ${CC.GREEN}${game.map.displayName}"
            board += "${CC.WHITE}Players: ${CC.GREEN}${game.allNonSpectators().size}${CC.GRAY}/${configuration.maximumPlayers}"
            board += ""
            board += "${CC.WHITE}Starting in ${CC.GREEN}${TimeUtil.formatIntoMMSS(game.startCountDown)}"
            board += "${CC.WHITE}Mode: ${CC.AQUA}${modeDisplay()}"
        }

        override fun createInGameScoreboardFor(player: Player, board: MutableList<String>)
        {
            val nextEvent = game.tracker.nextEvent()
            val nextEventTime = TimeUtil.formatIntoMMSS((game.tracker.timeUntilNextEvent() / 1000L).toInt())
            val team = game.getNullableTeam(player)?.teamIdentifier

            board += ""
            board += "${CC.WHITE}${eventDisplay(nextEvent?.description ?: "None")} in ${CC.GREEN}$nextEventTime"
            board += ""
            appendTeamStatuses(player, board)
            board += ""
        }

        override fun createEndingScoreboardFor(player: Player, report: GameReport?, board: MutableList<String>)
        {
            board += ""
            board += "${CC.WHITE}Game Over"
            board += ""
            board += "${CC.WHITE}Mode: ${CC.AQUA}${modeDisplay()}"
            board += "${CC.WHITE}Map: ${CC.GREEN}${game.map.displayName}"
        }
    }
}
