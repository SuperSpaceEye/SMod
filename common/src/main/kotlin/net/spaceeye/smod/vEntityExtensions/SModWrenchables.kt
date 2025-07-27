package net.spaceeye.smod.vEntityExtensions

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.spaceeye.smod.SMItems
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.vEntityManaging.VEExtensionTypes
import net.spaceeye.vmod.vEntityManaging.util.ExtendableVEntity
import org.valkyrienskies.core.api.ships.properties.ShipId
import kotlin.reflect.full.primaryConstructor

abstract class BaseWrenchable(var item: Item, override var numItems: Int): SModWrenchable {
    abstract fun constructor(): BaseWrenchable
    override fun getItemStack(): ItemStack = ItemStack(item, numItems)
    override fun onAfterCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>, new: ExtendableVEntity) { new.addExtension(constructor().also { it.numItems = numItems }) }
}

class SModRopeWrenchable(numItems: Int = 0): BaseWrenchable(SMItems.ROPE.get(), numItems) { override fun constructor() = this::class.primaryConstructor!!.call() }
class SModPhysRopeWrenchable(numItems: Int = 0): BaseWrenchable(SMItems.PHYS_ROPE.get(), numItems) { override fun constructor() = this::class.primaryConstructor!!.call() }
class SModConnectionWrenchable(numItems: Int = 0): BaseWrenchable(SMItems.CONNECTION_ITEM.get(), numItems) { override fun constructor() = this::class.primaryConstructor!!.call() }

object SModWrenchableExtensions {
    init { with(VEExtensionTypes) {
        register(SModRopeWrenchable::class)
        register(SModPhysRopeWrenchable::class)
        register(SModConnectionWrenchable::class)
    } }
}