package de.c4vxl.bedwars.shop.items

import de.c4vxl.gamemanager.gamemanagementapi.game.Game
import de.c4vxl.gamemanager.gamemanagementapi.team.Team
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory

object TeamChest {
    val items: MutableMap<Team, Inventory> = mutableMapOf()

    fun get(team: Team): Inventory = items.getOrPut(team) { Bukkit.createInventory(null, 9 * 3, Component.text("Team Chest")) }
    fun remove(game: Game) = game.teamManager.teams.forEach { items.remove(it) }
}