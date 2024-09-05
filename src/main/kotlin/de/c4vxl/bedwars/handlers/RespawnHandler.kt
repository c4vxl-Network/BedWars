package de.c4vxl.bedwars.handlers

import de.c4vxl.bedwars.BedWars
import de.c4vxl.bedwars.utils.TeamData.generalBlock
import de.c4vxl.bedwars.utils.TeamData.glassBlock
import de.c4vxl.bedwars.utils.TeamData.woolBlock
import de.c4vxl.gamemanager.gamemanagementapi.event.GamePlayerRespawnEvent
import de.c4vxl.gamemanager.gamemanagementapi.event.GameStopEvent
import de.c4vxl.gamemanager.gamemanagementapi.game.Game
import de.c4vxl.gamemanager.gamemanagementapi.player.GMAPlayer.Companion.asGamePlayer
import de.c4vxl.gamemanager.gamemanagementapi.team.Team
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.TitlePart
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import kotlin.math.abs

class RespawnHandler(plugin: Plugin): Listener {
    companion object {
        // we can use team itself as key, because every game has its own Team instances
        val canRespawn: MutableMap<Team, Boolean> = mutableMapOf()

        // list of all item types that will be dropped when player dies
        // all others (except armor) will be removed
        fun getMaterialsToKeepOnDeath(team: Team): MutableList<Material> = mutableListOf(
                Material.IRON_INGOT,
                Material.GOLD_INGOT,
                Material.BRICK,
                Material.DIAMOND,
                Material.FLINT_AND_STEEL,
                team.generalBlock,
                team.woolBlock,
                team.glassBlock
            )
    }

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onGameStop(event: GameStopEvent) {
        event.game.teamManager.teams.forEach { canRespawn.remove(it) }
    }

    // handle death messages
    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.player.asGamePlayer
        val game = player.game ?: return

        // return if game is not running
        if (!game.isRunning) return

        event.keepInventory = true

        // drop items
        val drops = player.bukkitPlayer.inventory.storageContents.filterNotNull().filter { getMaterialsToKeepOnDeath(player.team ?: return).contains(it.type) }
        drops.forEach {
            player.bukkitPlayer.world.dropItemNaturally(player.bukkitPlayer.location, it)
        }

        // clear inventory except armor content
        player.bukkitPlayer.inventory.storageContents = mutableListOf<ItemStack?>().toTypedArray()
        player.bukkitPlayer.inventory.extraContents = mutableListOf<ItemStack?>().toTypedArray()

        val killer = event.player.killer

        if (killer != null) {
            game.broadcast(BedWars.prefix.append(Component.text(player.bukkitPlayer.name).color(NamedTextColor.WHITE)
                    .append(Component.text(" has been"))
                    .append(Component.text(" killed").color(NamedTextColor.RED))
                    .append(Component.text(" by"))
                    .append(Component.text(" ${killer.name}").color(NamedTextColor.WHITE))
                    .append(Component.text("!"))
            ))
        } else {
            game.broadcast(BedWars.prefix.append(Component.text(player.bukkitPlayer.name).color(NamedTextColor.WHITE)
                .append(Component.text(" died!"))
            ))
        }
    }

    @EventHandler
    fun onFallOutOfWorld(event: PlayerMoveEvent) {
        val player = event.player.asGamePlayer
        val game = player.game ?: return
        val world = event.to.world

        // return if game is not running
        if (!game.isRunning) return
        if (player.isSpectating) return

        // return if event is not in game world
        if (world.name != game.worldManager.world?.name) return

        if (event.to.blockY > world.minHeight) return

        // kill player
        player.bukkitPlayer.health = 0.0
    }

    @EventHandler
    fun onBedBreak(event: BlockBreakEvent) {
        // check if block is a bed
        val block = event.block
        if (!block.type.name.contains("BED")) return

        // get player and game
        val player = event.player.asGamePlayer
        val game: Game = player.game ?: return

        // return if game is not running
        if (!game.isRunning) return

        // return if event is not in game world
        if (game.worldManager.world?.name != player.bukkitPlayer.world.name) return

        val config = game.worldManager.mapConfig.config
        val keys = config.getConfigurationSection("bedwars.beds")?.getKeys(false) ?: return

        // get id of team who owns the bed
        val teamID = keys.find {
            val (x, y, z) = config.getIntegerList("bedwars.beds.$it.location")
            abs(x - block.location.blockX) <= 1 && abs(y - block.location.blockY) <= 1 && abs(z - block.location.blockZ) <= 1
        }?.toIntOrNull() ?: return

        // get team from id
        val team = game.teamManager.teams.getOrNull(teamID)

        if (team == null || team.players.isEmpty()) {
            player.bukkitPlayer.sendMessage(BedWars.prefix.append(Component.text("This team does not exist!").color(NamedTextColor.RED)))
            event.isCancelled = true
            return
        }

        // cancel and return if player is in team of the bed
        if (team.players.contains(player)) {
            player.bukkitPlayer.sendMessage(BedWars.prefix.append(Component.text("You cannot destroy your own bed!").color(NamedTextColor.RED)))
            event.isCancelled = true
            return
        }

        event.isDropItems = false

        // set canRespawn for team to false
        canRespawn[team] = false

        // notify team members
        team.players.map { it.bukkitPlayer }.forEach {
            it.sendTitlePart(TitlePart.TITLE, Component.text("Your bed has been ").append(Component.text("DESTROYED").color(NamedTextColor.RED)).append(Component.text("!")))
            it.sendTitlePart(TitlePart.SUBTITLE, Component.text("You will no longer respawn!"))
        }

        // broadcast message
        game.broadcast(BedWars.prefix.append(Component.text("The bed of Team ")
            .append(LegacyComponentSerializer.legacySection().deserialize(team.name))
            .append(Component.text(" has been"))
            .append(Component.text(" DESTROYED").color(NamedTextColor.RED))
            .append(Component.text(" by"))
            .append(Component.text(" ${player.bukkitPlayer.name}").color(NamedTextColor.WHITE))
            .append(Component.text("!"))
        ))
    }

    // using GamePlayerRespawnEvent
    //  - will be triggered as soon as a player has been respawned at his team's spawn
    //  - if player can respawn: do nothing as game-manager has already respawned the player
    //  - if player can not respawn (bed is destroyed): eliminate player
    @EventHandler
    fun onPlayerDeath(event: GamePlayerRespawnEvent) {
        val player = event.player
        val team: Team = player.team ?: return

        // return if team can not respawn
        if (canRespawn.getOrPut(team) { true }) return

        // make player a spectator
        player.spectate(event.game)
        player.bukkitPlayer.sendTitlePart(TitlePart.TITLE, Component.text("You have been ").append(Component.text("ELIMINATED").color(NamedTextColor.RED)).append(Component.text("!")))
    }
}