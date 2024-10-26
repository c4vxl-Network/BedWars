package de.c4vxl.bedwars.handlers

import de.c4vxl.bedwars.shop.items.Items
import de.c4vxl.gamemanager.gamemanagementapi.event.GamePlayerRespawnEvent
import de.c4vxl.gamemanager.gamemanagementapi.event.GameStateChangeEvent
import de.c4vxl.gamemanager.gamemanagementapi.game.GameState
import de.c4vxl.gamemanager.gamemanagementapi.player.GMAPlayer.Companion.asGamePlayer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.plugin.Plugin

class SwordItemHandler(plugin: Plugin): Listener {
    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onSwordGet(event: InventoryClickEvent) {
        val player: Player = event.whoClicked as? Player ?: return
        player.asGamePlayer.game ?: return
        val item = event.currentItem ?: return

        item.type.name.takeIf { it.endsWith("SWORD") } ?: return

        player.inventory.remove(Items(player.asGamePlayer.team ?: return).TOOLS_SWORD_LVL1.type)
    }

    @EventHandler
    fun onSpawn(event: GamePlayerRespawnEvent) {
        event.player.bukkitPlayer.inventory.setItem(0, Items(event.player.team ?: return).TOOLS_SWORD_LVL1)
    }

    @EventHandler
    fun onStart(event: GameStateChangeEvent) {
        if (event.newState != GameState.RUNNING) return

        event.game.teamManager.teams.forEach {
            it.players.forEach {
                it.bukkitPlayer.inventory.setItem(0, Items(it.team ?: return).TOOLS_SWORD_LVL1)
            }
        }
    }
}