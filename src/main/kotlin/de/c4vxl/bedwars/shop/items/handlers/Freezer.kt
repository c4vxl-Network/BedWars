package de.c4vxl.bedwars.shop.items.handlers

import de.c4vxl.bedwars.BedWars
import de.c4vxl.gamemanager.gamemanagementapi.game.Game
import de.c4vxl.gamemanager.gamemanagementapi.player.GMAPlayer
import de.c4vxl.gamemanager.gamemanagementapi.player.GMAPlayer.Companion.asGamePlayer
import de.c4vxl.gamemanager.gamemanagementapi.team.Team
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.scheduler.BukkitRunnable
import kotlin.math.cos
import kotlin.math.sin

import org.bukkit.scheduler.BukkitTask

object Freezer : Listener {
    init {
        Bukkit.getPluginManager().registerEvents(this, BedWars.instance)
    }

    // Store both time remaining and the associated task for each player
    val timeRemaining: MutableMap<GMAPlayer, Int> = mutableMapOf()
    val freezeTasks: MutableMap<GMAPlayer, BukkitTask> = mutableMapOf()

    private fun createCircle(loc: Location) {
        val size = 1.0
        for (i in 0..359) {
            val angle = (i * Math.PI - 180)
            val x = size * cos(angle)
            val z = size * sin(angle)
            loc.world.spawnParticle(Particle.WATER_SPLASH, loc.add(x, 0.0, z), 1)
        }
    }

    fun freeze(player: Player) {
        val gamePlayer = player.asGamePlayer

        // Cancel any existing task for the player
        freezeTasks[gamePlayer]?.cancel()

        timeRemaining[gamePlayer] = 5

        // Start a new task
        val task = object : BukkitRunnable() {
            override fun run() {
                timeRemaining[gamePlayer]?.let { remaining -> // get remaining time
                    val game: Game = gamePlayer.game?.takeIf { it.isRunning } ?: return
                    val team: Team = gamePlayer.team ?: return

                    // send message
                    player.sendMessage(
                        BedWars.prefix.append(
                            Component.text("Unfreezing in ").color(NamedTextColor.GREEN)
                                .append(Component.text("$remaining"))))

                    // spawn particles
                    createCircle(player.location.add(0.0, (remaining / 5).toDouble(), 0.0))

                    // teleport player if no time is remaining
                    if (remaining == 0) {
                        timeRemaining.remove(gamePlayer)
                        freezeTasks.remove(gamePlayer)
                        cancel()
                        return
                    }

                    // subtract 1 from remaining time
                    timeRemaining[gamePlayer] = remaining - 1
                } ?: run {
                    // player moved or something went wrong
                    freezeTasks.remove(gamePlayer)
                    cancel()
                }
            }
        }.runTaskTimer(BedWars.instance, 20, 20)

        // Store the new task
        freezeTasks[gamePlayer] = task
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val gamePlayer = event.player.asGamePlayer

        // return if player is not in a game or team
        gamePlayer.game ?: return
        gamePlayer.team ?: return

        // only track if player moved an entire block
        if (event.to.block == event.from.block) return

        if (!timeRemaining.containsKey(gamePlayer)) return

        event.isCancelled = true
    }

    @EventHandler
    fun onHit(event: ProjectileHitEvent) {
        if (!event.entity.persistentDataContainer.has(NamespacedKey.minecraft("bw.item.freezer"))) return

        val hit = event.hitEntity as? Player ?: return

        freeze(hit)
    }
}