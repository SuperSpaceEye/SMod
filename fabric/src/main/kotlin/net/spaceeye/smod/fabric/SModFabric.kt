package net.spaceeye.smod.fabric

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller
import net.spaceeye.smod.MOD_ID
import net.spaceeye.smod.SM.init
import net.spaceeye.smod.blockentities.AllowedFrameBlocks
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

class SModFabric : ModInitializer {
    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.

        init()

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
            object : IdentifiableResourceReloadListener {
                override fun getFabricId(): ResourceLocation? = ResourceLocation(MOD_ID, "allowed_frame_blocks")

                override fun reload(
                    preparationBarrier: PreparableReloadListener.PreparationBarrier,
                    resourceManager: ResourceManager,
                    profilerFiller: ProfilerFiller,
                    profilerFiller2: ProfilerFiller,
                    executor: Executor,
                    executor2: Executor
                ): CompletableFuture<Void?>? {
                    return AllowedFrameBlocks.reload(preparationBarrier, resourceManager, profilerFiller, profilerFiller2, executor, executor2)
                }
            }
        )
    }
}
