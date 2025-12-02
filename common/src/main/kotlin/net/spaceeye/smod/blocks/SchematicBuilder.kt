package net.spaceeye.smod.blocks

import gg.essential.elementa.components.Window
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.spaceeye.smod.blockentities.VSchematicBuilderBE
import net.spaceeye.smod.blockentities.VSchematicBuilderMenu
import net.spaceeye.smod.blockentities.VSchematicBuilderNetworking

class SchematicBuilder(properties: Properties): BaseEntityBlock(properties) {
    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult? {
        if (hand == InteractionHand.OFF_HAND) return InteractionResult.FAIL
        if (level.isClientSide) {
            VSchematicBuilderNetworking.callbacks[pos] = { Window.enqueueRenderOperation {
                val screen = VSchematicBuilderMenu(level as ClientLevel, pos)
                Minecraft.getInstance().setScreen(screen)
                screen.makeGUI(it)
            } }
            VSchematicBuilderNetworking.getSchemStream.r2tRequestData.transmitData(VSchematicBuilderNetworking.SendSchemRequest(pos))
        }
        return InteractionResult.SUCCESS
    }

    override fun neighborChanged(
        blockState: BlockState,
        level: Level,
        blockPos: BlockPos,
        block: Block,
        blockPos2: BlockPos,
        bl: Boolean
    ) {
        val level = level as? ServerLevel ?: return
        val signal = level.getBestNeighborSignal(blockPos)
        if (signal == 0) {return}

        val be = level.getBlockEntity(blockPos) as? VSchematicBuilderBE ?: return
        be.buildSchematic(null)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState) = VSchematicBuilderBE(pos, state)
    override fun getRenderShape(state: BlockState) = RenderShape.MODEL
}