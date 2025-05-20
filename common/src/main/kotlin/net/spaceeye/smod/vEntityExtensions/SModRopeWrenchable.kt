package net.spaceeye.smod.vEntityExtensions

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.spaceeye.smod.SItems
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.vEntityManaging.util.ExtendableVEntity
import org.valkyrienskies.core.api.ships.properties.ShipId

class SModRopeWrenchable(override var numItems: Int): SModWrenchable {
    override fun getItemStack(): ItemStack {
        return ItemStack(SItems.ROPE.get(), numItems)
    }

    override fun onAfterCopyVEntity(
        level: ServerLevel,
        mapped: Map<ShipId, ShipId>,
        centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>,
        new: ExtendableVEntity
    ) {
        new.addExtension(SModRopeWrenchable(numItems))
    }
}