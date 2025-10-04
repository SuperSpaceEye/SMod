package net.spaceeye.smod

import dev.architectury.registry.registries.DeferredRegister
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.state.BlockBehaviour
import net.spaceeye.smod.SMItems.TAB
import net.spaceeye.smod.blocks.VSchematicBuilder

object SBlocks {
    private val BLOCKS = DeferredRegister.create(SM.MOD_ID, Registries.BLOCK)

    var VSCHEMATIC_BUILDER = BLOCKS.register("vschematic_builder") { VSchematicBuilder(BlockBehaviour.Properties.of().strength(2.0f)) }

    fun register() {BLOCKS.register()}
    fun registerItems(items: DeferredRegister<Item?>) {
        for (block in BLOCKS) {
            items.register(block.id) { BlockItem(block.get(), Item.Properties().`arch$tab`(TAB)) }
        }
    }
}