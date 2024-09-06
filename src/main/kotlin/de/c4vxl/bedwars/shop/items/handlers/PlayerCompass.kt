package de.c4vxl.bedwars.shop.items.handlers

import de.c4vxl.bedwars.BedWars
import de.c4vxl.bedwars.shop.Shop
import de.c4vxl.bedwars.utils.ItemBuilder
import de.c4vxl.bedwars.utils.ScrollableInventory
import de.c4vxl.gamemanager.gamemanagementapi.event.GamePlayerRespawnEvent
import de.c4vxl.gamemanager.gamemanagementapi.event.GameWorldLoadEvent
import de.c4vxl.gamemanager.gamemanagementapi.game.Game
import de.c4vxl.gamemanager.gamemanagementapi.player.GMAPlayer
import de.c4vxl.gamemanager.gamemanagementapi.player.GMAPlayer.Companion.asGamePlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.meta.SkullMeta

object PlayerCompass: Listener {
    // map of gma player and the player he tracks
    val tracking: MutableMap<GMAPlayer, Player> = mutableMapOf()

    fun get(player: GMAPlayer): Player? = tracking[player]
    fun remove(game: Game) { game.players.apply { addAll(game.deadPlayers) }.forEach { tracking.remove(it) } }

    init {
        Bukkit.getPluginManager().registerEvents(this, BedWars.instance)
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player.asGamePlayer

        // return if game is null
        player.game ?: return

        // return if team is null (if player is a spectator)
        player.team ?: return

        // update compass
        val players: MutableList<GMAPlayer> = tracking.filter { (k, v) -> v.uniqueId == event.player.uniqueId }.keys.toMutableList()
        players.forEach { it.bukkitPlayer.compassTarget = event.to }
    }

    @EventHandler
    fun onDeath(event: GamePlayerRespawnEvent) {
        // remove tracker
        tracking.remove(event.player)

        if (!event.game.isRunning) return

        event.player.bukkitPlayer.compassTarget = event.game.worldManager.mapConfig.getTeamSpawn(event.player.team?.id ?: return) ?: return
    }

    @EventHandler
    fun onGameStart(event: GameWorldLoadEvent) {
        event.game.players.forEach {
            it.bukkitPlayer.compassTarget = event.game.worldManager.mapConfig.getTeamSpawn(it.team?.id ?: return) ?: return
        }
    }

    fun openGUI(player: Player) {
        player.openInventory(ScrollableInventory(player.asGamePlayer.game?.alivePlayers?.filter { it != player.asGamePlayer }?.map { gplayer ->
            val ib = ItemBuilder(
                material = Material.PLAYER_HEAD,
                name = Component.text(gplayer.bukkitPlayer.name),
                lore = mutableListOf(
                    Component.text("Team:"), Component.text(gplayer.team?.name ?: "None").color(
                        NamedTextColor.WHITE)),
                invClickHandler = { event: InventoryClickEvent ->
                    event.isCancelled = true

                    if (gplayer.bukkitPlayer.world.name != player.world.name) return@ItemBuilder

                    if (!Shop.removeCurrencyFromPlayer(player, Material.GOLD_INGOT, 3)) {
                        player.sendMessage(BedWars.prefix.append(Component.text("You cannot afford to track this player! You need to pay 3 Gold!").color(NamedTextColor.RED)))
                        return@ItemBuilder
                    }

                    // set target
                    tracking[player.asGamePlayer] = gplayer.bukkitPlayer

                    player.compassTarget = gplayer.bukkitPlayer.location

                    player.sendMessage(BedWars.prefix.append(Component.text("Now tracking: ").color(NamedTextColor.GREEN)).append(Component.text(gplayer.bukkitPlayer.name)))

                    player.closeInventory()
                }
            )

            ib.build().apply {
                val meta = this.itemMeta as? SkullMeta ?: return@apply
                meta.playerProfile = Bukkit.createProfile(gplayer.bukkitPlayer.uniqueId).apply { setTextures(null) }
                this.setItemMeta(meta)
            }
        }?.toMutableList() ?: return, "§6§lTracker").page(0))
    }
}