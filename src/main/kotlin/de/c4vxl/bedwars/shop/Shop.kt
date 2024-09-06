package de.c4vxl.bedwars.shop


import de.c4vxl.bedwars.BedWars
import de.c4vxl.bedwars.shop.items.Items
import de.c4vxl.bedwars.utils.ItemBuilder
import de.c4vxl.bedwars.utils.TeamData.generalBlock
import de.c4vxl.gamemanager.gamemanagementapi.player.GMAPlayer.Companion.asGamePlayer
import de.c4vxl.gamemanager.gamemanagementapi.team.Team
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

object Shop {
    fun removeCurrencyFromPlayer(player: Player, currency: Material, price: Int): Boolean {
        var remainingAmount = price
        val inventory = player.inventory

        // check if player can afford the item
        var missing: Int = price
        inventory.contents.filterNotNull().filter { it.type == currency }.forEach { missing -= it.amount }
        if (missing > 0) return false

        inventory.contents = inventory.contents.apply {
            this.filterNotNull().filter { it.type == currency }.forEach { item ->
                if (remainingAmount == 0) return@apply

                if (item.amount >= remainingAmount) {
                    item.amount -= remainingAmount
                    remainingAmount = 0
                } else {
                    item.amount -= remainingAmount
                    remainingAmount = item.amount
                }
            }
        }

        return true
    }

    private fun buyableItem(item: ItemStack, currency: Material, price: Int, amount: Int = 1): ItemStack {
        return ItemBuilder(
            item.type,
            item.itemMeta.displayName() ?: Component.text(""),
            amount,
            mutableListOf(
                Component.text(""),
                Component.text("Cost: ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text("$price ${currency.name.split("_")[0].lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase() }}")
                        .color(NamedTextColor.WHITE).decorate()),
                Component.text(""),
                Component.text("Shift Click to buy all").color(NamedTextColor.AQUA).decorate(),
                Component.text("Click to purchase").color(NamedTextColor.YELLOW).decorate()
            ),
            item.itemMeta?.isUnbreakable ?: false,
            item.enchantments,
            eventKey = "bw.shop.buyableItem",
            invClickHandler = { event: InventoryClickEvent ->
                event.isCancelled = true

                val player: Player = event.whoClicked as? Player ?: return@ItemBuilder

                when (event.action) {
                    InventoryAction.MOVE_TO_OTHER_INVENTORY -> {
                        // Attempt to buy the item for (item.type.maxStackSize / amount) times
                        val maxPurchases: Int = (item.type.maxStackSize / amount)
                        for (i in 0..<maxPurchases) {
                            if (removeCurrencyFromPlayer(player, currency, price)) {
                                player.inventory.addItem(item.apply { this.amount = amount })
                            } else {
                                if (i == 0) player.sendMessage(BedWars.prefix.append(Component.text("You cannot afford this item!").color(NamedTextColor.RED)))
                                return@ItemBuilder
                            }
                        }
                    }

                    InventoryAction.HOTBAR_SWAP, InventoryAction.HOTBAR_MOVE_AND_READD -> {
                        // Handle moving the item to the hotbar
                        val hotbarSlot = event.hotbarButton
                        val oldItem = player.inventory.getItem(hotbarSlot)

                        if (removeCurrencyFromPlayer(player, currency, price)) {
                            player.inventory.setItem(hotbarSlot, item.apply {
                                this.amount = amount
                            })

                            // Add the old item back to inventory
                            if (oldItem != null) {
                                player.inventory.addItem(oldItem)
                            }
                        } else {
                            player.sendMessage(BedWars.prefix.append(Component.text("You cannot afford this item!").color(NamedTextColor.RED)))
                        }
                    }

                    else -> {
                        // Buy the item exactly one time
                        if (removeCurrencyFromPlayer(player, currency, price)) {
                            player.inventory.addItem(item.apply {
                                this.amount = amount
                            })
                        } else {
                            player.sendMessage(BedWars.prefix.append(Component.text("You cannot afford this item!").color(NamedTextColor.RED)))
                        }
                    }
                }
            },
            itemMeta = item.itemMeta
        ).build()
    }

    private fun armorItem(item: ItemStack, currency: Material, price: Int): ItemStack {
        return ItemBuilder(
            item.type,
            item.itemMeta.displayName() ?: Component.text(""),
            item.amount,
            mutableListOf(Component.text(""), Component.text("Cost: ").color(NamedTextColor.GRAY).append(Component.text("$price ${currency.name.split("_")[0].lowercase(
                Locale.getDefault()
            ).replaceFirstChar { it.uppercase() }}").color(NamedTextColor.WHITE).decorate()),
                Component.text(""),
                Component.text("Click to purchase").color(NamedTextColor.YELLOW).decorate()
            ),
            true,
            item.enchantments,
            eventKey = "bw.shop.armorItem",
            invClickHandler = { event: InventoryClickEvent ->
                event.isCancelled = true


                val isUpgrade: Boolean = mutableListOf("LEATHER", "CHAINMAIL", "GOLDEN", "IRON", "DIAMOND").let {
                    val current = it.indexOf(event.whoClicked.inventory.armorContents[0]?.type?.name?.split("_")?.getOrNull(0) ?: "LEATHER")
                    val toBuy = it.indexOf(item.type.name.split("_").getOrNull(0))
                    current < toBuy
                }

                if (!isUpgrade) {
                    event.whoClicked.sendMessage(BedWars.prefix.append(Component.text("You already have this or a better armor!").color(NamedTextColor.RED)))
                    return@ItemBuilder
                }

                // return if player can not pay
                if (!removeCurrencyFromPlayer(event.whoClicked as? Player ?: return@ItemBuilder, currency, price)) {
                    event.whoClicked.sendMessage(BedWars.prefix.append(Component.text("You cannot afford this item!").color(NamedTextColor.RED)))
                    return@ItemBuilder
                }

                val materialStart = item.type.name.split("_").first()
                fun getMat(name: String) = Material.entries.find { it.name == "${materialStart}_${name}" } ?: Material.AIR
                val armor = mutableListOf(
                    ItemBuilder(getMat("BOOTS"), Component.text("Boots").decorate(), invClickHandler = { it.isCancelled = true }).build(),
                    ItemBuilder(getMat("LEGGINGS"), Component.text("Leggings").decorate(), invClickHandler = { it.isCancelled = true }).build(),
                    ItemBuilder(getMat("CHESTPLATE"), Component.text("Chestplate").decorate(), invClickHandler = { it.isCancelled = true }).build(),
                    ItemBuilder(getMat("HELMET"), Component.text("Helmet").decorate(), invClickHandler = { it.isCancelled = true }).build()
                ).toTypedArray()

                event.whoClicked.inventory.armorContents = armor
            },
            itemMeta = item.itemMeta
        ).build()
    }

    fun getPage(player: Player, page: Int): Inventory? {
        val team: Team = player.asGamePlayer.team ?: return null

        // create pages
        val pages: MutableList<Inventory> = mutableListOf<Inventory>().apply {
            for (i in 1..6) add(Bukkit.createInventory(null, 9 * 5, Component.text("Shop")))
        }

        // fill pages with tab items
        fun tabItem(material: Material, name: String, page: Int) = ItemBuilder(
            material,
            Component.text(name).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD),
            invClickHandler = {
                it.isCancelled = true
                it.whoClicked.openInventory(getPage(it.whoClicked as? Player ?: return@ItemBuilder, page) ?: return@ItemBuilder)
            }
        ).build()
        pages.forEach {
            it.setItem(2, tabItem(team.generalBlock, "Blocks", 0))
            it.setItem(3, tabItem(Material.STONE_AXE, "Tools & Weapons", 1))
            it.setItem(4, tabItem(Material.CHAINMAIL_CHESTPLATE, "Armor", 2))
            it.setItem(5, tabItem(Material.POTION, "Potions", 3))
            it.setItem(6, tabItem(Material.NETHER_STAR, "Special", 4))
        }

        val items = Items(team)

        // page 1
        mutableMapOf<ItemStack, Int>(
            buyableItem(items.BLOCKS_PRIMARY, Material.BRICK, 8, 16) to 18,
            buyableItem(items.BLOCKS_GLASS, Material.BRICK, 8, 8) to 19,
            buyableItem(items.BLOCKS_WOOL, Material.BRICK, 6, 8) to 20,
            buyableItem(items.BLOCKS_WOOD, Material.BRICK, 24, 8) to 21,
            buyableItem(items.BLOCKS_ENDSTONE, Material.BRICK, 32, 4) to 22,
            buyableItem(items.BLOCKS_IRON, Material.IRON_INGOT, 8, 2) to 23,
            buyableItem(items.BLOCKS_OBSIDIAN, Material.GOLD_INGOT, 1, 1) to 24,
            buyableItem(items.BLOCKS_LADDER, Material.BRICK, 4, 4) to 25,
            buyableItem(items.BLOCKS_INSTANT_BRIDGE, Material.IRON_INGOT, 4, 1) to 27,
            buyableItem(items.BLOCKS_INSTANT_STAIRS, Material.IRON_INGOT, 4, 1) to 28,
        ).forEach { (t, u) -> pages[0].setItem(u, t) }

        // page 2
        mutableMapOf<ItemStack, Int>(
            buyableItem(items.TOOLS_SWORD_LVL1, Material.BRICK, 4) to 18,
            buyableItem(items.TOOLS_SWORD_LVL2, Material.IRON_INGOT, 10) to 19,
            buyableItem(items.TOOLS_SWORD_LVL3, Material.GOLD_INGOT, 6) to 20,
            buyableItem(items.TOOLS_PICKAXE_LVL1, Material.BRICK, 12) to 27,
            buyableItem(items.TOOLS_PICKAXE_LVL2, Material.IRON_INGOT, 3) to 28,
            buyableItem(items.TOOLS_PICKAXE_LVL3, Material.IRON_INGOT, 6) to 29,
            buyableItem(items.TOOLS_PICKAXE_LVL4, Material.GOLD_INGOT, 4) to 30,
            buyableItem(items.TOOLS_AXE_LVL1, Material.BRICK, 12) to 31,
            buyableItem(items.TOOLS_AXE_LVL2, Material.IRON_INGOT, 2) to 32,
            buyableItem(items.TOOLS_SHEARS, Material.IRON_INGOT, 1) to 36,
        ).forEach { (t, u) -> pages[1].setItem(u, t) }

        // page 3
        mutableMapOf<ItemStack, Int>(
            armorItem(items.ARMOR_LVL1, Material.BRICK, 64) to 19,
            armorItem(items.ARMOR_LVL2, Material.IRON_INGOT, 16) to 20,
            armorItem(items.ARMOR_LVL3, Material.GOLD_INGOT, 16) to 21,
        ).forEach { (t, u) -> pages[2].setItem(u, t) }

        // page 4
        mutableMapOf<ItemStack, Int>(
            buyableItem(items.POTION_JUMP_LVL1, Material.IRON_INGOT, 2) to 18,
            buyableItem(items.POTION_JUMP_LVL2, Material.IRON_INGOT, 6) to 19,
            buyableItem(items.POTION_SPEED_LVL1, Material.IRON_INGOT, 4) to 21,
            buyableItem(items.POTION_SPEED_LVL2, Material.IRON_INGOT, 8) to 22,
            buyableItem(items.POTION_HEAL_LVL1, Material.IRON_INGOT, 2) to 24,
            buyableItem(items.POTION_HEAL_LVL2, Material.IRON_INGOT, 5) to 25,
            buyableItem(items.POTION_REGEN_LVL1, Material.IRON_INGOT, 1) to 26,
            buyableItem(items.POTION_INVIS_LVL1, Material.GOLD_INGOT, 2) to 27,
            buyableItem(items.POTION_INVIS_LVL2, Material.GOLD_INGOT, 3) to 28,
            buyableItem(items.POTION_POISON_LVL1, Material.GOLD_INGOT, 1) to 30,
            buyableItem(items.POTION_POISON_LVL2, Material.GOLD_INGOT, 2) to 31,
            buyableItem(items.POTION_SLOWNESS_LVL1, Material.IRON_INGOT, 8) to 32,
            buyableItem(items.POTION_SLOWNESS_LVL2, Material.IRON_INGOT, 10) to 33,
        ).forEach { (t, u) -> pages[3].setItem(u, t) }

        // page 5
        mutableMapOf<ItemStack, Int>(
            buyableItem(items.SPECIAL_BOOSTER, Material.IRON_INGOT, 6) to 18,
            buyableItem(items.SPECIAL_LAST_CHANCE, Material.GOLD_INGOT, 3) to 19,
            buyableItem(items.SPECIAL_KB_STICK, Material.IRON_INGOT, 6) to 20,
            buyableItem(items.SPECIAL_COBWEB, Material.BRICK, 16) to 21,
            buyableItem(items.SPECIAL_ENDER_PEARL, Material.GOLD_INGOT, 2) to 22,
            buyableItem(items.SPECIAL_TIME_PEARL, Material.GOLD_INGOT, 6) to 23,
            buyableItem(items.SPECIAL_TNT, Material.IRON_INGOT, 2) to 24,
            buyableItem(items.SPECIAL_FIREBALL, Material.BRICK, 64) to 25,
            buyableItem(items.SPECIAL_TEAM_CHEST, Material.IRON_INGOT, 2) to 26,
            buyableItem(items.SPECIAL_PRIVATE_CHEST, Material.IRON_INGOT, 6) to 27,
            buyableItem(items.SPECIAL_BASE_TP, Material.IRON_INGOT, 2) to 28,
            buyableItem(items.SPECIAL_PLAYER_COMPASS, Material.IRON_INGOT, 1) to 29,
            buyableItem(items.SPECIAL_FREEZER, Material.IRON_INGOT, 12) to 30,
            buyableItem(items.SPECIAL_SHOP, Material.GOLD_INGOT, 2) to 31,
        ).forEach { (t, u) -> pages[4].setItem(u, t) }

        // replace all empty slots with filler item
        pages.forEach { it.contents = it.contents.map {
            it ?: ItemBuilder(
                Material.GRAY_STAINED_GLASS_PANE,
                name = Component.text(""),
                invClickHandler = { it.isCancelled = true }
            ).build()
        }.toTypedArray() }

        return pages[page]
    }

    fun openShop(player: Player) {
        player.openInventory(getPage(player, 0) ?: return)
    }
}