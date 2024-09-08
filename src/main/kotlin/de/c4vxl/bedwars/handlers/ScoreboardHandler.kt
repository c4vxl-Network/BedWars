package de.c4vxl.bedwars.handlers

import de.c4vxl.bedwars.scoreboard.BedScoreboard
import de.c4vxl.gamemanager.gamemanagementapi.event.*
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

class ScoreboardHandler(val plugin: Plugin): Listener {
    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onStart(event: GameStateChangeEvent) {
        if (!event.game.isRunning) return

        BedScoreboard.update(event.game)
    }

    @EventHandler
    fun onGameFinish(event: GameFinishEvent) {
        event.game.players.apply { addAll(event.game.spectators); addAll(event.game.deadPlayers) }.distinct().filter { it.game == event.game || it.game == null }.forEach {
            it.bukkitPlayer.scoreboard.getObjective("bedwarsBedsDisplay")?.unregister()
        }
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