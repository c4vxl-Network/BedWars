package de.c4vxl.bedwars.shop.items

import de.c4vxl.bedwars.BedWars
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
        timeRemaining[player.asGamePlayer] = 5

        object : BukkitRunnable() {
            override fun run() {
                timeRemaining[player.asGamePlayer]?.let { remaining -> // get remaining time
                    val game: Game = player.asGamePlayer.game?.takeIf { it.isRunning } ?: return
                    val team: Team = player.asGamePlayer.team ?: return
                    val spawnLocation: Location = game.worldManager.mapConfig.getTeamSpawn(team.id) ?: return

                    // send message
                    player.sendMessage(BedWars.prefix.append(Component.text("Teleporting in ").color(NamedTextColor.GREEN)
                        .append(Component.text("$remaining"))))

                    // spawn particles
                    createCircle(player.location.add(0.0, (remaining / 5).toDouble(), 0.0))

                    // teleport player if no time is remaining
                    if (remaining == 0) {
                        player.teleport(spawnLocation)
                        timeRemaining.remove(player.asGamePlayer)
                        Bukkit.getScheduler().cancelTask(taskId)
                        cancel()
                        return
                    }

                    // subtract 1 from remaining time
                    timeRemaining[player.asGamePlayer] = remaining - 1
                } ?: { // player moved
                    Bukkit.getScheduler().cancelTask(taskId)
                    cancel()
                }
            }
        }.runTaskTimer(BedWars.instance, 20, 20)
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        // return if player is not in a game
        event.player.asGamePlayer.game ?: return

        // return if player is not in a team
        event.player.asGamePlayer.team ?: return

        // only track if player moved an entire block
        // we do not care about small mouse adjustments
        if (event.to.block == event.from.block) return

        if (!timeRemaining.containsKey(event.player.asGamePlayer)) return

        // remove player from list
        // this will cancel the teleport
        timeRemaining.remove(event.player.asGamePlayer)

        // give item back
        event.player.inventory.addItem(Items(event.player.asGamePlayer.team!!).SPECIAL_BASE_TP)

        // send message
        event.player.sendMessage(BedWars.prefix.append(Component.text("You teleport has been cancelled due to your movement!").color(NamedTextColor.RED)))
    }
}