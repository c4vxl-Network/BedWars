package de.c4vxl.bedwars.handlers

import de.c4vxl.bedwars.utils.ItemBuilder
import de.c4vxl.gamemanager.gamemanagementapi.game.Game
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector

object ItemSpawnerHandler {
    fun init(game: Game, plugin: Plugin) {
        val world = game.worldManager.world
        val config = game.worldManager.mapConfig.config
        val spawners = config.getConfigurationSection("bedwars.spawners")?.getKeys(false) ?: mutableListOf()

        spawners.forEach {
            val delay = config.getLong("bedwars.spawners.$it.delay")
            val material = Material.entries.find { d -> config.getString("bedwars.spawners.$it.item") == d.name } ?: return@forEach
            val itemName: String = config.getString("bedwars.spawners.$it.name").toString()
            val (x, y, z) = config.getIntegerList("bedwars.spawners.$it.location")
            val location = Location(world, x + 0.5, y + 0.5, z + 0.5)

            object : BukkitRunnable() {
                override fun run() {
                    // stop spawner if game is not running
                    if (!game.isRunning) {
                        cancel()
                        return
                    }

                    // drop item
                    val item = world?.dropItem(location, ItemBuilder(
                        material,
                        LegacyComponentSerializer.legacySection().deserialize(itemName),
                        unbreakable = true
                    ).build())

                    // set velocity to 0 so the items will drop at the exact center of the block
                    item?.velocity = Vector(0, 0, 0)
                }
            }.runTaskTimer(plugin, delay, delay)
        }
    }
}