package net.spaceeye.smod.items

import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.spaceeye.smod.SItems
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.rendering.RenderingData
import net.spaceeye.vmod.rendering.types.special.PrecisePlacementAssistRenderer
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivateFn
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d

class RopeItem: Item(Properties().tab(SItems.TAB).stacksTo(64)) {
    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack?>? {
        if (level.isClientSide) {return super.use(level, player, usedHand)}
        if (usedHand != InteractionHand.MAIN_HAND) {return super.use(level, player, usedHand)}

        val raycast = RaycastFunctions.raycast(
            level,
            RaycastFunctions.Source(Vector3d(player.lookAngle), Vector3d(player.eyePosition)),
            raycastDistance
        )

        serverRaycastAndActivateFn(PositionModes.PRECISE_PLACEMENT, numPreciseSides, level, raycast) {
            level, shipId, ship, spoint, rpoint, rresult ->

            val stack = player.mainHandItem
            return InteractionResultHolder.consume<ItemStack>(stack)
        }

        return super.use(level, player, usedHand)
    }

    companion object {
        const val raycastDistance = 7.0
        const val numPreciseSides = 7

        var placementAssistRID = -1
        init {
            PersistentEvents.clientOnTick.on { (minecraft), _ ->
                val player = minecraft.player ?: return@on
                if (player.mainHandItem.item !is RopeItem) {
                    if (placementAssistRID != -1) {
                        RenderingData.client.removeClientsideRenderer(placementAssistRID)
                        placementAssistRID = -1
                    }
                    return@on
                }
                if (placementAssistRID != -1) { return@on }
                placementAssistRID = RenderingData.client.addClientsideRenderer(
                    PrecisePlacementAssistRenderer(7, 7.0) { player.mainHandItem.item is RopeItem }
                )
            }
        }
    }
}