package de.c4vxl.bedwars.shop.items

import de.c4vxl.bedwars.BedWars
import de.c4vxl.bedwars.handlers.BlockHandler
import de.c4vxl.bedwars.utils.BlockUtils
import de.c4vxl.bedwars.utils.ItemBuilder
import de.c4vxl.bedwars.utils.TeamData.generalBlock
import de.c4vxl.bedwars.utils.TeamData.glassBlock
import de.c4vxl.bedwars.utils.TeamData.woolBlock
import de.c4vxl.gamemanager.gamemanagementapi.player.GMAPlayer.Companion.asGamePlayer
import de.c4vxl.gamemanager.gamemanagementapi.team.Team
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.entity.Fireball
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionData
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType
import org.bukkit.util.Vector

class Items(private val team: Team) {
    private fun item(material: Material, name: String, suffix: String = "", enchantments: MutableMap<Enchantment, Int> = mutableMapOf()): ItemStack =
        ItemBuilder(material, Component.text("").append(Component.text(name).decorate(TextDecoration.BOLD)).append(Component.text(" $suffix")), unbreakable = true, enchantments = enchantments).build()

    // blocks
    val BLOCKS_PRIMARY = item(team.generalBlock, "Blocks")
    val BLOCKS_WOOL = item(team.woolBlock, "Wool")
    val BLOCKS_GLASS = item(team.glassBlock, "Glass")
    val BLOCKS_ENDSTONE = item(Material.END_STONE, "Endstone")
    val BLOCKS_OBSIDIAN = item(Material.OBSIDIAN, "Obsidian")
    val BLOCKS_WOOD = item(Material.OAK_PLANKS, "Wood")
    val BLOCKS_IRON = item(Material.IRON_BLOCK, "Iron Block")
    val BLOCKS_LADDER = item(Material.LADDER, "Ladder")
    val BLOCKS_INSTANT_BRIDGE = ItemBuilder(
        Material.SANDSTONE_SLAB,
        itemMeta = item(Material.SANDSTONE_SLAB, "Instant bridge", "| Right click").itemMeta,
        interactonHandler = { event: PlayerInteractEvent ->
            if (!event.action.isRightClick) return@ItemBuilder

            event.isCancelled = true

            val player: Player = event.player
            val blockMaterial: Material = player.asGamePlayer.team?.generalBlock ?: Material.SANDSTONE

            // The length of the bridge
            val bridgeLength = 10

            // Get the direction the player is facing
            val direction = player.location.direction
            val blockFace = BlockUtils.getBlockFace(direction)

            // Get the player's current location and adjust to the block they are targeting
            var targetBlock = player.location.subtract(0.0, 1.0, 0.0).block

            // Create the bridge
            for (i in 1..bridgeLength) {
                Bukkit.getScheduler().runTaskLater(BedWars.instance, Runnable {
                    // Move the target block position in the desired direction
                    targetBlock = targetBlock.getRelative(blockFace)

                    // Place the block if the space is air or water (or other passable block)
                    if (targetBlock.type.isAir || targetBlock.isLiquid) {
                        BlockHandler.addBlock(player.asGamePlayer.game ?: return@Runnable, targetBlock)
                        targetBlock.type = blockMaterial
                    }
                }, (i * 2).toLong())
            }

            // remove 1 item
            event.item!!.amount -= 1
        }).build()
    val BLOCKS_INSTANT_STAIRS = ItemBuilder(
        Material.SANDSTONE_STAIRS,
        itemMeta = item(Material.SANDSTONE_STAIRS, "Instant stairs", "| Right click").itemMeta,
        interactonHandler = { event: PlayerInteractEvent ->
            if (!event.action.isRightClick) return@ItemBuilder

            event.isCancelled = true

            val player: Player = event.player
            val blockMaterial: Material = player.asGamePlayer.team?.generalBlock ?: Material.SANDSTONE

            // The length of the bridge
            val bridgeLength = 10

            // Get the direction the player is facing
            val direction = player.location.direction
            val blockFace = BlockUtils.getBlockFace(direction)

            // Get the player's current location and adjust to the block they are targeting
            var targetBlock = player.location.subtract(0.0, 1.0, 0.0).block

            // Create the bridge
            for (i in 1..bridgeLength) {
                Bukkit.getScheduler().runTaskLater(BedWars.instance, Runnable {
                    // Move the target block position in the desired direction
                    targetBlock = targetBlock.getRelative(blockFace)

                    // Every 3 blocks, move the bridge one block up
                    if (i % 3 == 0) {
                        targetBlock = targetBlock.getRelative(BlockFace.UP)
                    }

                    if (targetBlock.type.isAir || targetBlock.isLiquid) {
                        BlockHandler.addBlock(player.asGamePlayer.game ?: return@Runnable, targetBlock)
                        targetBlock.type = blockMaterial
                    }
                }, (i * 2).toLong())
            }

            // remove 1 item
            event.item!!.amount -= 1
        }).build()

    // tools and weapons
    val TOOLS_SWORD_LVL1 = item(Material.WOODEN_SWORD, "Sword", "| Level 1")
    val TOOLS_SWORD_LVL2 = item(Material.STONE_SWORD, "Sword", "| Level 2")
    val TOOLS_SWORD_LVL3 = item(Material.IRON_SWORD, "Sword", "| Level 3")
    val TOOLS_PICKAXE_LVL1 = item(Material.WOODEN_PICKAXE, "Pickaxe", "| Level 1")
    val TOOLS_PICKAXE_LVL2 = item(Material.STONE_PICKAXE, "Pickaxe", "| Level 2")
    val TOOLS_PICKAXE_LVL3 = item(Material.IRON_PICKAXE, "Pickaxe", "| Level 3")
    val TOOLS_PICKAXE_LVL4 = item(Material.DIAMOND_PICKAXE, "Pickaxe", "| Level 4")
    val TOOLS_AXE_LVL1 = item(Material.WOODEN_AXE, "Axe", "| Level 1")
    val TOOLS_AXE_LVL2 = item(Material.STONE_AXE, "Axe", "| Level 2")
    val TOOLS_SHEARS = item(Material.SHEARS, "Shears")

    // armor tab
    val ARMOR_LVL1 = item(Material.CHAINMAIL_CHESTPLATE, "Permanent Armor", "| Chainmail")
    val ARMOR_LVL2 = item(Material.IRON_CHESTPLATE, "Permanent Armor", "| Iron")
    val ARMOR_LVL3 = item(Material.DIAMOND_CHESTPLATE, "Permanent Armor", "| Diamond")

    // potions
    private fun getPotion(item: ItemStack, potionEffect: PotionEffectType, duration: Int = 60, amplifier: Int = 1): ItemStack = item.apply {
        val meta = this.itemMeta as PotionMeta
        meta.basePotionData = PotionData(PotionType.WATER)
        meta.addCustomEffect(PotionEffect(potionEffect, duration, amplifier), true)
        this.setItemMeta(meta)
    }
    val POTION_JUMP_LVL1 = getPotion(item(Material.POTION, "Jump", "| Level 1"), PotionEffectType.JUMP, 60, 1)
    val POTION_JUMP_LVL2 = getPotion(item(Material.POTION, "Jump", "| Level 2"), PotionEffectType.JUMP, 60, 2)
    val POTION_SPEED_LVL1 = getPotion(item(Material.POTION, "Speed", "| Level 1"), PotionEffectType.SPEED, 30, 1)
    val POTION_SPEED_LVL2 = getPotion(item(Material.POTION, "Speed", "| Level 2"), PotionEffectType.SPEED, 30, 2)
    val POTION_HEAL_LVL1 = getPotion(item(Material.POTION, "Instant health", "| Level 1"), PotionEffectType.HEALTH_BOOST, 1, 0)
    val POTION_HEAL_LVL2 = getPotion(item(Material.POTION, "Instant health", "| Level 1"), PotionEffectType.HEALTH_BOOST, 1, 1)
    val POTION_REGEN_LVL1 = getPotion(item(Material.POTION, "Regeneration", "| Level 1"), PotionEffectType.REGENERATION, 20, 1)
    val POTION_INVIS_LVL1 = getPotion(item(Material.POTION, "Invisibility", "| 20 Sec"), PotionEffectType.INVISIBILITY, 20, 1)
    val POTION_INVIS_LVL2 = getPotion(item(Material.POTION, "Invisibility", "| 40 Sec"), PotionEffectType.INVISIBILITY, 20, 1)
    val POTION_POISON_LVL1 = getPotion(item(Material.SPLASH_POTION, "Poison", "| Level 1"), PotionEffectType.POISON, 3, 1)
    val POTION_POISON_LVL2 = getPotion(item(Material.SPLASH_POTION, "Poison", "| Level 2"), PotionEffectType.POISON, 3, 2)
    val POTION_SLOWNESS_LVL1 = getPotion(item(Material.SPLASH_POTION, "Slowness", "| Level 1"), PotionEffectType.SLOW, 10, 1)
    val POTION_SLOWNESS_LVL2 = getPotion(item(Material.SPLASH_POTION, "Slowness", "| Level 2"), PotionEffectType.SLOW, 10, 2)

    // special
    val SPECIAL_LAST_CHANCE = ItemBuilder(
        Material.BLAZE_ROD,
        itemMeta = item(Material.BLAZE_ROD, "Last chance", "| Right click").itemMeta,
        interactonHandler = {
            if (!it.action.isRightClick) return@ItemBuilder
            val item = it.item ?: return@ItemBuilder

            if (it.player.getCooldown(item.type) > 0) {
                it.player.sendMessage(BedWars.prefix.append(Component.text("This item has not recovered yet!!").color(
                    NamedTextColor.RED)))
                return@ItemBuilder
            }

            // 16 sec cooldown
            it.player.setCooldown(item.type, 20 * 3)

            val block = it.player.asGamePlayer.team?.glassBlock ?: Material.GLASS

            val start = it.player.location.subtract(0.0, 3.0, 0.0)
            val locations = mutableListOf(
                start,
                start.clone().add(0.0, 0.0, 1.0),
                start.clone().add(0.0, 0.0, 2.0),
                start.clone().add(0.0, 0.0, -1.0),
                start.clone().add(0.0, 0.0, -2.0),
                start.clone().add(1.0, 0.0, 0.0),
                start.clone().add(2.0, 0.0, 0.0),
                start.clone().add(-1.0, 0.0, 0.0),
                start.clone().add(-2.0, 0.0, 0.0),
                start.clone().add(1.0, 0.0, 1.0),
                start.clone().add(1.0, 0.0, -1.0),
                start.clone().add(-1.0, 0.0, 1.0),
                start.clone().add(-1.0, 0.0, -1.0),
            )

            locations.forEach { l ->
                if (l.block.type.isAir || l.block.isLiquid) {
                    l.block.type = block
                    BlockHandler.addBlock(it.player.asGamePlayer.game ?: return@ItemBuilder, l.block)
                }
            }

            item.amount -= 1

            it.isCancelled = true
        }
    ).build()
    val SPECIAL_BOOSTER = ItemBuilder(
        Material.FLINT_AND_STEEL,
        itemMeta = item(Material.FLINT_AND_STEEL, "Booster", "| Right click").itemMeta,
        interactonHandler = {
            if (!it.action.isRightClick) return@ItemBuilder
            val item = it.item ?: return@ItemBuilder

            if (it.player.getCooldown(item.type) > 0) {
                it.player.sendMessage(BedWars.prefix.append(Component.text("Your booster hasn't recovered yet!").color(
                    NamedTextColor.RED)))
                return@ItemBuilder
            }

            // 16 sec cooldown
            it.player.setCooldown(item.type, 20 * 16)

            // boost
            it.player.velocity = it.player.eyeLocation.direction.add(Vector(0.0, 0.3, 0.0)).multiply(1.1)

            it.isCancelled = true
        }
    ).build()
    val SPECIAL_TNT = ItemBuilder(
        Material.TNT,
        itemMeta = item(Material.TNT, "Explosive", "| Right click").itemMeta,
        interactonHandler = {
            if (!it.action.isRightClick) return@ItemBuilder

            val pos = it.clickedBlock ?: return@ItemBuilder
            val item = it.item ?: return@ItemBuilder

            if (it.player.getCooldown(item.type) > 0) return@ItemBuilder

            // 16 sec cooldown
            it.player.setCooldown(item.type, 20 * 16)

            // spawn tnt with 3 seconds timer
            (pos.world.spawnEntity(pos.location.add(0.0, 2.0, 0.0), EntityType.PRIMED_TNT) as TNTPrimed).fuseTicks = 20 * 3

            it.isCancelled = true

            it.item!!.amount -= 1
        }
    ).build()
    val SPECIAL_FIREBALL = ItemBuilder(
        Material.LEGACY_FIREBALL,
        itemMeta = item(Material.LEGACY_FIREBALL, "Fireball", "| Right click").itemMeta,
        interactonHandler = {
            if (!it.action.isRightClick) return@ItemBuilder
            val item = it.item ?: return@ItemBuilder

            if (it.player.getCooldown(item.type) > 0) return@ItemBuilder

            // 5 sec cooldown
            it.player.setCooldown(item.type, 20 * 5)

            // spawn fireball
            val fireball = it.player.location.world.spawnEntity(it.player.eyeLocation.add(it.player.eyeLocation.direction), EntityType.FIREBALL) as Fireball
            fireball.isVisualFire = true
            fireball.yield = 3.0F // explosion radius. Default is 1F

            // boost fireball
            fireball.velocity = it.player.eyeLocation.direction.multiply(2.5)

            it.item!!.amount -= 1

            it.isCancelled = true
        }
    ).build()
    val SPECIAL_ENDER_PEARL = item(Material.ENDER_PEARL, "Ender pearl")
    val SPECIAL_COBWEB = item(Material.COBWEB, "Cobweb")
    val SPECIAL_KB_STICK = item(Material.STICK, "Knockback stick", enchantments = mutableMapOf(Enchantment.KNOCKBACK to 2))
    val SPECIAL_TEAM_CHEST = ItemBuilder(
        Material.CHEST,
        itemMeta = item(Material.CHEST, "Portable Team chest", "| Right click").itemMeta,
        interactonHandler = {
            if (!it.action.isRightClick) return@ItemBuilder

            val item = it.item ?: return@ItemBuilder
            val team: Team = it.player.asGamePlayer.team ?: return@ItemBuilder

            it.player.openInventory(TeamChest.get(team))

            it.isCancelled = true
        }
    ).build()
    val SPECIAL_BASE_TP = ItemBuilder(
        Material.GUNPOWDER,
        itemMeta = item(Material.GUNPOWDER, "Base teleporter", "| Right click").itemMeta,
        interactonHandler = {
            if (!it.action.isRightClick) return@ItemBuilder

            val item = it.item ?: return@ItemBuilder

            BaseTP.start(it.player)

            it.isCancelled = true
            item.amount -= 1
        }
    ).build()
}