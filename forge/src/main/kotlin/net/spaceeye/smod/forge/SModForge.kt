package net.spaceeye.smod.forge

import dev.architectury.platform.forge.EventBuses
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.spaceeye.smod.SM
import net.spaceeye.smod.SM.init

@Mod(SM.MOD_ID)
class SModForge {
    init {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(SM.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus())

        // Run our common setup.
        init()
    }
}
