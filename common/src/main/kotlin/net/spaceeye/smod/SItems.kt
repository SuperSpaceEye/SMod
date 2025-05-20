package net.spaceeye.smod

import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.spaceeye.smod.items.PhysRopeItem
import net.spaceeye.smod.items.RopeItem
import net.spaceeye.smod.items.WrenchItem

object SItems {
    val ITEMS = DeferredRegister.create(SM.MOD_ID, Registry.ITEM_REGISTRY)

    val TAB: CreativeModeTab = CreativeTabRegistry.create(
        ResourceLocation(
            SM.MOD_ID,
            "smod_tab"
        )
    ) { ItemStack(LOGO.get()) }

    var LOGO: RegistrySupplier<Item> = ITEMS.register("smod_logo") { Item(Item.Properties()) }

    var WRENCH = ITEMS.register("wrench") { WrenchItem() }

    var ROPE = ITEMS.register("rope") { RopeItem() }
    var PHYS_ROPE = ITEMS.register("phys_rope") { PhysRopeItem() }

    fun register() {
        ITEMS.register()
    }
}