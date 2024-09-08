package de.c4vxl.bedwars.handlers

import de.c4vxl.bedwars.shop.Shop
import de.c4vxl.gamemanager.gamemanagementapi.GameManagementAPI
import de.c4vxl.gamemanager.gamemanagementapi.game.Game
import de.c4vxl.gamemanager.gamemanagementapi.player.GMAPlayer.Companion.asGamePlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import java.lang.Math.toDegrees
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.sqrt

class ShopHandler(val plugin: Plugin): Listener {
    companion object {
        fun spawnShops(game: Game, plugin: Plugin) {
            val world = game.worldManager.world ?: return
            val config = game.worldManager.mapConfig.config
            val shops = config.getConfigurationSection("bedwars.shops")?.getKeys(false) ?: mutableListOf()

            shops.forEach {
                val (x, y, z) = config.getDoubleList("bedwars.shops.$it.location")
                val location: Location = Location(world, x + 0.5, y, z + 0.5)

                val entity: LivingEntity = world.spawnEntity(location, EntityType.VILLAGER) as LivingEntity
                entity.isCustomNameVisible = true
                entity.customName(Component.text("Shop").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                entity.isInvulnerable = true
                entity.fireTicks = 0
                entity.isVisualFire = false
                entity.setAI(false)
                entity.setGravity(false)
                entity.isSilent = true
                entity.persistentDataContainer.set(NamespacedKey(plugin, "isShop"), PersistentDataType.STRING, "true")
            }
        }
    }

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)

        // system for always making a shop look to the nearest player
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            GameManagementAPI.games.forEach { game ->
                if (!game.isRunning) return@forEach
                val world = game.worldManager.world ?: return@forEach
                val config = game.worldManager.mapConfig.config
                val shops = config.getConfigurationSection("bedwars.shops")?.getKeys(false) ?: mutableListOf()

                shops.forEach {
                    val (x, y, z) = config.getDoubleList("bedwars.shops.$it.location")
                    val location: Location = Location(world, x, y, z)
                    val shop = location.getNearbyEntitiesByType(Villager::class.java, 1.0).find { it.persistentDataContainer.has(NamespacedKey(plugin, "isShop")) } ?: return@forEach
                    shop.location.getNearbyPlayers(20.0).filter {
                        !it.asGamePlayer.isSpectating && it.asGamePlayer.game == game
                    }.firstOrNull()?.let { player ->
                        val height: Vector = shop.location.subtract(player.location).toVector().normalize()

                        // Calculate the pitch
                        val pitchInDegrees = toDegrees(atan(height.y / sqrt(height.x * height.x + height.z * height.z)))
                        val pitch = ((pitchInDegrees * 256.0) / 360.0).toFloat()

                        // Calculate the yaw
                        val yawInDegrees = toDegrees(atan2(-height.x, -height.z))
                        val yaw = ((yawInDegrees * 256.0) / 360.0).toFloat() * -1

                        shop.setRotation(yaw, pitch)
                    }
                }
            }
        }, 0, 0)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractAtEntityEvent) {
        // get objects
        val player = event.player
        val entity = event.rightClicked

        // return if player is not in a game
        event.player.asGamePlayer.game ?: return

        // return if entity is not a shop
        if (!entity.persistentDataContainer.has(NamespacedKey(plugin, "isShop"))) return

        event.isCancelled = true

        // don't let spectators interact with the shop
        if (player.asGamePlayer.isSpectating) return

        // open shop
        Shop.openShop(player)
    }
}