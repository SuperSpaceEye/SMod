package net.spaceeye.smod

import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.spaceeye.smod.items.ConnectionItem
import net.spaceeye.smod.items.PhysRopeItem
import net.spaceeye.smod.items.RopeItem
import net.spaceeye.smod.items.SchemMakerItem
import net.spaceeye.smod.items.WrenchItem
import net.spaceeye.smod.toolgun.ToolgunItem

object SMItems {
    val ITEMS = DeferredRegister.create(SM.MOD_ID, Registry.ITEM_REGISTRY)

    val TAB: CreativeModeTab = CreativeTabRegistry.create(
        ResourceLocation(
            SM.MOD_ID,
            "smod_tab"
        )
    ) { ItemStack(LOGO.get()) }

    var LOGO: RegistrySupplier<Item> = ITEMS.register("smod_logo") { Item(Item.Properties()) }

    var WRENCH = ITEMS.register("wrench") { WrenchItem() }
    var SCHEM_MAKER = ITEMS.register("schematic_maker") { SchemMakerItem() }
    var SURVIVAL_TOOLGUN = ITEMS.register("survival_toolgun") { ToolgunItem() }

    var ROPE = ITEMS.register("rope") { Item(Item.Properties().tab(TAB).stacksTo(64)) }
    var PHYS_ROPE = ITEMS.register("phys_rope") { Item(Item.Properties().tab(TAB).stacksTo(64)) }
    var CONNECTION_ITEM = ITEMS.register("connection_item") { Item(Item.Properties().tab(TAB).stacksTo(64)) }

    fun register() {
        SBlocks.registerItems(ITEMS)
        ITEMS.register()
    }
}