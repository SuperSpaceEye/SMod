package net.spaceeye.smod.fabric

import net.fabricmc.api.ModInitializer
import net.spaceeye.smod.SM.init

class SModFabric : ModInitializer {
    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.

        init()
    }
}
