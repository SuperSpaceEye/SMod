package net.spaceeye.smod.vEntityExtensions

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.spaceeye.vmod.vEntityManaging.util.ExtendableVEntity
import net.spaceeye.vmod.vEntityManaging.util.VEntityExtension
import org.valkyrienskies.core.api.ships.properties.ShipId

//TODO add it in some form to VMod?
interface SModWrenchable: VEntityExtension {
    var numItems: Int
    fun getItemStack(): ItemStack

    override fun onInit(obj: ExtendableVEntity) {}

    override fun onSerialize(): CompoundTag? {
        return CompoundTag().also { it.putInt("numItems", numItems) }
    }

    override fun onDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): Boolean {
        numItems = tag.getInt("numItems")
        return true
    }

    override fun onMakeVEntity(level: ServerLevel) {}
    override fun onDeleteVEntity(level: ServerLevel) {}
}