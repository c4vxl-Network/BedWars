package de.c4vxl.bedwars.handlers

import de.c4vxl.bedwars.utils.TeamData.coloredLeatherArmor
import de.c4vxl.gamemanager.gamemanagementapi.event.GameStateChangeEvent
import de.c4vxl.gamemanager.gamemanagementapi.event.GameWorldLoadEvent
import de.c4vxl.gamemanager.gamemanagementapi.game.GameState
import org.bukkit.Bukkit
import org.bukkit.Difficulty
import org.bukkit.GameRule
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

class GameStartHandler(val plugin: Plugin): Listener {
    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onWorldLoad(event: GameWorldLoadEvent) {
        val world = event.world
        val game = event.game

        // create item spawners
        ItemSpawnerHandler.init(game, plugin)

        // spawn shops
        ShopHandler.spawnShops(game, plugin)

        // set game-rules
        world.setGameRule(GameRule.KEEP_INVENTORY, true)
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)
        world.difficulty = Difficulty.PEACEFUL
        world.time = 1200
    }

    @EventHandler
    fun onGameStateChange(event: GameStateChangeEvent) {
        val game = event.game

        if (event.newState != GameState.RUNNING) return

        // make all teams wear their leather armor
        game.players.forEach {
            it.bukkitPlayer.inventory.armorContents = it.team!!.coloredLeatherArmor
        }
    }
}