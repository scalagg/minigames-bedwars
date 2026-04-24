package gg.scala.minigame.bedwars.items.rotational

import gg.scala.minigame.bedwars.BedWarsLifecycle
import gg.scala.minigame.bedwars.shop.menu.BedWarsItemShopMenu
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import net.minecraft.server.v1_8_R3.NBTTagCompound
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity
import java.util.UUID

/**
 * @author Limo
 * @date 2/18/2026
 */
class PortableShopkeeperSystem(
    private val lifecycle: BedWarsLifecycle
)
{
    companion object
    {
        private const val LIFETIME_TICKS = 20L * 10
        private const val COOLDOWN_MS = 500L
    }

    private val activeVillagers = mutableListOf<Villager>()
    private val cooldowns = mutableMapOf<UUID, Long>()

    fun configure()
    {
        Events
            .subscribe(PlayerInteractEvent::class.java)
            .filter { event ->
                event.action == Action.RIGHT_CLICK_BLOCK || event.action == Action.RIGHT_CLICK_AIR
            }
            .filter { event ->
                val item = event.item ?: return@filter false
                item.type == Material.CHEST &&
                    item.itemMeta?.displayName?.contains("Portable Shopkeeper") == true
            }
            .filter { event ->
                val game = GameService.byPlayer(event.player)
                game?.identifier == lifecycle.game.identifier
            }
            .handler { event ->
                event.isCancelled = true
                val player = event.player

                if (!lifecycle.game.state(GameState.Playing)) return@handler

                val now = System.currentTimeMillis()
                if (now - cooldowns.getOrDefault(player.uniqueId, 0L) < COOLDOWN_MS) return@handler
                cooldowns[player.uniqueId] = now

                val world = lifecycle.game.arenaWorld ?: return@handler

                consumeOneFromHand(player)

                val spawnLoc = player.location.clone().add(
                    player.location.direction.clone().setY(0).normalize().multiply(2)
                )

                val villager = world.spawn(spawnLoc, Villager::class.java)
                villager.customName = "${CC.AQUA}${CC.BOLD}Portable Shop ${CC.GRAY}[10s]"
                villager.isCustomNameVisible = true
                villager.profession = Villager.Profession.LIBRARIAN

                val nmsEntity = (villager as CraftLivingEntity).handle
                val tag = NBTTagCompound()
                nmsEntity.c(tag)
                tag.setInt("NoAI", 1)
                nmsEntity.f(tag)

                activeVillagers += villager

                player.playSound(player.location, Sound.VILLAGER_YES, 1.0f, 1.0f)
                player.sendMessage("${CC.GREEN}Portable Shopkeeper deployed! It will vanish in 10 seconds.")

                Schedulers
                    .sync()
                    .runLater({
                        if (villager.isValid && !villager.isDead)
                        {
                            villager.remove()
                            activeVillagers -= villager
                        }
                    }, LIFETIME_TICKS)
                    .bindWith(lifecycle)
            }
            .bindWith(lifecycle)

        Events
            .subscribe(PlayerInteractAtEntityEvent::class.java)
            .filter { event ->
                event.rightClicked is Villager
            }
            .filter { event ->
                val villager = event.rightClicked as Villager
                villager.customName?.contains("Portable Shop") == true
            }
            .filter { event ->
                val game = GameService.byPlayer(event.player)
                game?.identifier == lifecycle.game.identifier
            }
            .handler { event ->
                event.isCancelled = true
                BedWarsItemShopMenu(lifecycle).openMenu(event.player)
            }
            .bindWith(lifecycle)
    }

    private fun consumeOneFromHand(player: Player)
    {
        val inHand = player.itemInHand ?: return
        if (inHand.amount <= 1)
        {
            player.inventory.removeItem(inHand)
        }
        else
        {
            inHand.amount -= 1
        }
        player.updateInventory()
    }

    fun cleanup()
    {
        activeVillagers.forEach { villager ->
            if (villager.isValid && !villager.isDead)
            {
                villager.remove()
            }
        }
        activeVillagers.clear()
    }
}
