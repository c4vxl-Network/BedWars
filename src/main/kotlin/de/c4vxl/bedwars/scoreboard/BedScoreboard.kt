package de.c4vxl.bedwars.scoreboard

import de.c4vxl.bedwars.BedWars
import de.c4vxl.bedwars.handlers.RespawnHandler
import de.c4vxl.gamemanager.gamemanagementapi.game.Game
import org.bukkit.scoreboard.DisplaySlot

object BedScoreboard {
    fun update(game: Game) {
        game.players.apply { addAll(game.spectators) }.map { it.bukkitPlayer }.forEach { player ->
            val scoreboard = game.scoreboard
            scoreboard.getObjective("bedwarsBedsDisplay")?.unregister()
            val objective = scoreboard.registerNewObjective("bedwarsBedsDisplay", "dummy", BedWars.prefix)
            objective.displaySlot = DisplaySlot.SIDEBAR

            fun set(content: String, id: Int) { objective.getScore(content).score = id }

            set("  ", 16)

            game.teamManager.teams.forEachIndexed { index, team ->
                val alivePlayers = team.players.filter { game.alivePlayers.contains(it) }

                val canRespawn: Boolean = RespawnHandler.canRespawn[team] ?: team.players.isNotEmpty()

                set("${team.name}  §r-  ${
                    if (canRespawn) "§a§l✔"
                    else if (alivePlayers.isNotEmpty()) "§7${alivePlayers.size} Remaining"
                    else "§c§l✕"
                }", index)
            }
        }
    }
}