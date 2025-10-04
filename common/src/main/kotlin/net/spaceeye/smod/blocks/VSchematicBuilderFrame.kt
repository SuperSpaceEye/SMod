package net.spaceeye.smod.blocks

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState

class VSchematicBuilderFrame(properties: Properties): Block(properties) {
    override fun getRenderShape(blockState: BlockState) = RenderShape.MODEL
}