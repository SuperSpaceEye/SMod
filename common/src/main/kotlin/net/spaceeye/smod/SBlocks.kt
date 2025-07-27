package net.spaceeye.smod

import dev.architectury.registry.registries.DeferredRegister
import net.minecraft.core.Registry
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.Material
import net.spaceeye.smod.SMItems.TAB
import net.spaceeye.smod.blocks.VSchematicBuilder

object SBlocks {
    private val BLOCKS = DeferredRegister.create(SM.MOD_ID, Registry.BLOCK_REGISTRY)

    var VSCHEMATIC_BUILDER = BLOCKS.register("vschematic_builder") { VSchematicBuilder(BlockBehaviour.Properties.of(Material.METAL).strength(2.0f)) }

    fun register() {BLOCKS.register()}
    fun registerItems(items: DeferredRegister<Item?>) {
        for (block in BLOCKS) {
            items.register(block.id) { BlockItem(block.get(), Item.Properties().tab(TAB)) }
        }
    }
}