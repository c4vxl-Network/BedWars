package de.c4vxl.bedwars.handlers

import de.c4vxl.bedwars.shop.Shop
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
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class ShopHandler(val plugin: Plugin): Listener {
    companion object {
        fun spawnShops(game: Game, plugin: Plugin) {
            val world = game.worldManager.world ?: return
            val config = game.worldManager.mapConfig.config
            val shops = config.getConfigurationSection("bedwars.shops")?.getKeys(false) ?: mutableListOf()

            shops.forEach {
                val (x, y, z, yaw, pitch) = config.getFloatList("bedwars.shops.$it.location")
                val location: Location = Location(world, x.toDouble(), y.toDouble(), z.toDouble(), yaw, pitch)

                val entity: LivingEntity = world.spawnEntity(location, EntityType.VILLAGER) as LivingEntity
                entity.isCustomNameVisible = true
                entity.customName(Component.text("Shop").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                entity.isInvulnerable = true
                entity.fireTicks = 0
                entity.isVisualFire = false
                entity.setAI(false)
                entity.setGravity(false)
                entity.persistentDataContainer.set(NamespacedKey(plugin, "isShop"), PersistentDataType.BOOLEAN, true)
            }
        }
    }

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractAtEntityEvent) {
        // get objects
        val player = event.player
        val entity = event.rightClicked

        // return if player is not in a game
        event.player.asGamePlayer.game ?: return

        // return if entity is not a shop
        if (entity.persistentDataContainer.get(NamespacedKey(plugin, "isShop"), PersistentDataType.BOOLEAN) != true) return

        event.isCancelled = true

        // don't let spectators interact with the shop
        if (player.asGamePlayer.isSpectating) return

        // open shop
        Shop.openShop(player)
    }
}