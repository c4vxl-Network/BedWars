package de.c4vxl.bedwars.handlers

import de.c4vxl.bedwars.BedWars
import de.c4vxl.bedwars.shop.items.handlers.PlayerCompass
import de.c4vxl.bedwars.shop.items.handlers.TeamChest
import de.c4vxl.gamemanager.gamemanagementapi.GameManagementAPI
import de.c4vxl.gamemanager.gamemanagementapi.event.GameStopEvent
import de.c4vxl.gamemanager.gamemanagementapi.game.Game
import de.c4vxl.gamemanager.gamemanagementapi.game.GameID
import de.c4vxl.gamemanager.gamemanagementapi.player.GMAPlayer.Companion.asGamePlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.block.Bed
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.plugin.Plugin

class BlockHandler(plugin: Plugin): Listener {
    companion object {
        private val placedBlocks: MutableMap<Game, MutableList<Block>> = mutableMapOf()

        fun getPlacedBlocks(game: Game) = placedBlocks.getOrPut(game) { mutableListOf() }
        fun addBlock(game: Game, block: Block) = placedBlocks.put(game, getPlacedBlocks(game).apply { add(block) })
        fun isMapBlock(game: Game, block: Block) = !getPlacedBlocks(game).contains(block)
    }

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player: Player = event.player
        val game: Game = player.asGamePlayer.game ?: return

        // return if game is not running
        if (!game.isRunning) return

        val block = event.block

        // return if block is not in game world
        if (block.world.name != game.worldManager.world?.name) return

        event.isCancelled = false
        addBlock(game, block) // add block to list
    }

    @EventHandler
    fun onExplosion(event: EntityExplodeEvent) {
        val blocks = event.blockList()

        // Iterate through the blocks affected by the explosion
        blocks.removeIf { block ->
            val game: Game = GameManagementAPI.getGame(GameID.fromString(block.world.name)) ?: return@removeIf false
            isMapBlock(game, block)
        }
    }

    @EventHandler
    fun onBlockDestroyByPlayer(event: BlockBreakEvent) {
        val player: Player = event.player
        val block = event.block
        val game = player.asGamePlayer.game ?: return

        // return if player is in creative mode
        if (player.gameMode == GameMode.CREATIVE) return

        // return if game is not running
        if (!game.isRunning) return

        // return if block is not in game world
        if (block.world.name != game.worldManager.world?.name) return

        // let event go through if block is not a part of the map
        if (!isMapBlock(game, block)) {
            return
        }

        // return if the block is a bed
        if (block.state is Bed) {
            event.isDropItems = false
            return
        }

        // allow blocks like flowers to be destroyed
        if (!block.type.isSolid) {
            event.isDropItems = false
            return
        }

        // warn player
        player.sendMessage(BedWars.prefix.append(Component.text("Hey! ").color(NamedTextColor.RED)).append(Component.text("You cannot break blocks of the map")))

        // cancel
        event.isCancelled = true
    }

    @EventHandler
    fun onGameStop(event: GameStopEvent) {
        // remove placed blocks array for a game as it stops
        // we do this, so we don't have to iterate over data which we will never use again
        placedBlocks.remove(event.game)

        // remove team chests of game
        TeamChest.remove(event.game)

        // remove player targets
        PlayerCompass.remove(event.game)
    }
}