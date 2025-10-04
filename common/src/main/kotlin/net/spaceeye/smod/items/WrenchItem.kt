package net.spaceeye.smod.items

import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import net.spaceeye.smod.SMItems
import net.spaceeye.smod.vEntityExtensions.SModWrenchable
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.vEntityManaging.getVEntity
import net.spaceeye.vmod.vEntityManaging.getVEntityIdsOfPosition
import net.spaceeye.vmod.vEntityManaging.removeVEntity
import net.spaceeye.vmod.vEntityManaging.util.ExtendableVEntity
import org.valkyrienskies.mod.common.util.toJOMLD

class WrenchItem: Item(Properties().`arch$tab`(SMItems.TAB).stacksTo(1)) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        if (level !is ServerLevel) { return super.useOn(context) }

        val iter = BlockPos.betweenClosed(context.clickedPos.subtract(Vec3i(1, 1, 1)), context.clickedPos.subtract(Vec3i(-1, -1, -1)))
        val cPos = Vector3d(context.clickLocation)

        val ventities = iter
            .mapNotNull { level.getVEntityIdsOfPosition(it) }
            .flatten()
            .toSet()
            .mapNotNull { level.getVEntity(it) as? ExtendableVEntity }
            .filter { it.getExtensionsOfType<SModWrenchable>().isNotEmpty() }
            .filter { it.getAttachmentPoints(-1).any { (it - cPos).dist() <= 0.6 } }
        if (ventities.isEmpty()) {return super.useOn(context)}

        val pos = Vector3d(context.clickedPos) + 0.5 + Vector3d(context.clickedFace.normal.toJOMLD()) * 0.5

        ventities
            .map { it.getExtensionsOfType<SModWrenchable>().map { it.getItemStack() } }
            .flatten()
            .forEach { level.addFreshEntity(ItemEntity(level, pos.x, pos.y, pos.z, it)) }
        ventities.forEach { level.removeVEntity(it) }

        return super.useOn(context)
    }
}