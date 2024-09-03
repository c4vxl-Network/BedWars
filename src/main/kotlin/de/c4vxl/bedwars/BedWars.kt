package de.c4vxl.bedwars

import de.c4vxl.bedwars.handlers.BlockHandler
import de.c4vxl.bedwars.handlers.GameStartHandler
import de.c4vxl.bedwars.handlers.RespawnHandler
import de.c4vxl.bedwars.handlers.ShopHandler
import de.c4vxl.gamelobby.utils.ItemBuilder
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.plugin.java.JavaPlugin

class BedWars : JavaPlugin() {
    companion object {
        val prefix: Component = Component.text("[").color(NamedTextColor.GRAY)
            .append(Component.text("BedWars").color(NamedTextColor.GOLD))
            .append(Component.text("] ").color(NamedTextColor.GRAY))

        lateinit var instance: JavaPlugin
    }

    override fun onLoad() {
        instance = this

        CommandAPI.onLoad(CommandAPIBukkitConfig(this).silentLogs(true))
    }

    override fun onEnable() {
        // register ItemBuilder
        ItemBuilder.register(this)

        // register commands
        CommandAPI.onEnable()


        // register listeners
        RespawnHandler(this)
        ShopHandler(this)
        GameStartHandler(this)
        BlockHandler(this)

        logger.info("[+] $name has been enabled! \n  -> using version ${pluginMeta.version}")
    }

    override fun onDisable() {
        CommandAPI.onDisable()

        logger.info("[-] $name has been disabled!")
    }
}