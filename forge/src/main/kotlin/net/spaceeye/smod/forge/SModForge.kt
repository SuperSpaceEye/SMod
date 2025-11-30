package net.spaceeye.smod.forge

import dev.architectury.platform.forge.EventBuses
import net.minecraftforge.common.MinecraftForge.EVENT_BUS
import net.minecraftforge.event.AddReloadListenerEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.spaceeye.smod.SM
import net.spaceeye.smod.SM.init
import net.spaceeye.smod.blockentities.AllowedFrameBlocks

@Mod(SM.MOD_ID)
class SModForge {
    init {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(SM.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus())
        EVENT_BUS.addListener(::registerResourceManagers)

        // Run our common setup.
        init()
    }

    private fun registerResourceManagers(event: AddReloadListenerEvent) {
        event.addListener(AllowedFrameBlocks)
    }
}
