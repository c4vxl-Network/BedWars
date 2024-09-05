package de.c4vxl.bedwars.utils

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
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.UUID

class ItemBuilder(
    var material: Material,
    var name: Component? = null,
    var amount: Int = 1,
    var lore: MutableList<Component> = mutableListOf(),
    var unbreakable: Boolean = false,
    var enchantments: MutableMap<Enchantment, Int> = mutableMapOf(),
    var interactonHandler: ((PlayerInteractEvent) -> Unit)? = null,
    var invClickHandler: ((InventoryClickEvent) -> Unit)? = null,
    var itemMeta: ItemMeta? = null
) {
    companion object : Listener {
        val interactionHandlers: MutableMap<String, (PlayerInteractEvent) -> Unit> = mutableMapOf()
        val inventoryClickHandlers: MutableMap<String, (InventoryClickEvent) -> Unit> = mutableMapOf()

        fun register(plugin: Plugin) {
            Bukkit.getPluginManager().registerEvents(this, plugin)
        }

        @EventHandler
        fun onItemInteraction(event: PlayerInteractEvent) {
            val currentItem: ItemStack = event.item ?: return
            val meta = currentItem.itemMeta ?: return
            val key = meta.persistentDataContainer.get(UniqueKey.ITEM_KEY, PersistentDataType.STRING) ?: return

            interactionHandlers[key]?.invoke(event)
        }

        @EventHandler
        fun onInventoryClick(event: InventoryClickEvent) {
            val currentItem: ItemStack = event.currentItem ?: return
            val meta = currentItem.itemMeta ?: return
            val key = meta.persistentDataContainer.get(UniqueKey.ITEM_KEY, PersistentDataType.STRING) ?: return

            inventoryClickHandlers[key]?.invoke(event)
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

        val uniqueId = itemStack.itemMeta!!.persistentDataContainer.get(UniqueKey.ITEM_KEY, PersistentDataType.STRING)!!

        interactonHandler?.let { interactionHandlers[uniqueId] = it }
        invClickHandler?.let { inventoryClickHandlers[uniqueId] = it }

        return itemStack
    }
}

// Utility class to define unique keys for item metadata
object UniqueKey {
    val ITEM_KEY = org.bukkit.NamespacedKey.minecraft("unique_item_id")
}