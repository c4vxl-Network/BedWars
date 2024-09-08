package de.c4vxl.bedwars.handlers

import de.c4vxl.bedwars.utils.ItemBuilder
import de.c4vxl.gamemanager.gamemanagementapi.game.Game
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector

data class Spawner(val location: Location, val delay: Int, val material: Material, val itemName: String, val delayDisplay: ArmorStand?)

object ItemSpawnerHandler {
    val spawners: MutableMap<Game, MutableList<Spawner>> = mutableMapOf()
    val spawnersTimings: MutableMap<Spawner, Int> = mutableMapOf()

    fun init(game: Game, plugin: Plugin) {
        val world = game.worldManager.world
        val config = game.worldManager.mapConfig.config

        val spawners = (config.getConfigurationSection("bedwars.spawners")?.getKeys(false) ?: mutableListOf()).mapNotNull {
            val (x, y, z) = config.getIntegerList("bedwars.spawners.$it.location")
            val loc = Location(world, x + 0.5, y + 0.5, z + 0.5)
            val mat = Material.getMaterial(config.getString("bedwars.spawners.$it.item") ?: "") ?: return@mapNotNull null

            Spawner(loc,
                (config.getLong("bedwars.spawners.$it.delay") / 20).toInt(),
                mat,
                config.getString("bedwars.spawners.$it.name").toString(),
                null
            )
        }


        ItemSpawnerHandler.spawners[game] = spawners.map { spawner ->
            var entity: ArmorStand? = (world?.spawnEntity(spawner.location, EntityType.ARMOR_STAND) as? ArmorStand)?.apply {
                isCustomNameVisible = true
                setGravity(false)
                isInvulnerable = true
                isInvisible = true
                isMarker = true
            }

            // if there is another display in a distance of 2.0 blocks
            // we won't use this entity or any other display in this radius
            val useDisplay = spawners.filter { it.location.distance(spawner.location) <= 2.0 }.size < 2
            if (!useDisplay) {
                entity?.remove()
                entity = null
            }

            Spawner(spawner.location, spawner.delay, spawner.material, spawner.itemName, entity)
        }.distinct().toMutableList()

        // task for spawners
        object : BukkitRunnable() {
            override fun run() {
                // stop spawner if game is not running
                if (!game.isRunning) {
                    cancel()
                    return
                }

                ItemSpawnerHandler.spawners[game]?.forEach { spawner ->
                    val remaining: Int = spawnersTimings.getOrPut(spawner) { spawner.delay }

                    spawner.delayDisplay?.customName(Component.text("Next drop in ").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD).append(
                        Component.text(remaining).color(NamedTextColor.GRAY)))

                    if (remaining > 0) {
                        spawnersTimings[spawner] = spawnersTimings[spawner]!! - 1
                    } else {
                        // drop item
                        val item = world?.dropItem(spawner.location, ItemBuilder(
                            spawner.material,
                            LegacyComponentSerializer.legacySection().deserialize(spawner.itemName),
                            unbreakable = true
                        ).build())

                        // set velocity to 0 so the items will drop at the exact center of the block
                        item?.velocity = Vector(0, 0, 0)

                        // reset timing
                        spawnersTimings.remove(spawner)
                    }
                }
            }
        }.runTaskTimer(plugin, 20, 20)
    }
}