package de.c4vxl.gamelobby.utils

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class ItemBuilder(
    var material: Material,
    var name: Component? = null,
    var amount: Int = 1,
    var lore: MutableList<Component> = mutableListOf(),
    var unbreakable: Boolean = false,
    var enchantments: MutableMap<Enchantment, Int> = mutableMapOf(),
    var interactonHandler: ((PlayerInteractEvent) -> Unit)? = null,
    var invClickHandler: ((InventoryClickEvent) -> Unit)? = null,
    var itemMeta: ItemMeta? = null) {
    companion object : Listener {
        val interactionHandlers: MutableMap<Int, (PlayerInteractEvent) -> Unit> = mutableMapOf()
        val inventoryClickHandlers: MutableMap<Int, (InventoryClickEvent) -> Unit> = mutableMapOf()

        fun register(plugin: Plugin) {
            Bukkit.getPluginManager().registerEvents(this, plugin)
        }

        @EventHandler
        fun onItemInteraction(event: PlayerInteractEvent) {
            val currentItem: ItemStack = event.item ?: return
            interactionHandlers[currentItem.itemMeta?.hashCode()]?.invoke(event)
        }

        @EventHandler
        fun onInventoryClick(event: InventoryClickEvent) {
            val currentItem: ItemStack = event.currentItem ?: return
            inventoryClickHandlers[currentItem.itemMeta?.hashCode()]?.invoke(event)
        }
    }

    fun build(): ItemStack {
        val itemStack = ItemStack(material, amount).apply {
            this@ItemBuilder.itemMeta = this@ItemBuilder.itemMeta ?: this.itemMeta
            if (this@ItemBuilder.name != null) this@ItemBuilder.itemMeta!!.displayName(this@ItemBuilder.name)
            this@ItemBuilder.itemMeta!!.lore(this@ItemBuilder.lore)
            this@ItemBuilder.itemMeta!!.isUnbreakable = this@ItemBuilder.unbreakable
            this.itemMeta = this@ItemBuilder.itemMeta

            this.addUnsafeEnchantments(this@ItemBuilder.enchantments)
        }

        interactonHandler?.let { interactionHandlers[itemMeta.hashCode()] = it }
        invClickHandler?.let { inventoryClickHandlers[itemMeta.hashCode()] = it }

        return itemStack
    }
}