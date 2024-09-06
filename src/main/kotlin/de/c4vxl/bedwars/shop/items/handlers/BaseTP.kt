package de.c4vxl.bedwars.shop.items.handlers

import de.c4vxl.bedwars.BedWars
import de.c4vxl.bedwars.shop.items.Items
import de.c4vxl.gamemanager.gamemanagementapi.game.Game
import de.c4vxl.gamemanager.gamemanagementapi.player.GMAPlayer
import de.c4vxl.gamemanager.gamemanagementapi.player.GMAPlayer.Companion.asGamePlayer
import de.c4vxl.gamemanager.gamemanagementapi.team.Team
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.scheduler.BukkitRunnable
import kotlin.math.cos
import kotlin.math.sin

object BaseTP : Listener {
    init {
        Bukkit.getPluginManager().registerEvents(this, BedWars.instance)
    }

    val timeRemaining: MutableMap<GMAPlayer, Int> = mutableMapOf()
    val runningTasks: MutableMap<GMAPlayer, BukkitRunnable> = mutableMapOf()

    private fun createCircle(loc: Location) {
        val size = 1.0
        for (i in 0..359) {
            val angle = (i * Math.PI - 180)
            val x = size * cos(angle)
            val z = size * sin(angle)
            loc.world.spawnParticle(Particle.CRIT, loc.add(x, 0.0, z), 10)
        }
    }

    fun start(player: Player) {
        val gamePlayer = player.asGamePlayer

        // If the player is already in the map, reset the time and cancel the old task
        if (timeRemaining.containsKey(gamePlayer)) {
            timeRemaining[gamePlayer] = 5
            runningTasks[gamePlayer]?.cancel()
        } else {
            timeRemaining[gamePlayer] = 5
        }

        // Create a new BukkitRunnable task
        val task = object : BukkitRunnable() {
            override fun run() {
                timeRemaining[gamePlayer]?.let { remaining ->
                    val game: Game = gamePlayer.game?.takeIf { it.isRunning } ?: return
                    val team: Team = gamePlayer.team ?: return
                    val spawnLocation: Location = game.worldManager.mapConfig.getTeamSpawn(team.id) ?: return

                    if (player.world != game.worldManager.world) return

                    // Send message to the player
                    player.sendMessage(BedWars.prefix.append(
                        Component.text("Teleporting in ").color(NamedTextColor.GREEN)
                            .append(Component.text("$remaining"))
                    ))

                    // Spawn particles
                    createCircle(player.location.add(0.0, (remaining / 5).toDouble(), 0.0))

                    // If time is up, teleport the player and stop the task
                    if (remaining == 0) {
                        player.teleport(spawnLocation)
                        timeRemaining.remove(gamePlayer)
                        runningTasks.remove(gamePlayer)
                        cancel()
                        return
                    }

                    // Subtract 1 from remaining time
                    timeRemaining[gamePlayer] = remaining - 1
                } ?: {
                    cancel()
                }
            }
        }

        // Store and start the task
        runningTasks[gamePlayer] = task
        task.runTaskTimer(BedWars.instance, 20, 20)
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val gamePlayer = event.player.asGamePlayer

        // Return if player is not in a game or team
        gamePlayer.game ?: return
        gamePlayer.team ?: return

        // Only track if the player moved an entire block (ignore small mouse adjustments)
        if (event.to.block == event.from.block) return

        if (!timeRemaining.containsKey(gamePlayer)) return

        // Remove player from frozen list, cancelling teleport
        timeRemaining.remove(gamePlayer)

        // Cancel the task if it's running
        runningTasks[gamePlayer]?.cancel()
        runningTasks.remove(gamePlayer)

        // Give the teleport item back to the player
        event.player.inventory.addItem(Items(gamePlayer.team!!).SPECIAL_BASE_TP)

        // Send cancellation message
        event.player.sendMessage(
            BedWars.prefix.append(
                Component.text("Your teleport has been cancelled due to your movement!").color(NamedTextColor.RED)
            )
        )
    }
}