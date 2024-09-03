package de.c4vxl.bedwars.utils

import org.bukkit.block.BlockFace
import org.bukkit.util.Vector

object BlockUtils {
    fun getBlockFace(direction: Vector): BlockFace {
        val x = direction.x
        val z = direction.z

        return when {
            x > 0 && Math.abs(x) > Math.abs(z) -> BlockFace.EAST
            x < 0 && Math.abs(x) > Math.abs(z) -> BlockFace.WEST
            z > 0 && Math.abs(z) > Math.abs(x) -> BlockFace.SOUTH
            z < 0 && Math.abs(z) > Math.abs(x) -> BlockFace.NORTH
            else -> BlockFace.NORTH // Fallback, shouldn't happen
        }
    }
}