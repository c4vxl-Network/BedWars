package de.c4vxl.bedwars.handlers

import de.c4vxl.gamemanager.gamemanagementapi.player.GMAPlayer.Companion.asGamePlayer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.plugin.Plugin

class CraftingHandler(plugin: Plugin): Listener {
    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onItemCraft(event: CraftItemEvent) {
        val player = event.whoClicked as? Player ?: return

        // return if player is not in a game
        player.asGamePlayer.game ?: return

        event.isCancelled = true
    }
}