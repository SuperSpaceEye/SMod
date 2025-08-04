package net.spaceeye.smod.vEntityExtensions

import net.minecraft.core.Registry
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.vEntityManaging.util.ExtendableVEntity
import org.valkyrienskies.core.api.ships.properties.ShipId
import kotlin.properties.Delegates

class AnyWrenchable(): SModWrenchable {
    lateinit var itemLocation: ResourceLocation
    override var numItems by Delegates.notNull<Int>()

    constructor(item: Item, numItems: Int): this(item.builtInRegistryHolder().key().location(), numItems)
    constructor(location: ResourceLocation, numItems: Int): this() {this.itemLocation = location; this.numItems = numItems}

    override fun getItemStack() = ItemStack(Registry.ITEM.get(itemLocation), numItems)
    override fun onAfterCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>, new: ExtendableVEntity) { new.addExtension(AnyWrenchable(itemLocation, numItems)) }

    override fun onSerialize(): CompoundTag? {
        val tag = super.onSerialize()
        tag?.putString("resourceLocation", itemLocation.toString())
        return tag
    }

    override fun onDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): Boolean {
        itemLocation = ResourceLocation(tag.getString("resourceLocation"))
        return super.onDeserialize(tag, lastDimensionIds)
    }
}