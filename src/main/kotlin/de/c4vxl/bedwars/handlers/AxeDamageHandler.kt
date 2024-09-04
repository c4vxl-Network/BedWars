package de.c4vxl.bedwars.handlers

import de.c4vxl.gamemanager.gamemanagementapi.player.GMAPlayer.Companion.asGamePlayer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.plugin.Plugin

class AxeDamageHandler(plugin: Plugin): Listener {
    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onPlayerDamage(event: EntityDamageByEntityEvent) {
        val player: Player = event.entity as? Player ?: return
        val damager: Player = event.damager as? Player ?: return

        if (player.asGamePlayer.game == null) return
        if (damager.asGamePlayer.game != player.asGamePlayer.game) return

        // check if used weapon is in blocked list
        if (!mutableListOf(
            Material.IRON_AXE,
            Material.STONE_AXE,
            Material.WOODEN_AXE,
            Material.DIAMOND_AXE
        ).contains(damager.inventory.itemInMainHand.type)) return


        // lower damage
        event.setDamage(EntityDamageEvent.DamageModifier.BASE, 1.0)
    }
}