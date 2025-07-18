package net.spaceeye.smod.schemCompat

import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.compat.schem.SchemCompatItem
import net.spaceeye.vmod.utils.JVector3d
import org.valkyrienskies.core.api.ships.ServerShip

class CreateIMultiBlockEntitySurvivalCompat(): SchemCompatItem {
    override fun onCopy(
        level: ServerLevel,
        pos: BlockPos,
        state: BlockState,
        ships: List<ServerShip>,
        centerPositions: Map<Long, JVector3d>,
        be: BlockEntity?,
        tag: CompoundTag?,
        cancelBlockCopying: () -> Unit
    ) {
        if (be is IMultiBlockEntityContainer && tag != null) {
            tag.remove("Inventory")
            tag.remove("TankContent")
        }
    }

    override fun onPaste(
        level: ServerLevel,
        oldToNewId: Map<Long, Long>,
        centerPositions: Map<Long, Pair<JVector3d, JVector3d>>,
        tag: CompoundTag,
        pos: BlockPos,
        state: BlockState,
        tagTransformer: (((CompoundTag?) -> CompoundTag?)?) -> Unit,
        afterPasteCallbackSetter: ((BlockEntity?) -> Unit) -> Unit
    ) {
        tagTransformer {
            if (level.getBlockEntity(pos) is IMultiBlockEntityContainer) it?.also {
                it.remove("Inventory")
                it.remove("TankContent")
            } else it
        }
    }
}