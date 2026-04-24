package gg.scala.minigame.bedwars.cosmetic

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.scala.minigame.bedwars.shared.cosmetic.KillMessageType
import gg.scala.minigame.bedwars.shared.cosmetic.WoodSkinType
import gg.scala.minigame.bedwars.shared.profile.BedWarsProfileService
import gg.scala.minigame.bedwars.shop.menu.BedWarsItemShopMenu
import gg.scala.minigame.bedwars.shop.menu.BedWarsUpgradeMenu
import gg.scala.minigame.bedwars.team.BedWarsTeamPalette
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import gg.tropic.practice.games.event.GameCompleteEvent
import gg.tropic.practice.games.event.GameStartEvent
import gg.tropic.practice.games.team.TeamIdentifier
import gg.tropic.practice.map.metadata.impl.MapShopKeeperMetadata
import gg.tropic.practice.map.metadata.impl.MapTeamUpgraderMetadata
import gg.tropic.practice.minigame.event.functionality.MiniGameDestroyBedEvent
import gg.tropic.practice.minigame.event.functionality.MiniGamePlayerDeathEvent
import gg.tropic.practice.visuals.TeamVisuals
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import net.minecraft.server.v1_8_R3.NBTTagCompound
import org.bukkit.Color
import org.bukkit.DyeColor
import org.bukkit.Effect
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Bat
import org.bukkit.entity.Blaze
import org.bukkit.entity.Creature
import org.bukkit.entity.Creeper
import org.bukkit.entity.Enderman
import org.bukkit.entity.EntityType
import org.bukkit.entity.Fireball
import org.bukkit.entity.Firework
import org.bukkit.entity.Guardian
import org.bukkit.entity.Horse
import org.bukkit.entity.IronGolem
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Sheep
import org.bukkit.entity.Silverfish
import org.bukkit.entity.Skeleton
import org.bukkit.entity.Squid
import org.bukkit.entity.EnderDragon
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Pig
import org.bukkit.entity.Wither
import org.bukkit.entity.WitherSkull
import org.bukkit.entity.Witch
import org.bukkit.entity.Zombie
import org.bukkit.entity.PigZombie
import org.bukkit.entity.Snowman
import gg.scala.minigame.bedwars.shared.cosmetic.ShopKeeperSkinType
import net.evilblock.cubed.entity.npc.NpcEntity
import net.evilblock.cubed.entity.EntityHandler
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.FireworkMeta
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.material.MaterialData
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import me.lucko.helper.terminable.Terminable
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * @author Limo
 * @date 2/20/2026
 */
class CosmeticExecutor(
    private val lifecycle: BedWarsLifecycle
)
{
    private val game get() = lifecycle.game
    private val shopKeeperEntities = mutableListOf<org.bukkit.entity.Entity>()
    private val upgraderEntities = mutableListOf<org.bukkit.entity.Entity>()
    private val hologramStands = mutableListOf<ArmorStand>()
    private val victoryDanceEntities = mutableListOf<org.bukkit.entity.Entity>()
    private val victoryDanceTasks = mutableListOf<me.lucko.helper.terminable.Terminable>()

    fun configure()
    {
        configureProjectileTrails()
        configureWoodSkins()
        configureGlyphs()
        configureIslandToppers()
        configureShopKeeperSkins()

        Events
            .subscribe(MiniGamePlayerDeathEvent::class.java)
            .filter { it.game === lifecycle }
            .handler { event ->
                handleDeathCosmetics(event)
            }
            .bindWith(lifecycle)

        Events
            .subscribe(MiniGameDestroyBedEvent::class.java)
            .filter { it.game === lifecycle }
            .handler { event ->
                handleBedBreakCosmetics(event)
            }
            .bindWith(lifecycle)

        Events
            .subscribe(GameCompleteEvent::class.java)
            .filter { it.game.identifier == game.identifier }
            .handler { event ->
                handleVictoryCosmetics(event)
            }
            .bindWith(lifecycle)

        Events
            .subscribe(EntityDamageEvent::class.java)
            .filter { event -> event.entity in victoryDanceEntities }
            .handler { event -> event.isCancelled = true }
            .bindWith(lifecycle)

        Events
            .subscribe(EntityTargetEvent::class.java)
            .filter { event -> event.entity in victoryDanceEntities }
            .handler { event -> event.isCancelled = true }
            .bindWith(lifecycle)

        Events
            .subscribe(EntityDamageEvent::class.java)
            .filter { it.entity is Player && it.cause == EntityDamageEvent.DamageCause.FALL }
            .filter { event ->
                val player = event.entity as Player
                GameService.byPlayer(player)?.identifier == game.identifier
            }
            .filter { event ->
                val player = event.entity as Player

                game.report?.winners?.contains(player.uniqueId) == true &&
                    !game.state(GameState.Playing)
            }
            .handler { event -> event.isCancelled = true }
            .bindWith(lifecycle)

        lifecycle.bind(Terminable {
            victoryDanceTasks.forEach { task ->
                try { task.close() } catch (_: Exception) {}
            }
            victoryDanceTasks.clear()

            victoryDanceEntities.forEach { entity ->
                try
                {
                    if (entity.passenger != null) entity.eject()
                    if (entity.vehicle != null) entity.leaveVehicle()
                    if (!entity.isDead) entity.remove()
                }
                catch (_: Exception) {}
            }
            victoryDanceEntities.clear()
            dollyEntities.clear()
        })
    }

    private fun handleDeathCosmetics(event: MiniGamePlayerDeathEvent)
    {
        if (!game.state(GameState.Playing)) return

        val deathLocation = event.player.location
        val isFinalKill = !lifecycle.bedTracker.isBedAlive(
            game.getNullableTeam(event.player)?.teamIdentifier ?: return
        )

        val dyingProfile = BedWarsProfileService.find(event.player)
        if (dyingProfile != null && dyingProfile.selectedDeathCry != "none")
        {
            playDeathCry(dyingProfile.selectedDeathCry, deathLocation)
        }

        if (isFinalKill)
        {
            val killer = event.killer as? Player ?: return
            if (killer.uniqueId == event.player.uniqueId) return

            val killerProfile = BedWarsProfileService.find(killer) ?: return
            if (killerProfile.selectedFinalKillEffect != "none")
            {
                playFinalKillEffect(killerProfile.selectedFinalKillEffect, deathLocation, killer, event.player)
            }

            if (killerProfile.selectedGlyph != "none")
            {
                playGlyph(killerProfile.selectedGlyph, deathLocation)
            }
        }
    }

    private fun handleBedBreakCosmetics(event: MiniGameDestroyBedEvent)
    {
        if (!game.state(GameState.Playing)) return

        val profile = BedWarsProfileService.find(event.destroyedBy) ?: return
        if (profile.selectedBedBreakEffect == "none") return

        val bedLocations = lifecycle.bedTracker.bedLocations(
            event.teamDestroyed,
            game.arenaWorld ?: return
        )

        val location = bedLocations.firstOrNull() ?: event.destroyedBy.location
        playBedBreakEffect(profile.selectedBedBreakEffect, location)
    }

    private fun handleVictoryCosmetics(event: GameCompleteEvent)
    {
        val report = event.game.report ?: return
        val winners = report.winners

        winners.forEach { uuid ->
            val player = org.bukkit.Bukkit.getPlayer(uuid) ?: return@forEach
            val profile = BedWarsProfileService.find(player) ?: return@forEach

            if (profile.selectedVictoryDance != "none")
            {
                playVictoryDance(profile.selectedVictoryDance, player)
            }
        }
    }

    private fun playFinalKillEffect(effectId: String, location: Location, killer: Player, victim: Player)
    {
        val world = location.world ?: return

        when (effectId)
        {
            "lightning_strike" ->
            {
                world.strikeLightningEffect(location)
            }

            "firework" ->
            {
                val firework = world.spawn(location.clone().add(0.0, 1.0, 0.0), Firework::class.java)
                val meta = firework.fireworkMeta
                meta.addEffect(
                    FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(Color.RED, Color.YELLOW, Color.ORANGE)
                        .withFade(Color.WHITE)
                        .withTrail()
                        .withFlicker()
                        .build()
                )
                meta.power = 1
                firework.fireworkMeta = meta
            }

            "heart_aura" ->
            {

                val center = location.clone()
                for (yOffset in listOf(0.0, 0.1, 0.2, 0.3, 0.4, 0.5))
                {
                    for (i in 0 until 17)
                    {
                        val xOff = (Math.random() - 0.5) * 3.0
                        val zOff = (Math.random() - 0.5) * 3.0
                        world.playEffect(
                            center.clone().add(xOff, 1.0 + yOffset, zOff),
                            Effect.HEART, 0
                        )
                    }
                }
            }

            "tornado" ->
            {

                val center = location.clone()
                world.playSound(center, Sound.EXPLODE, 1.0f, 1.0f)
                var angle = 0
                Schedulers.sync().runRepeating({ task ->
                    if (angle >= 70)
                    {
                        task.close()
                        return@runRepeating
                    }

                    val maxHeight = 7
                    val maxRadius = 3.0
                    val lines = 4
                    val heightIncrement = 0.25

                    for (l in 0 until lines)
                    {
                        var y = 0.0
                        while (y < maxHeight)
                        {
                            val radius = y * (maxRadius / maxHeight)
                            val x = Math.cos(Math.toRadians(360.0 / lines * l + y * 30.0 - angle)) * radius
                            val z = Math.sin(Math.toRadians(360.0 / lines * l + y * 30.0 - angle)) * radius
                            world.playEffect(center.clone().add(x, y, z), Effect.CLOUD, 0)
                            y += heightIncrement
                        }
                    }
                    angle++
                }, 2L, 0L).bindWith(lifecycle)
            }

            "bat_crux" ->
            {

                val bats = mutableListOf<Bat>()
                for (i in 0 until 4)
                {
                    val bat = world.spawn(location, Bat::class.java)
                    bat.velocity = bat.location.direction.multiply(0.7).setY(2.0)
                    bat.isCustomNameVisible = true
                    bat.customName = "${CC.GRAY}${victim.name}'s horcrux #${i + 1}"
                    bats += bat
                }

                Schedulers.sync().runLater({
                    bats.forEach { bat ->
                        if (!bat.isDead)
                        {
                            world.playEffect(bat.location, Effect.LARGE_SMOKE, 0)
                        }
                    }
                }, 5L).bindWith(lifecycle)

                Schedulers.sync().runLater({
                    bats.forEach { bat ->
                        if (!bat.isDead) bat.remove()
                    }
                }, 80L).bindWith(lifecycle)
            }

            "burning_shoes" ->
            {

                val center = location.clone()
                var t = 0.0
                Schedulers.sync().runRepeating({ task ->
                    t += 0.3
                    for (phi in generateSequence(0.0) { it + 1.5 }.takeWhile { it <= 6.0 })
                    {
                        val x = 0.11 * (12.5 - t) * Math.cos(t + phi)
                        val y = 0.23 * t
                        val z = 0.11 * (12.5 - t) * Math.sin(t + phi)
                        world.playEffect(center.clone().add(x, y, z), Effect.MOBSPAWNER_FLAMES, 0)

                        if (t >= 12.5 && phi > Math.PI)
                        {
                            task.close()
                            return@runRepeating
                        }
                    }
                }, 1L, 1L).bindWith(lifecycle)
            }

            "rekt" ->
            {

                val eyeLoc = location.clone().add(0.0, 1.62, 0.0)
                val stand = world.spawn(eyeLoc, ArmorStand::class.java)
                stand.isVisible = false
                stand.setGravity(false)
                stand.customName = "${CC.GOLD}${killer.displayName} ${CC.YELLOW}has #rekt ${CC.GOLD}${victim.displayName}${CC.YELLOW}here"
                stand.isCustomNameVisible = true
                stand.isSmall = true

                Schedulers.sync().runLater({
                    if (!stand.isDead) stand.remove()
                }, 200L).bindWith(lifecycle)
            }

            "squid_missile" ->
            {

                val stand = world.spawn(location, ArmorStand::class.java)
                stand.setGravity(false)
                stand.isVisible = false

                val squid = world.spawn(location, Squid::class.java)
                stand.passenger = squid

                var tickCount = 0
                Schedulers.sync().runRepeating({ task ->
                    tickCount++
                    squid.location.yaw = 180.0f

                    stand.eject()
                    stand.teleport(stand.location.add(0.0, 0.5, 0.0))
                    stand.passenger = squid

                    world.playEffect(stand.location, Effect.MOBSPAWNER_FLAMES, 0)
                    world.playSound(stand.location, Sound.CHICKEN_EGG_POP, 1.0f, 1.0f)

                    if (tickCount == 13)
                    {
                        val fw = world.spawn(stand.location, Firework::class.java)
                        val fm = fw.fireworkMeta
                        fm.addEffect(
                            FireworkEffect.builder()
                                .flicker(true).trail(false)
                                .with(FireworkEffect.Type.BALL)
                                .withColor(Color.BLACK).withFade(Color.BLACK)
                                .build()
                        )
                        fw.fireworkMeta = fm
                    }

                    if (tickCount >= 25)
                    {
                        stand.remove()
                        squid.remove()
                        task.close()
                    }
                }, 4L, 1L).bindWith(lifecycle)
            }
        }
    }

    private fun playBedBreakEffect(effectId: String, location: Location)
    {
        val world = location.world ?: return

        when (effectId)
        {
            "fireworks" ->
            {
                for (i in 0 until 3)
                {
                    Schedulers.sync().runLater({
                        val firework = world.spawn(
                            location.clone().add(
                                (Math.random() - 0.5) * 3.0,
                                1.0 + Math.random(),
                                (Math.random() - 0.5) * 3.0
                            ),
                            Firework::class.java
                        )
                        val meta = firework.fireworkMeta
                        meta.addEffect(
                            FireworkEffect.builder()
                                .with(FireworkEffect.Type.BALL)
                                .withColor(Color.RED, Color.WHITE)
                                .withFlicker()
                                .build()
                        )
                        meta.power = 0
                        firework.fireworkMeta = meta
                    }, i * 10L).bindWith(lifecycle)
                }
            }

            "lightning_strike" ->
            {
                world.strikeLightningEffect(location)
                world.strikeLightningEffect(location.clone().add(1.0, 0.0, 0.0))
            }

            "bed_bugs" ->
            {
                val bugs = mutableListOf<Silverfish>()
                for (i in 0 until 6)
                {
                    val bug = world.spawn(
                        location.clone().add(
                            (Math.random() - 0.5) * 2.0,
                            0.5,
                            (Math.random() - 0.5) * 2.0
                        ),
                        Silverfish::class.java
                    )
                    bug.velocity = Vector(
                        (Math.random() - 0.5) * 0.3,
                        0.2,
                        (Math.random() - 0.5) * 0.3
                    )
                    bugs += bug
                }

                Schedulers.sync().runLater({
                    bugs.forEach { bug ->
                        if (!bug.isDead) bug.remove()
                    }
                }, 60L).bindWith(lifecycle)
            }

            "egg_explosion" ->
            {
                for (i in 0 until 10)
                {
                    val egg = world.spawn(
                        location.clone().add(0.0, 1.5, 0.0),
                        org.bukkit.entity.Egg::class.java
                    )
                    egg.velocity = Vector(
                        (Math.random() - 0.5) * 0.8,
                        0.5 + Math.random() * 0.5,
                        (Math.random() - 0.5) * 0.8
                    )
                }
            }

            "tornado" ->
            {

                val center = location.clone()
                world.playSound(center, Sound.EXPLODE, 1.0f, 1.0f)
                var angle = 0
                Schedulers.sync().runRepeating({ task ->
                    if (angle >= 70)
                    {
                        task.close()
                        return@runRepeating
                    }

                    val maxHeight = 7
                    val maxRadius = 3.0
                    val lines = 4
                    val heightIncrement = 0.25

                    for (l in 0 until lines)
                    {
                        var y = 0.0
                        while (y < maxHeight)
                        {
                            val radius = y * (maxRadius / maxHeight)
                            val x = Math.cos(Math.toRadians(360.0 / lines * l + y * 30.0 - angle)) * radius
                            val z = Math.sin(Math.toRadians(360.0 / lines * l + y * 30.0 - angle)) * radius
                            world.playEffect(center.clone().add(x, y, z), Effect.CLOUD, 0)
                            y += heightIncrement
                        }
                    }
                    angle++
                }, 2L, 0L).bindWith(lifecycle)
            }

            "pig_missile" ->
            {

                val stand = world.spawn(location, ArmorStand::class.java)
                stand.setGravity(false)
                stand.isVisible = false

                val pig = world.spawn(location, Pig::class.java)
                pig.setSaddle(true)
                stand.passenger = pig

                var tickCount = 0
                Schedulers.sync().runRepeating({ task ->
                    tickCount++
                    stand.eject()
                    stand.teleport(stand.location.add(0.0, 0.5, 0.0))
                    stand.passenger = pig

                    world.playEffect(stand.location, Effect.MOBSPAWNER_FLAMES, 0)
                    world.playSound(stand.location, Sound.CHICKEN_EGG_POP, 1.0f, 1.0f)

                    if (tickCount == 13)
                    {
                        val fw = world.spawn(stand.location, Firework::class.java)
                        val fm = fw.fireworkMeta
                        fm.addEffect(
                            FireworkEffect.builder()
                                .flicker(true).trail(false)
                                .with(FireworkEffect.Type.BALL)
                                .withColor(Color.BLACK).withFade(Color.BLACK)
                                .build()
                        )
                        fw.fireworkMeta = fm
                    }

                    if (tickCount >= 25)
                    {
                        stand.remove()
                        pig.remove()
                        task.close()
                    }
                }, 4L, 1L).bindWith(lifecycle)
            }

            "squid_missile" ->
            {

                val stand = world.spawn(location, ArmorStand::class.java)
                stand.setGravity(false)
                stand.isVisible = false

                val squid = world.spawn(location, Squid::class.java)
                stand.passenger = squid

                var tickCount = 0
                Schedulers.sync().runRepeating({ task ->
                    tickCount++
                    squid.location.yaw = 180.0f

                    stand.eject()
                    stand.teleport(stand.location.add(0.0, 0.5, 0.0))
                    stand.passenger = squid

                    world.playEffect(stand.location, Effect.MOBSPAWNER_FLAMES, 0)
                    world.playSound(stand.location, Sound.CHICKEN_EGG_POP, 1.0f, 1.0f)

                    if (tickCount == 13)
                    {
                        val fw = world.spawn(stand.location, Firework::class.java)
                        val fm = fw.fireworkMeta
                        fm.addEffect(
                            FireworkEffect.builder()
                                .flicker(true).trail(false)
                                .with(FireworkEffect.Type.BALL)
                                .withColor(Color.BLACK).withFade(Color.BLACK)
                                .build()
                        )
                        fw.fireworkMeta = fm
                    }

                    if (tickCount >= 25)
                    {
                        stand.remove()
                        squid.remove()
                        task.close()
                    }
                }, 4L, 1L).bindWith(lifecycle)
            }

            "ghost" ->
            {

                val entities = mutableListOf<org.bukkit.entity.Entity>()
                for (i in 0 until 5)
                {
                    val spawnLoc = location.clone().add(
                        (Math.random() - 0.5) * 3.0,
                        1.0 + Math.random(),
                        (Math.random() - 0.5) * 3.0
                    )

                    val bat = world.spawn(spawnLoc, Bat::class.java)
                    bat.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, 19999980, 1, false, false))
                    entities += bat

                    val stand = world.spawn(spawnLoc, ArmorStand::class.java)
                    stand.isVisible = false
                    stand.setGravity(false)
                    stand.isSmall = true
                    stand.helmet = ItemStack(Material.SKULL_ITEM, 1, 3.toShort())
                    bat.passenger = stand
                    entities += stand
                }

                world.playSound(location, Sound.WITHER_SPAWN, 0.5f, 1.5f)

                Schedulers.sync().runLater({
                    entities.forEach { entity ->
                        if (!entity.isDead)
                        {
                            world.playEffect(entity.location, Effect.LARGE_SMOKE, 0)
                            entity.remove()
                        }
                    }
                    world.playSound(location, Sound.EXPLODE, 0.8f, 1.0f)
                }, 160L).bindWith(lifecycle)
            }

            "hologram" ->
            {
                val stand = world.spawn(
                    location.clone().add(0.0, 2.0, 0.0),
                    ArmorStand::class.java
                )
                stand.isVisible = false
                stand.setGravity(false)
                stand.isSmall = true
                stand.customName = "${CC.RED}\u2620 ${CC.WHITE}Bed Destroyed! ${CC.RED}\u2620"
                stand.isCustomNameVisible = true

                Schedulers.sync().runLater({
                    if (!stand.isDead) stand.remove()
                }, 100L).bindWith(lifecycle)
            }

            "thief" ->
            {
                val enderman = world.spawn(
                    location.clone().add(1.0, 0.0, 0.0),
                    Enderman::class.java
                )
                enderman.carriedMaterial = MaterialData(Material.BED_BLOCK)

                var ticks = 0
                Schedulers.sync().runRepeating({ task ->
                    if (ticks >= 70 || enderman.isDead)
                    {
                        if (!enderman.isDead) enderman.remove()
                        task.close()
                        return@runRepeating
                    }

                    val angle = ticks * 0.15
                    val radius = 2.0 + ticks * 0.05
                    val x = Math.cos(angle) * radius
                    val z = Math.sin(angle) * radius
                    val target = location.clone().add(x, 0.0, z)
                    target.yaw = (Math.toDegrees(Math.atan2(
                        -(target.x - location.x),
                        target.z - location.z
                    ))).toFloat()
                    enderman.teleport(target)
                    world.playEffect(enderman.location, Effect.PORTAL, 0)

                    ticks += 2
                }, 0L, 2L).bindWith(lifecycle)
            }

            "random" ->
            {
                val availableEffects = listOf(
                    "fireworks", "lightning_strike", "bed_bugs", "egg_explosion",
                    "tornado", "pig_missile", "squid_missile", "ghost", "hologram", "thief"
                )
                val randomEffect = availableEffects.random()
                playBedBreakEffect(randomEffect, location)
            }
        }
    }

    private fun playDeathCry(cryId: String, location: Location)
    {
        val world = location.world ?: return

        when (cryId)
        {
            "bazinga" -> world.playSound(location, Sound.BLAZE_DEATH, 1.5f, 1.0f)
            "enderman" -> world.playSound(location, Sound.ENDERMAN_DEATH, 1.5f, 0.8f)
            "pig" -> world.playSound(location, Sound.PIG_DEATH, 1.5f, 1.0f)
            "bat" -> world.playSound(location, Sound.BAT_DEATH, 1.5f, 1.0f)
            "skeleton" -> world.playSound(location, Sound.SKELETON_DEATH, 1.5f, 1.0f)
            "wolf" -> world.playSound(location, Sound.WOLF_HOWL, 1.5f, 0.8f)
            "burp" -> world.playSound(location, Sound.BURP, 1.5f, 1.0f)
            "firework" -> world.playSound(location, Sound.FIREWORK_LAUNCH, 1.5f, 1.0f)
            "splash" -> world.playSound(location, Sound.SPLASH, 1.5f, 1.0f)
            "sad-puppy" -> world.playSound(location, Sound.WOLF_DEATH, 1.5f, 0.8f)
            "sad-moo" -> world.playSound(location, Sound.COW_IDLE, 1.5f, 0.7f)
            "plop" -> world.playSound(location, Sound.CHICKEN_EGG_POP, 1.5f, 1.0f)
            "ding" -> world.playSound(location, Sound.SUCCESSFUL_HIT, 1.5f, 1.0f)
            "fireball" -> world.playSound(location, Sound.GHAST_FIREBALL, 1.5f, 1.0f)
            "explode" -> world.playSound(location, Sound.EXPLODE, 1.5f, 1.0f)
            "zombie" -> world.playSound(location, Sound.ENDERMAN_SCREAM, 1.5f, 0.6f)
            "breath" -> world.playSound(location, Sound.BLAZE_BREATH, 1.5f, 0.8f)
            "click" -> world.playSound(location, Sound.CLICK, 1.5f, 1.0f)
            "creepy" -> world.playSound(location, Sound.AMBIENCE_CAVE, 1.5f, 1.0f)
        }
    }

    

    

    private val toyStickCooldowns = mutableMapOf<UUID, Long>()
    private val dollyEntities = mutableListOf<Sheep>()

    private fun playVictoryDance(danceId: String, player: Player)
    {
        val world = player.world

        when (danceId)
        {
            "fireworks" ->
            {

                
                victoryDanceTasks += Schedulers.sync().runRepeating({ task ->
                    if (!player.isOnline)
                    {
                        task.close()
                        return@runRepeating
                    }

                    val loc = player.eyeLocation
                    spawnAndDetonateFirework(world, loc, Color.RED, Color.BLUE)

                    victoryDanceTasks += Schedulers.sync().runLater({
                        if (player.isOnline)
                        {
                            spawnAndDetonateFirework(world, loc, Color.ORANGE, Color.YELLOW)
                        }
                    }, 300L)
                }, 0L, 60L)
            }

            "anvil_rain" ->
            {

                victoryDanceTasks += Schedulers.sync().runRepeating({ task ->
                    if (!player.isOnline)
                    {
                        task.close()
                        return@runRepeating
                    }

                    val loc = getRandomLocation(player.location, 20)
                    val anvil = world.spawnFallingBlock(loc, Material.ANVIL, 0.toByte())
                    anvil.dropItem = false
                    victoryDanceEntities += anvil
                }, 0L, 1L)
            }

            "cold_snap" ->
            {

                
                var radius = 2
                var pitch = 0.6f

                val mainTask = Schedulers.sync().runRepeating({ task ->
                    if (!player.isOnline)
                    {
                        task.close()
                        return@runRepeating
                    }

                    val loc = player.location.subtract(0.0, 1.0, 0.0)
                    val blocks = getSphereBlocks(loc, radius) { block ->
                        block.type != Material.AIR &&
                            block.type != Material.PACKED_ICE &&
                            block.type != Material.ICE
                    }

                    world.players.forEach { p ->
                        blocks.forEach { block ->
                            @Suppress("DEPRECATION")
                            p.sendBlockChange(block.location, Material.PACKED_ICE, 0.toByte())
                        }
                    }

                    world.playSound(player.location, Sound.ORB_PICKUP, 1.0f, pitch)
                    pitch += 0.15f
                    radius++
                }, 0L, 10L)
                victoryDanceTasks += mainTask

                victoryDanceTasks += Schedulers.sync().runLater({
                    mainTask.close()
                }, 165L)
            }

            "dragon_rider" ->
            {

                

                val dragon = world.spawn(player.location, EnderDragon::class.java)
                dragon.setPassenger(player)
                dragon.noDamageTicks = Integer.MAX_VALUE
                victoryDanceEntities += dragon

                val stand = world.spawn(player.location, ArmorStand::class.java)
                stand.isVisible = false
                stand.setGravity(false)
                victoryDanceEntities += stand

                val startingWorld = world

                victoryDanceTasks += Schedulers.sync().runRepeating({ task ->
                    if (!player.isOnline || dragon.isDead || world.uid != startingWorld.uid)
                    {
                        if (!stand.isDead) stand.remove()
                        if (!dragon.isDead) dragon.remove()
                        task.close()
                        return@runRepeating
                    }

                    try
                    {
                        (dragon as? Creature)?.target = stand
                    }
                    catch (_: Exception) {}

                    val original = player.eyeLocation
                    val direction = player.eyeLocation.direction.normalize().multiply(2.0)
                    val newLocation = original.add(direction)
                    dragon.velocity = dragon.velocity.add(direction)
                    stand.teleport(newLocation)

                    for (block in getBlocksInRadius(dragon.location, 10))
                    {
                        block.type = Material.AIR
                    }

                    if (dragon.passenger !== player)
                    {
                        dragon.setPassenger(player)
                    }

                    val fireball = world.spawn(original, Fireball::class.java)
                    fireball.direction = direction
                    victoryDanceEntities += fireball
                }, 0L, 10L)
            }

            "guardians" ->
            {

                
                player.noDamageTicks = Integer.MAX_VALUE
                val guardians = mutableListOf<Guardian>()

                for (i in 0 until 3)
                {
                    val guardianLoc = player.location.clone().add(0.0, 1.0, 0.0)
                    guardianLoc.yaw = (i * 120).toFloat()
                    val guardian = world.spawn(guardianLoc, Guardian::class.java)
                    guardian.noDamageTicks = Integer.MAX_VALUE
                    guardians += guardian
                    victoryDanceEntities += guardian
                }

                victoryDanceTasks += Schedulers.sync().runRepeating({ task ->
                    if (!player.isOnline)
                    {
                        guardians.forEach { if (!it.isDead) it.remove() }
                        task.close()
                        return@runRepeating
                    }

                    guardians.forEach { guardian ->
                        if (!guardian.isDead)
                        {
                            guardian.noDamageTicks = Integer.MAX_VALUE
                            guardian.target = player
                        }
                    }
                }, 0L, 20L)
            }

            "haunted" ->
            {

                
                victoryDanceTasks += Schedulers.sync().runRepeating({ task ->
                    if (!player.isOnline)
                    {
                        task.close()
                        return@runRepeating
                    }

                    val loc = getRandomLocation(player.location, 10)
                    val bat = world.spawn(loc, Bat::class.java)
                    bat.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1))
                    bat.noDamageTicks = Integer.MAX_VALUE
                    victoryDanceEntities += bat

                    val stand = world.spawn(loc, ArmorStand::class.java)
                    stand.isVisible = false
                    victoryDanceEntities += stand

                    bat.passenger = stand

                    val leather = ItemStack(Material.LEATHER_CHESTPLATE)
                    val lm = leather.itemMeta as LeatherArmorMeta
                    lm.color = Color.GRAY
                    leather.itemMeta = lm
                    stand.chestplate = leather
                    stand.helmet = createSkullFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjhkMjE4MzY0MDIxOGFiMzMwYWM1NmQyYWFiN2UyOWE5NzkwYTU0NWY2OTE2MTllMzg1NzhlYTRhNjlhZTBiNiJ9fX0=")
                }, 0L, 8L)
            }

            "raining_pigs" ->
            {

                victoryDanceTasks += Schedulers.sync().runRepeating({ task ->
                    if (!player.isOnline)
                    {
                        task.close()
                        return@runRepeating
                    }

                    val loc = getRandomLocation(player.location, 20)
                    val pig = world.spawn(loc, Pig::class.java)
                    pig.setSaddle(true)
                    pig.noDamageTicks = Integer.MAX_VALUE
                    victoryDanceEntities += pig
                }, 0L, 1L)
            }

            "wither_rider" ->
            {

                

                val wither = world.spawn(player.location, Wither::class.java)
                wither.setPassenger(player)
                wither.noDamageTicks = Integer.MAX_VALUE
                wither.customName = "${CC.GREEN}${player.name}'s Wither!"
                victoryDanceEntities += wither

                victoryDanceTasks += Schedulers.sync().runRepeating({ task ->
                    if (!player.isOnline || wither.isDead)
                    {
                        if (!wither.isDead) wither.remove()
                        task.close()
                        return@runRepeating
                    }

                    if (wither.passenger !== player)
                    {
                        wither.setPassenger(player)
                    }

                    val direction = player.eyeLocation.direction.normalize().multiply(0.5)
                    wither.velocity = direction
                    wither.target = null
                }, 0L, 1L)

                Events
                    .subscribe(PlayerInteractEvent::class.java)
                    .filter { it.player.uniqueId == player.uniqueId }
                    .filter { it.player.vehicle is Wither }
                    .handler { event ->
                        event.isCancelled = true

                        val spawnLoc = player.eyeLocation.add(player.eyeLocation.direction.normalize().multiply(2.0))
                        val skull = world.spawn(spawnLoc, WitherSkull::class.java)
                        skull.shooter = player
                        skull.velocity = player.eyeLocation.direction.normalize().multiply(3.0)
                        victoryDanceEntities += skull
                    }
            }

            "floating_lantern" ->
            {

                
                victoryDanceTasks += Schedulers.sync().runRepeating({ task ->
                    if (!player.isOnline)
                    {
                        task.close()
                        return@runRepeating
                    }

                    val loc = getRandomLocation(player.location, 10)
                    val bat = world.spawn(loc, Bat::class.java)
                    bat.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1))
                    bat.noDamageTicks = Integer.MAX_VALUE
                    victoryDanceEntities += bat

                    val stand = world.spawn(loc, ArmorStand::class.java)
                    stand.isVisible = false
                    stand.setGravity(false)
                    stand.helmet = createSkullFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTRjNzM2ODY0YTY0OWY2NjVmMDRmMjhiYjE0YTNjNGVhMGYyNDVlODQ2MDE3YWNmZTM2NmU3ZDEzNWI0ZmNhOCJ9fX0=")
                    victoryDanceEntities += stand

                    bat.passenger = stand
                }, 0L, 8L)
            }

            "night_shift" ->
            {

                victoryDanceTasks += Schedulers.sync().runRepeating({ task ->
                    if (!player.isOnline)
                    {
                        task.close()
                        return@runRepeating
                    }

                    var time = world.time + 1000L
                    if (time > 24000L) time = 0L
                    world.time = time
                }, 0L, 8L)
            }

            "rainbow_dolly" ->
            {

                
                val sheep = world.spawn(player.location, Sheep::class.java)
                sheep.color = DyeColor.RED
                sheep.customName = "Dolly"
                sheep.noDamageTicks = Integer.MAX_VALUE
                dollyEntities += sheep
                victoryDanceEntities += sheep

                Events
                    .subscribe(PlayerInteractAtEntityEvent::class.java)
                    .filter { it.rightClicked.type == org.bukkit.entity.EntityType.SHEEP }
                    .filter {
                        val clicked = it.rightClicked as? Sheep
                        clicked?.customName == "Dolly"
                    }
                    .handler { event ->
                        val clickedSheep = event.rightClicked as Sheep
                        val colors = listOf(
                            DyeColor.RED, DyeColor.BLUE, DyeColor.BROWN, DyeColor.GREEN,
                            DyeColor.PINK, DyeColor.YELLOW, DyeColor.GRAY, DyeColor.LIME,
                            DyeColor.CYAN, DyeColor.MAGENTA
                        )

                        val newSheep = world.spawn(clickedSheep.location, Sheep::class.java)
                        world.createExplosion(newSheep.location, 1.0f)
                        newSheep.color = colors.random()
                        newSheep.velocity = clickedSheep.location.direction.multiply(-0.5).setY(1.0)
                        newSheep.customName = "Dolly"
                        dollyEntities += newSheep
                        victoryDanceEntities += newSheep
                    }
            }

            "toy_stick" ->
            {

                
                val stick = ItemStack(Material.STICK)
                val meta = stick.itemMeta
                meta.displayName = "${CC.GREEN}Toy Stick"
                meta.lore = listOf(
                    "${CC.GRAY}Right Click on a block",
                    "${CC.GRAY}to fly!"
                )
                stick.itemMeta = meta
                player.inventory.addItem(stick)

                Events
                    .subscribe(PlayerInteractEvent::class.java)
                    .filter { it.player.uniqueId == player.uniqueId }
                    .filter { it.player.itemInHand?.type == Material.STICK }
                    .filter {
                        val im = it.player.itemInHand?.itemMeta
                        im?.displayName?.contains("Toy Stick") == true
                    }
                    .handler { event ->
                        val now = System.currentTimeMillis()
                        val lastUse = toyStickCooldowns[player.uniqueId]
                        if (lastUse != null && (now - lastUse) < TimeUnit.SECONDS.toMillis(1))
                        {
                            return@handler
                        }

                        event.player.velocity = event.player.location.direction.multiply(-6.0).setY(6.0)

                        for (loc in generateSphere(event.player.location, 6, false))
                        {
                            val block = loc.block
                            bounceBlock(block, world)
                            block.breakNaturally()
                        }

                        toyStickCooldowns[player.uniqueId] = now
                    }
            }

            "twerk_apocalypse" ->
            {

                

                val twerkStands = mutableListOf<ArmorStand>()

                victoryDanceTasks += Schedulers.sync().runRepeating({ spawnTask ->
                    if (!player.isOnline || twerkStands.size >= 15)
                    {
                        spawnTask.close()
                        return@runRepeating
                    }

                    val loc = getRandomLocation(player.location, 10)
                    val below = loc.clone().subtract(0.0, 1.0, 0.0)
                    if (loc.block.type == Material.AIR && below.block.type != Material.AIR)
                    {
                        val spawnLoc = loc.clone()
                        spawnLoc.yaw = (Math.random() * 360.0).toFloat()

                        val stand = world.spawn(spawnLoc, ArmorStand::class.java)
                        stand.setGravity(false)
                        stand.setArms(true)
                        stand.helmet = createSkullFromUsername(player.name)
                        stand.chestplate = ItemStack(Material.LEATHER_CHESTPLATE)
                        stand.leggings = ItemStack(Material.LEATHER_LEGGINGS)
                        stand.boots = ItemStack(Material.LEATHER_BOOTS)
                        stand.customName = player.displayName
                        stand.isCustomNameVisible = true
                        twerkStands += stand
                        victoryDanceEntities += stand

                        victoryDanceTasks += Schedulers.sync().runRepeating({ toggleTask ->
                            if (stand.isDead || !player.isOnline)
                            {
                                toggleTask.close()
                                return@runRepeating
                            }
                            stand.isSmall = !stand.isSmall
                        }, 0L, 20L)
                    }
                }, 0L, 1L)

                victoryDanceTasks += Schedulers.sync().runLater({
                    twerkStands.forEach { if (!it.isDead) it.remove() }
                }, 190L)
            }

            "yee_haw" ->
            {

                
                val horse = world.spawn(player.location, Horse::class.java)
                horse.inventory.saddle = ItemStack(Material.SADDLE)
                @Suppress("DEPRECATION")
                horse.jumpStrength = 10.0
                horse.passenger = player
                horse.setAdult()
                horse.noDamageTicks = Integer.MAX_VALUE
                horse.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 190000000, 3))
                horse.isTamed = true
                horse.owner = player
                victoryDanceEntities += horse

                victoryDanceTasks += Schedulers.sync().runRepeating({ task ->
                    if (!player.isOnline || horse.isDead)
                    {
                        if (!horse.isDead) horse.remove()
                        task.close()
                        return@runRepeating
                    }

                    if (horse.passenger == null)
                    {
                        horse.remove()
                        task.close()
                    }
                }, 0L, 400L)
            }
        }
    }

    private fun spawnAndDetonateFirework(world: org.bukkit.World, loc: Location, color1: Color, color2: Color)
    {
        val fw = world.spawn(loc.clone().subtract(0.0, 1.0, 0.0), Firework::class.java)
        val fwm = fw.fireworkMeta
        fwm.addEffect(FireworkEffect.builder().withColor(color1).trail(false).flicker(true).build())
        fwm.addEffect(FireworkEffect.builder().withColor(color2).trail(false).flicker(true).build())
        fw.fireworkMeta = fwm
        fw.detonate()

        val fw2 = world.spawn(loc, Firework::class.java)
        fw2.fireworkMeta = fwm
    }

    private fun getRandomLocation(origin: Location, radius: Int): Location
    {
        val x = origin.x + (Math.random() * radius * 2) - radius
        val y = origin.y + (Math.random() * radius * 2) - radius
        val z = origin.z + (Math.random() * radius * 2) - radius
        return Location(origin.world, x, y, z)
    }

    private fun generateSphere(center: Location, radius: Int, hollow: Boolean): List<Location>
    {
        val locations = mutableListOf<Location>()
        val bx = center.blockX
        val by = center.blockY
        val bz = center.blockZ
        for (x in (bx - radius)..(bx + radius))
        {
            for (y in (by - radius)..(by + radius))
            {
                for (z in (bz - radius)..(bz + radius))
                {
                    val distance = ((bx - x) * (bx - x) + (bz - z) * (bz - z) + (by - y) * (by - y)).toDouble()
                    if (distance < radius * radius && (!hollow || distance >= (radius - 1) * (radius - 1)))
                    {
                        locations += Location(center.world, x.toDouble(), y.toDouble(), z.toDouble())
                    }
                }
            }
        }
        return locations
    }

    private fun getSphereBlocks(
        center: Location,
        radius: Int,
        filter: (org.bukkit.block.Block) -> Boolean
    ): List<org.bukkit.block.Block>
    {
        val blocks = mutableListOf<org.bukkit.block.Block>()
        val bx = center.blockX
        val by = center.blockY
        val bz = center.blockZ
        for (x in (bx - radius)..(bx + radius))
        {
            for (y in (by - radius)..(by + radius))
            {
                for (z in (bz - radius)..(bz + radius))
                {
                    val distance = ((bx - x) * (bx - x) + (by - y) * (by - y) + (bz - z) * (bz - z)).toDouble()
                    if (distance < radius * radius)
                    {
                        val block = center.world.getBlockAt(x, y, z)
                        if (block.type != Material.BARRIER && filter(block))
                        {
                            blocks += block
                        }
                    }
                }
            }
        }
        return blocks
    }

    private fun getBlocksInRadius(location: Location, radius: Int): List<org.bukkit.block.Block>
    {
        val blocks = mutableListOf<org.bukkit.block.Block>()
        val bx = location.blockX
        val by = location.blockY
        val bz = location.blockZ
        for (x in (bx - radius)..(bx + radius))
        {
            for (y in (by - radius)..(by + radius))
            {
                for (z in (bz - radius)..(bz + radius))
                {
                    val distance = ((bx - x) * (bx - x) + (by - y) * (by - y) + (bz - z) * (bz - z)).toDouble()
                    if (distance < radius * radius)
                    {
                        val block = location.world.getBlockAt(x, y, z)
                        if (block.type != Material.BARRIER)
                        {
                            blocks += block
                        }
                    }
                }
            }
        }
        return blocks
    }

    @Suppress("DEPRECATION")
    private fun bounceBlock(block: org.bukkit.block.Block, world: org.bukkit.World)
    {
        if (block.type == Material.AIR) return

        val entity: org.bukkit.entity.Entity = if (block.type == Material.TNT)
        {
            world.spawn(block.location.add(0.0, 1.0, 0.0), org.bukkit.entity.TNTPrimed::class.java)
        }
        else
        {
            val fb = world.spawnFallingBlock(block.location.add(0.0, 1.0, 0.0), block.type, block.data)
            fb.dropItem = false
            fb
        }
        block.type = Material.AIR
        val x = -1.0f + (Math.random() * 3.0).toFloat()
        val y = 0.5f
        val z = -0.3f + (Math.random() * 1.6).toFloat()
        entity.velocity = Vector(x.toDouble(), y.toDouble(), z.toDouble())
    }

    private fun createSkullFromBase64(base64: String): ItemStack
    {
        val skull = ItemStack(Material.SKULL_ITEM, 1, 3.toShort())
        val meta = skull.itemMeta as SkullMeta
        val profile = GameProfile(UUID.randomUUID(), null)
        profile.properties.put("textures", Property("textures", base64))
        try
        {
            val profileField = meta.javaClass.getDeclaredField("profile")
            profileField.isAccessible = true
            profileField.set(meta, profile)
        }
        catch (_: Exception) {}
        skull.itemMeta = meta
        return skull
    }

    private fun createSkullFromUsername(username: String): ItemStack
    {
        val skull = ItemStack(Material.SKULL_ITEM, 1, 3.toShort())
        val meta = skull.itemMeta as SkullMeta
        meta.owner = username
        skull.itemMeta = meta
        return skull
    }

    private fun configureProjectileTrails()
    {
        Schedulers.sync().runRepeating({ _ ->
            if (!game.state(GameState.Playing)) return@runRepeating

            val world = game.arenaWorld ?: return@runRepeating

            world.entities
                .filter { entity ->
                    entity is org.bukkit.entity.Projectile &&
                        entity.shooter is Player
                }
                .forEach { entity ->
                    val projectile = entity as org.bukkit.entity.Projectile
                    val shooter = projectile.shooter as? Player ?: return@forEach

                    if (GameService.byPlayer(shooter)?.identifier != game.identifier)
                    {
                        return@forEach
                    }

                    val profile = BedWarsProfileService.find(shooter) ?: return@forEach
                    if (profile.selectedProjectileTrail == "none") return@forEach

                    val loc = entity.location

                    when (profile.selectedProjectileTrail)
                    {
                        "slime" -> world.playEffect(loc, Effect.SLIME, 0)
                        "ender" -> world.playEffect(loc, Effect.PORTAL, 0)
                        "lava" -> world.playEffect(loc, Effect.LAVA_POP, 0)
                        "potion" -> world.playEffect(loc, Effect.POTION_SWIRL, 0)
                        "water" -> world.playEffect(loc, Effect.WATERDRIP, 0)
                        "black-smoke" -> world.playEffect(loc, Effect.LARGE_SMOKE, 0)
                        "white-smoke" -> world.playEffect(loc, Effect.SMOKE, 0)
                        "note" -> world.playEffect(loc, Effect.NOTE, 0)
                        "blue-dust" -> world.playEffect(loc, Effect.COLOURED_DUST, 0)
                        "red-dust" -> world.playEffect(loc, Effect.COLOURED_DUST, 0)
                        "purple-dust" -> world.playEffect(loc, Effect.COLOURED_DUST, 0)
                        "fire" -> world.playEffect(loc, Effect.MOBSPAWNER_FLAMES, 0)
                        "magic" -> world.playEffect(loc, Effect.MAGIC_CRIT, 0)
                        "firework" -> world.playEffect(loc, Effect.FIREWORKS_SPARK, 0)
                        "angry-villager" -> world.playEffect(loc, Effect.VILLAGER_THUNDERCLOUD, 0)
                        "happy-villager" -> world.playEffect(loc, Effect.HAPPY_VILLAGER, 0)
                        "portal" -> world.playEffect(loc, Effect.PORTAL, 0)
                    }
                }
        }, 0L, 1L).bindWith(lifecycle)
    }

    fun buildCustomKillMessage(
        killer: Player,
        victim: Player,
        killerTeam: gg.tropic.practice.games.team.TeamIdentifier,
        victimTeam: gg.tropic.practice.games.team.TeamIdentifier
    ): String?
    {
        val profile = BedWarsProfileService.find(killer) ?: return null
        if (profile.selectedKillMessage == "none") return null

        val killerColor = TeamVisuals.toChatColor(killerTeam).toString()
        val victimColor = TeamVisuals.toChatColor(victimTeam).toString()

        return KillMessageType.formatKillMessage(
            killMessageId = profile.selectedKillMessage,
            killerName = killer.name,
            victimName = victim.name,
            killerColor = killerColor,
            victimColor = victimColor
        )
    }

    private fun configureWoodSkins()
    {
        Events
            .subscribe(BlockPlaceEvent::class.java)
            .filter { game.state(GameState.Playing) }
            .filter { GameService.byPlayer(it.player)?.identifier == game.identifier }
            .filter { it.block.type == Material.WOOD }
            .handler { event ->
                val profile = BedWarsProfileService.find(event.player) ?: return@handler
                if (profile.selectedWoodSkin == "none") return@handler

                val blockData = WoodSkinType.getBlockData(profile.selectedWoodSkin) ?: return@handler
                event.block.type = blockData.first
                event.block.data = blockData.second
            }
            .bindWith(lifecycle)
    }

    private fun configureGlyphs()
    {

        
    }

    private fun playGlyph(glyphId: String, location: Location)
    {
        val world = location.world ?: return
        val center = location.clone().add(0.0, 3.0, 0.0)

        when (glyphId)
        {
            "iron" ->
            {

                val points = mutableListOf<Pair<Double, Double>>()
                for (x in -2..2) for (y in -1..1)
                {
                    if (x == -2 || x == 2 || y == -1 || y == 1)
                        points += Pair(x * 0.15, y * 0.2)
                }

                for (x in -1..1) for (y in 0..0)
                    points += Pair(x * 0.15, y * 0.2)

                var index = 0
                Schedulers.sync().runRepeating({ task ->
                    if (index >= points.size)
                    {
                        task.close()
                        return@runRepeating
                    }
                    for (i in 0 until minOf(4, points.size - index))
                    {
                        val (px, py) = points[index + i]
                        world.playEffect(center.clone().add(px, py, 0.0), Effect.COLOURED_DUST, 0)
                    }
                    index += 4
                }, 0L, 2L).bindWith(lifecycle)
            }

            "gold" ->
            {

                val points = mutableListOf<Pair<Double, Double>>()

                for (i in -3..3) points += Pair(i * 0.12, -0.2)

                for (i in -2..2) points += Pair(i * 0.12, 0.2)

                points += Pair(-0.36, -0.2); points += Pair(-0.30, 0.0); points += Pair(-0.24, 0.2)

                points += Pair(0.36, -0.2); points += Pair(0.30, 0.0); points += Pair(0.24, 0.2)

                var index = 0
                Schedulers.sync().runRepeating({ task ->
                    if (index >= points.size)
                    {
                        task.close()
                        return@runRepeating
                    }
                    for (i in 0 until minOf(4, points.size - index))
                    {
                        val (px, py) = points[index + i]
                        world.playEffect(center.clone().add(px, py, 0.0), Effect.COLOURED_DUST, 0)
                    }
                    index += 4
                }, 0L, 2L).bindWith(lifecycle)
            }

            "thumbsup" ->
            {

                val points = mutableListOf<Pair<Double, Double>>()

                for (i in 0..5) points += Pair(0.0, i * 0.15)

                for (x in -2..2) for (y in -2..-1)
                    points += Pair(x * 0.12, y * 0.15)

                var index = 0
                Schedulers.sync().runRepeating({ task ->
                    if (index >= points.size)
                    {
                        task.close()
                        return@runRepeating
                    }
                    for (i in 0 until minOf(3, points.size - index))
                    {
                        val (px, py) = points[index + i]
                        world.playEffect(center.clone().add(px, py, 0.0), Effect.HAPPY_VILLAGER, 0)
                    }
                    index += 3
                }, 0L, 2L).bindWith(lifecycle)
            }

            "thumbsdown" ->
            {

                val points = mutableListOf<Pair<Double, Double>>()

                for (i in 0..5) points += Pair(0.0, -i * 0.15)

                for (x in -2..2) for (y in 1..2)
                    points += Pair(x * 0.12, y * 0.15)

                var index = 0
                Schedulers.sync().runRepeating({ task ->
                    if (index >= points.size)
                    {
                        task.close()
                        return@runRepeating
                    }
                    for (i in 0 until minOf(3, points.size - index))
                    {
                        val (px, py) = points[index + i]
                        world.playEffect(center.clone().add(px, py, 0.0), Effect.VILLAGER_THUNDERCLOUD, 0)
                    }
                    index += 3
                }, 0L, 2L).bindWith(lifecycle)
            }

            "sword" ->
            {

                val points = mutableListOf<Pair<Double, Double>>()

                for (i in 0..8) points += Pair(0.0, i * 0.15)

                points += Pair(-0.3, 0.6); points += Pair(-0.15, 0.6)
                points += Pair(0.15, 0.6); points += Pair(0.3, 0.6)

                points += Pair(0.0, -0.15); points += Pair(0.0, -0.3)

                var index = 0
                Schedulers.sync().runRepeating({ task ->
                    if (index >= points.size)
                    {
                        task.close()
                        return@runRepeating
                    }
                    for (i in 0 until minOf(3, points.size - index))
                    {
                        val (px, py) = points[index + i]
                        world.playEffect(center.clone().add(px, py, 0.0), Effect.INSTANT_SPELL, 0)
                    }
                    index += 3
                }, 0L, 2L).bindWith(lifecycle)
            }

            "storm" ->
            {

                val points = mutableListOf<Pair<Double, Double>>()
                points += Pair(-0.1, 0.8); points += Pair(0.0, 0.8); points += Pair(0.1, 0.8)
                points += Pair(-0.05, 0.6); points += Pair(0.05, 0.6)
                points += Pair(0.0, 0.4)
                points += Pair(-0.2, 0.3); points += Pair(-0.1, 0.3); points += Pair(0.0, 0.3); points += Pair(0.1, 0.3); points += Pair(0.2, 0.3)
                points += Pair(0.05, 0.1); points += Pair(-0.05, 0.1)
                points += Pair(0.0, -0.1)
                points += Pair(-0.05, -0.3); points += Pair(0.05, -0.3)

                var index = 0
                Schedulers.sync().runRepeating({ task ->
                    if (index >= points.size)
                    {
                        task.close()
                        return@runRepeating
                    }
                    for (i in 0 until minOf(4, points.size - index))
                    {
                        val (px, py) = points[index + i]
                        world.playEffect(center.clone().add(px, py, 0.0), Effect.FIREWORKS_SPARK, 0)
                    }
                    index += 4
                }, 0L, 2L).bindWith(lifecycle)
            }

            "burn" ->
            {

                val points = mutableListOf<Pair<Double, Double>>()

                points += Pair(-0.2, -0.3); points += Pair(-0.1, -0.3); points += Pair(0.0, -0.3); points += Pair(0.1, -0.3); points += Pair(0.2, -0.3)

                points += Pair(-0.25, -0.1); points += Pair(-0.15, -0.1); points += Pair(0.0, -0.1); points += Pair(0.15, -0.1); points += Pair(0.25, -0.1)
                points += Pair(-0.3, 0.1); points += Pair(-0.15, 0.1); points += Pair(0.0, 0.1); points += Pair(0.15, 0.1); points += Pair(0.3, 0.1)

                points += Pair(-0.2, 0.3); points += Pair(-0.1, 0.3); points += Pair(0.0, 0.3); points += Pair(0.1, 0.3); points += Pair(0.2, 0.3)
                points += Pair(-0.1, 0.5); points += Pair(0.0, 0.5); points += Pair(0.1, 0.5)
                points += Pair(0.0, 0.7)

                var index = 0
                Schedulers.sync().runRepeating({ task ->
                    if (index >= points.size)
                    {
                        task.close()
                        return@runRepeating
                    }
                    for (i in 0 until minOf(4, points.size - index))
                    {
                        val (px, py) = points[index + i]
                        world.playEffect(center.clone().add(px, py, 0.0), Effect.MOBSPAWNER_FLAMES, 0)
                    }
                    index += 4
                }, 0L, 2L).bindWith(lifecycle)
            }

            "bronze-shield" ->
            {

                val points = mutableListOf<Pair<Double, Double>>()

                val r = 0.5
                for (angle in 0 until 360 step 15)
                {
                    val rad = Math.toRadians(angle.toDouble())
                    points += Pair(Math.cos(rad) * r * 0.7, Math.sin(rad) * r)
                }

                for (i in -2..2)
                {
                    points += Pair(i * 0.1, 0.0) 
                    points += Pair(0.0, i * 0.15) 
                }

                var index = 0
                Schedulers.sync().runRepeating({ task ->
                    if (index >= points.size)
                    {
                        task.close()
                        return@runRepeating
                    }
                    for (i in 0 until minOf(4, points.size - index))
                    {
                        val (px, py) = points[index + i]
                        world.playEffect(center.clone().add(px, py, 0.0), Effect.MAGIC_CRIT, 0)
                    }
                    index += 4
                }, 0L, 1L).bindWith(lifecycle)
            }
        }
    }

    private fun configureIslandToppers()
    {
        Events
            .subscribe(GameStartEvent::class.java)
            .filter { it.game.identifier == game.identifier }
            .handler {
                Schedulers.sync().runLater({
                    val world = game.arenaWorld ?: return@runLater

                    game.teams.forEach { gameTeam ->
                        val team = gameTeam.teamIdentifier

                        val topperId = lifecycle.teamPlayers(team)
                            .mapNotNull { player ->
                                val profile = BedWarsProfileService.find(player) ?: return@mapNotNull null
                                val topper = profile.selectedIslandTopper
                                if (topper != null && topper != "none") topper else null
                            }
                            .firstOrNull() ?: return@forEach

                        val spawn = game.map.findSpawnLocationMatchingTeam(team)
                            ?.toLocation(world) ?: return@forEach

                        val base = spawn.clone().add(0.0, 5.0, 0.0)
                        val woolData = BedWarsTeamPalette.woolData(team)

                        buildIslandTopper(world, base, topperId, woolData)
                    }
                }, 20L).bindWith(lifecycle)
            }
            .bindWith(lifecycle)
    }

    @Suppress("DEPRECATION")
    private fun buildIslandTopper(
        world: org.bukkit.World,
        base: Location,
        topperId: String,
        woolData: Int
    )
    {
        when (topperId)
        {
            "flag" ->
            {

                for (y in 0..4) setBlock(world, base.clone().add(0.0, y.toDouble(), 0.0), Material.FENCE)

                for (x in -1..1)
                {
                    setBlock(world, base.clone().add(x.toDouble(), 5.0, 0.0), Material.WOOL, woolData)
                    setBlock(world, base.clone().add(x.toDouble(), 4.0, 0.0), Material.WOOL, woolData)
                }
            }

            "pillar" ->
            {

                for (y in 0..5) setBlock(world, base.clone().add(0.0, y.toDouble(), 0.0), Material.QUARTZ_BLOCK, 2)

                setBlock(world, base.clone().add(0.0, 6.0, 0.0), Material.QUARTZ_BLOCK, 1)
            }

            "mushroom" ->
            {

                for (y in 0..3) setBlock(world, base.clone().add(0.0, y.toDouble(), 0.0), Material.HUGE_MUSHROOM_2, 10)

                for (x in -1..1)
                {
                    for (z in -1..1)
                    {
                        setBlock(world, base.clone().add(x.toDouble(), 4.0, z.toDouble()), Material.HUGE_MUSHROOM_1, 14)
                    }
                }
            }

            "cactus" ->
            {

                for (y in 0..5)
                {
                    val center = base.clone().add(0.0, y.toDouble(), 0.0)
                    for (offset in listOf(
                        intArrayOf(1, 0), intArrayOf(-1, 0),
                        intArrayOf(0, 1), intArrayOf(0, -1)
                    ))
                    {
                        val adj = center.clone().add(offset[0].toDouble(), 0.0, offset[1].toDouble())
                        if (world.getBlockAt(adj).type != Material.AIR)
                        {
                            world.getBlockAt(adj).type = Material.AIR
                        }
                    }
                }

                setBlock(world, base.clone().add(0.0, 0.0, 0.0), Material.SAND)
                for (y in 1..5) setBlock(world, base.clone().add(0.0, y.toDouble(), 0.0), Material.CACTUS)
            }

            "tree" ->
            {

                for (y in 0..3) setBlock(world, base.clone().add(0.0, y.toDouble(), 0.0), Material.LOG)

                for (y in 4..5)
                {
                    for (x in -1..1)
                    {
                        for (z in -1..1)
                        {
                            if (x == 0 && z == 0 && y == 4)
                            {
                                setBlock(world, base.clone().add(x.toDouble(), y.toDouble(), z.toDouble()), Material.LOG)
                            } else
                            {
                                setBlock(world, base.clone().add(x.toDouble(), y.toDouble(), z.toDouble()), Material.LEAVES)
                            }
                        }
                    }
                }

                setBlock(world, base.clone().add(0.0, 6.0, 0.0), Material.LEAVES)
            }

            "lighthouse" ->
            {

                for (x in -1..1)
                {
                    for (z in -1..1)
                    {
                        if (x == 0 && z == 0) continue
                        setBlock(world, base.clone().add(x.toDouble(), 0.0, z.toDouble()), Material.PRISMARINE, 2)
                    }
                }

                for (y in 0..5) setBlock(world, base.clone().add(0.0, y.toDouble(), 0.0), Material.PRISMARINE)

                setBlock(world, base.clone().add(0.0, 6.0, 0.0), Material.SEA_LANTERN)

                setBlock(world, base.clone().add(1.0, 6.0, 0.0), Material.STAINED_GLASS, woolData)
                setBlock(world, base.clone().add(-1.0, 6.0, 0.0), Material.STAINED_GLASS, woolData)
                setBlock(world, base.clone().add(0.0, 6.0, 1.0), Material.STAINED_GLASS, woolData)
                setBlock(world, base.clone().add(0.0, 6.0, -1.0), Material.STAINED_GLASS, woolData)
            }

            "castle" ->
            {

                for (y in 0..3)
                {
                    for (x in -1..1)
                    {
                        for (z in -1..1)
                        {
                            if (x == 0 && z == 0) continue
                            setBlock(world, base.clone().add(x.toDouble(), y.toDouble(), z.toDouble()), Material.SMOOTH_BRICK)
                        }
                    }
                }

                setBlock(world, base.clone().add(-1.0, 4.0, -1.0), Material.SMOOTH_BRICK)
                setBlock(world, base.clone().add(1.0, 4.0, -1.0), Material.SMOOTH_BRICK)
                setBlock(world, base.clone().add(-1.0, 4.0, 1.0), Material.SMOOTH_BRICK)
                setBlock(world, base.clone().add(1.0, 4.0, 1.0), Material.SMOOTH_BRICK)

                setBlock(world, base.clone().add(0.0, 0.0, 0.0), Material.FENCE)
                setBlock(world, base.clone().add(0.0, 1.0, 0.0), Material.FENCE)
                setBlock(world, base.clone().add(0.0, 2.0, 0.0), Material.FENCE)
                setBlock(world, base.clone().add(0.0, 3.0, 0.0), Material.WOOL, woolData)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun setBlock(world: org.bukkit.World, loc: Location, material: Material, data: Int = 0)
    {
        val block = world.getBlockAt(loc)
        block.type = material
        if (data != 0) block.data = data.toByte()
    }

    private fun configureShopKeeperSkins()
    {
        Events
            .subscribe(GameStartEvent::class.java)
            .filter { it.game.identifier == game.identifier }
            .handler {
                Schedulers.sync().runLater({
                    val world = game.arenaWorld ?: return@runLater

                    val shopKeeperMeta = game.map.metadata.metadata
                        .filterIsInstance<MapShopKeeperMetadata>()
                        .firstOrNull()

                    val upgraderMeta = game.map.metadata.metadata
                        .filterIsInstance<MapTeamUpgraderMetadata>()
                        .firstOrNull()

                    game.teams.forEach { gameTeam ->
                        val team = gameTeam.teamIdentifier

                        val skinId = lifecycle.teamPlayers(team)
                            .mapNotNull { player ->
                                val profile = BedWarsProfileService.find(player) ?: return@mapNotNull null
                                val skin = profile.selectedShopKeeperSkin
                                if (skin != "none") skin else null
                            }
                            .firstOrNull() ?: return@forEach

                        val resolvedSkinId = if (skinId == "random")
                        {
                            ShopKeeperSkinType.selectableEntries().randomOrNull()?.cosmetic?.id ?: return@forEach
                        }
                        else skinId

                        val skinEntry = ShopKeeperSkinType.findSkinEntry(resolvedSkinId)
                        val isPlayerSkin = ShopKeeperSkinType.isPlayerSkin(resolvedSkinId)
                        val entityClass = if (!isPlayerSkin) resolveShopKeeperEntityClass(resolvedSkinId) else null

                        if (!isPlayerSkin && entityClass == null) return@forEach

                        if (shopKeeperMeta != null)
                        {
                            lifecycle.npcSystem.removeShopNpcsForTeam(team.label)

                            val locations = shopKeeperMeta.getByTeam(team)
                            locations.forEach { position ->
                                val loc = position.toLocation(world).add(0.5, 0.0, 0.5)

                                if (isPlayerSkin && skinEntry != null)
                                {
                                    spawnPlayerSkinShopkeeper(
                                        world, loc, skinEntry,
                                        "${CC.GREEN}${CC.BOLD}ITEM SHOP",
                                        "${CC.YELLOW}Right Click",
                                        shopKeeperEntities
                                    )
                                }
                                else if (entityClass != null)
                                {
                                    val entity = spawnSkinnedNpc(world, loc, entityClass)
                                    shopKeeperEntities += entity
                                    spawnHologramAbove(world, loc, entity, "${CC.GREEN}${CC.BOLD}ITEM SHOP", "${CC.YELLOW}Right Click")
                                }
                            }
                        }

                        if (upgraderMeta != null)
                        {
                            lifecycle.npcSystem.removeUpgraderNpcsForTeam(team.label)

                            val locations = upgraderMeta.getByTeam(team)
                            locations.forEach { position ->
                                val loc = position.toLocation(world).add(0.5, 0.0, 0.5)

                                if (isPlayerSkin && skinEntry != null)
                                {
                                    spawnPlayerSkinShopkeeper(
                                        world, loc, skinEntry,
                                        "${CC.AQUA}${CC.BOLD}TEAM UPGRADES",
                                        "${CC.YELLOW}Right Click",
                                        upgraderEntities
                                    )
                                }
                                else if (entityClass != null)
                                {
                                    val entity = spawnSkinnedNpc(world, loc, entityClass)
                                    upgraderEntities += entity
                                    spawnHologramAbove(world, loc, entity, "${CC.AQUA}${CC.BOLD}TEAM UPGRADES", "${CC.YELLOW}Right Click")
                                }
                            }
                        }
                    }

                    val allSkinnedEntities = shopKeeperEntities + upgraderEntities

                    if (allSkinnedEntities.isNotEmpty())
                    {
                        Events
                            .subscribe(PlayerInteractEntityEvent::class.java)
                            .filter { event -> event.rightClicked in shopKeeperEntities || event.rightClicked in upgraderEntities }
                            .handler { event ->
                                if (GameService.byPlayer(event.player)?.identifier != game.identifier)
                                {
                                    return@handler
                                }
                                event.isCancelled = true

                                if (event.rightClicked in upgraderEntities)
                                {
                                    val playerTeam = game.getNullableTeam(event.player)
                                        ?.teamIdentifier ?: return@handler
                                    BedWarsUpgradeMenu(lifecycle, playerTeam).openMenu(event.player)
                                }
                                else
                                {
                                    BedWarsItemShopMenu(lifecycle).openMenu(event.player)
                                }
                            }
                            .bindWith(lifecycle)

                        Events
                            .subscribe(EntityDamageEvent::class.java)
                            .filter { event -> event.entity in shopKeeperEntities || event.entity in upgraderEntities }
                            .handler { event -> event.isCancelled = true }
                            .bindWith(lifecycle)

                        Events
                            .subscribe(EntityTargetEvent::class.java)
                            .filter { event -> event.entity in shopKeeperEntities || event.entity in upgraderEntities }
                            .handler { event -> event.isCancelled = true }
                            .bindWith(lifecycle)
                    }
                }, 5L).bindWith(lifecycle)
            }
            .bindWith(lifecycle)

        lifecycle.bind(me.lucko.helper.terminable.Terminable {
            shopKeeperEntities.forEach { entity ->
                if (entity.isValid && !entity.isDead) entity.remove()
            }
            shopKeeperEntities.clear()

            upgraderEntities.forEach { entity ->
                if (entity.isValid && !entity.isDead) entity.remove()
            }
            upgraderEntities.clear()

            hologramStands.forEach { stand ->
                if (stand.isValid && !stand.isDead) stand.remove()
            }
            hologramStands.clear()

            playerSkinNpcs.forEach { npc ->
                npc.destroyForCurrentWatchers()
                EntityHandler.forgetEntity(npc)
            }
            playerSkinNpcs.clear()
        })
    }

    private fun spawnSkinnedNpc(
        world: org.bukkit.World,
        loc: Location,
        entityClass: Class<out org.bukkit.entity.Entity>
    ): org.bukkit.entity.Entity
    {
        val entity = world.spawn(loc, entityClass)

        if (entity is LivingEntity)
        {
            entity.isCustomNameVisible = false
            entity.setCanPickupItems(false)

            val nmsEntity = (entity as CraftLivingEntity).handle
            val tag = NBTTagCompound()
            nmsEntity.c(tag)
            tag.setInt("NoAI", 1)
            nmsEntity.f(tag)
        }

        return entity
    }

    private fun spawnHologramAbove(
        world: org.bukkit.World,
        baseLoc: Location,
        entity: org.bukkit.entity.Entity,
        titleLine: String,
        subtitleLine: String
    )
    {

        val mobHeight = when (entity)
        {
            is IronGolem -> 2.7
            is Enderman -> 2.9
            is Blaze -> 1.8
            is Creeper -> 1.7
            is Skeleton -> 1.95
            is Zombie -> 1.95
            is Witch -> 1.95
            is PigZombie -> 1.95
            is Snowman -> 1.9
            else -> 1.95
        }

        
        val subtitleY = mobHeight - 0.98 + 0.15
        val titleY = subtitleY + 0.25

        val subtitleStand = world.spawn(
            baseLoc.clone().add(0.0, subtitleY, 0.0),
            ArmorStand::class.java
        )
        subtitleStand.isVisible = false
        subtitleStand.setGravity(false)
        subtitleStand.isSmall = true
        subtitleStand.customName = subtitleLine
        subtitleStand.isCustomNameVisible = true
        subtitleStand.setBasePlate(false)
        hologramStands += subtitleStand

        val titleStand = world.spawn(
            baseLoc.clone().add(0.0, titleY, 0.0),
            ArmorStand::class.java
        )
        titleStand.isVisible = false
        titleStand.setGravity(false)
        titleStand.isSmall = true
        titleStand.customName = titleLine
        titleStand.isCustomNameVisible = true
        titleStand.setBasePlate(false)
        hologramStands += titleStand
    }

    private fun resolveShopKeeperEntityClass(skinId: String): Class<out org.bukkit.entity.Entity>?
    {
        return when (skinId)
        {
            "zombie" -> Zombie::class.java
            "skeleton" -> Skeleton::class.java
            "creeper" -> Creeper::class.java
            "blaze" -> Blaze::class.java
            "witch" -> Witch::class.java
            "pigmen" -> PigZombie::class.java
            "snowman" -> Snowman::class.java
            else -> null
        }
    }

    private val playerSkinNpcs = mutableListOf<NpcEntity>()

    private fun spawnPlayerSkinShopkeeper(
        world: org.bukkit.World,
        loc: Location,
        skinEntry: ShopKeeperSkinType.ShopKeeperSkinEntry,
        titleLine: String,
        subtitleLine: String,
        targetList: MutableList<org.bukkit.entity.Entity>
    ): Boolean
    {
        val skinValue = skinEntry.skinValue ?: return false
        val skinSignature = skinEntry.skinSignature ?: return false

        val isUpgrader = titleLine.contains("UPGRADES")

        val npc = object : NpcEntity(listOf(titleLine, subtitleLine), loc)
        {
            override fun onRightClick(player: Player)
            {
                val playerGame = GameService.byPlayer(player)
                    ?: return
                if (playerGame.identifier != game.identifier) return

                if (isUpgrader)
                {
                    val playerTeam = game.getNullableTeam(player)
                        ?.teamIdentifier ?: return
                    BedWarsUpgradeMenu(lifecycle, playerTeam).openMenu(player)
                }
                else
                {
                    BedWarsItemShopMenu(lifecycle).openMenu(player)
                }
            }
        }
        npc.persistent = false
        npc.initializeData()
        npc.updateTexture(skinValue, skinSignature)
        EntityHandler.trackEntity(npc)
        playerSkinNpcs += npc

        return true
    }
}
