package net.spaceeye.smod.blocks

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.spaceeye.smod.blockentities.VSchematicBuilderBE
import net.spaceeye.smod.blockentities.VSchematicBuilderMenu

class VSchematicBuilder(properties: Properties): BaseEntityBlock(properties) {
    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult? {
        if (level.isClientSide) {
            Minecraft.getInstance().setScreen(VSchematicBuilderMenu(level as ClientLevel, pos))
        }

        return InteractionResult.CONSUME
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState) = VSchematicBuilderBE(pos, state)
    override fun getRenderShape(state: BlockState) = RenderShape.MODEL
}