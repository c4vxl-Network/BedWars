package de.c4vxl.bedwars.handlers

import de.c4vxl.bedwars.scoreboard.BedScoreboard
import de.c4vxl.gamemanager.gamemanagementapi.event.GamePlayerQuitEvent
import de.c4vxl.gamemanager.gamemanagementapi.event.GameSpectateStartEvent
import de.c4vxl.gamemanager.gamemanagementapi.event.GameStartEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

class ScoreboardHandler(val plugin: Plugin): Listener {
    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onStart(event: GameStartEvent) {
        BedScoreboard.update(event.game)
    }

    @EventHandler
    fun onQuit(event: GamePlayerQuitEvent) {
        event.player.bukkitPlayer.scoreboard.getObjective("bedwarsBedsDisplay")?.unregister()
    }

    @EventHandler
    fun onSpecJoin(event: GameSpectateStartEvent) {
        BedScoreboard.update(event.game)
    }
}