package de.c4vxl.bedwars.utils

import de.c4vxl.bedwars.utils.TeamData.color
import de.c4vxl.gamelobby.utils.ItemBuilder
import de.c4vxl.gamemanager.gamemanagementapi.team.Team
import de.c4vxl.gamemanager.gamemanagementapi.world.WorldManager
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import java.io.File
import java.util.*

object TeamData {
    /**
     * This accesses the teams.yml storing data about the different teams
     * We expect this file to have a for each team additional information about the blocks stored
     *
     * Example:
     * 0:
     *   name: "Team 1"
     *   bedwars:
     *      block:
     *          general: RED_CONCRETE
     *          glass: RED_STAINED_GLASS
     *      color: RED
     */

    // functions for convenience
    private val Team.config get() = YamlConfiguration.loadConfiguration(File(WorldManager.mapsContainerPath, "teams.yml")).getConfigurationSection("${this.id}.bedwars")!!
    private fun materialFromString(str: String?, orElse: Material? = null) = Material.entries.find { it.name == str?.uppercase(Locale.getDefault()) } ?: orElse

    // blocks
    val Team.glassBlock get() = materialFromString(this.config.getString("block.glass"), Material.GLASS)!!
    val Team.generalBlock get() = materialFromString(this.config.getString("block.general"), Material.SANDSTONE)!!
    val Team.woolBlock get() = materialFromString(this.config.getString("block.wool"), Material.WHITE_WOOL)!!

    // colors
    val Team.color get() = config.getColor("color")


    // returns a list with itemstacks for the colored leather armor
    // can be directly passed to player.inventory.setArmorContents
    val Team.coloredLeatherArmor: Array<ItemStack> get() {
        val team = this

        fun getArmorPeace(piece: String) = ItemBuilder(
            Material.entries.find { it.name == "LEATHER_$piece" }!!,
            Component.text(piece.lowercase().replaceFirstChar { it.uppercase() })

        ).build().apply {
            // color armor
            val meta = this.itemMeta as? LeatherArmorMeta ?: return@apply
            meta.setColor(team.color)
            this.setItemMeta(meta)
        }.let {
            // cancel all invClickEvents
            ItemBuilder(it.type, itemMeta = it.itemMeta, invClickHandler = { it.isCancelled = true }).build()
        }

        return mutableListOf(
            getArmorPeace("BOOTS"),
            getArmorPeace("LEGGINGS"),
            getArmorPeace("CHESTPLATE"),
            getArmorPeace("HELMET"),
        ).toTypedArray()
    }
}