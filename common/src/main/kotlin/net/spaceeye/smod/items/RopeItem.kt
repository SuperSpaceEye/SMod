package net.spaceeye.smod.items

import net.minecraft.client.Minecraft
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.spaceeye.smod.SItems
import net.spaceeye.smod.renderers.clientOnly.GhostBarRenderer
import net.spaceeye.smod.utils.regS2C
import net.spaceeye.smod.vEntityExtensions.SModRopeWrenchable
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.rendering.RenderingData
import net.spaceeye.vmod.rendering.types.RopeRenderer
import net.spaceeye.vmod.rendering.types.special.PrecisePlacementAssistRenderer
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.serverRaycast2PointsFnActivationBase
import net.spaceeye.vmod.utils.EmptyPacket
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.vEntityManaging.extensions.RenderableExtension
import net.spaceeye.vmod.vEntityManaging.makeVEntity
import net.spaceeye.vmod.vEntityManaging.types.constraints.RopeConstraint
import kotlin.math.roundToInt

class RopeItem: Item(Properties().tab(SItems.TAB).stacksTo(64)) {
    var firstPos: RaycastFunctions.RaycastResult? = null
    var clientPos: Vector3d? = null

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (usedHand != InteractionHand.MAIN_HAND) {return super.use(level, player, usedHand)}

        val raycast = RaycastFunctions.raycast(
            level,
            RaycastFunctions.Source(Vector3d(player.lookAngle), Vector3d(player.eyePosition)),
            raycastDistance
        )

        if (raycast.state.isAir) {
            firstPos = null
            clientPos = null
            return InteractionResultHolder.consume<ItemStack>(player.mainHandItem)
        }

        if (level.isClientSide) {return super.use(level, player, usedHand)}
        level as ServerLevel
        player as ServerPlayer

        return serverRaycast2PointsFnActivationBase(PositionModes.PRECISE_PLACEMENT, numPreciseSides, level, raycast, { nr -> firstPos?.let { Pair(true, it) } ?: let { firstPos = nr; s2cSendPos.sendToClient(player, S2CSendPos(nr.globalHitPos!!)); Pair(false, null) } }, { firstPos = null; s2cResetPos.sendToClient(player, EmptyPacket()) }) {
                level, shipId1, shipId2, ship1, ship2, sPos1, sPos2, rPos1, rPos2, pr, cr ->
            val length = (rPos1 - rPos2).dist().toFloat()

            if (player.mainHandItem.count < length.roundToInt()) {
                firstPos = null
                s2cResetPos.sendToClient(player, EmptyPacket())
                return@serverRaycast2PointsFnActivationBase null
            }

            level.makeVEntity(
                RopeConstraint(sPos1, sPos2, shipId1, shipId2, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, (rPos1 - rPos2).dist().toFloat())
                    .addExtension(RenderableExtension(RopeRenderer(shipId1, shipId2, sPos1, sPos2, length.toDouble(), 1.0/8.0, length.roundToInt(), false)))
                    .addExtension(SModRopeWrenchable(length.roundToInt()))
            )
            firstPos = null
            s2cResetPos.sendToClient(player, EmptyPacket())
            InteractionResultHolder.consume<ItemStack>(player.mainHandItem.also { it.count -= length.roundToInt() })
        } ?: super.use(level, player, usedHand)
    }

    companion object {
        const val raycastDistance = 7.0
        const val numPreciseSides = 7

        var placementAssistRID = -1
        var ghostBarRID = -1

        data class S2CSendPos(var pos: Vector3d): AutoSerializable

        val s2cSendPos = regS2C<S2CSendPos>("send_pos", "rope_item") { pkt ->
            val item = (Minecraft.getInstance().player?.mainHandItem?.item as? RopeItem) ?: return@regS2C
            item.clientPos = pkt.pos
        }
        val s2cResetPos = regS2C<EmptyPacket>("reset_pos", "rope_item") {
            val item = (Minecraft.getInstance().player?.mainHandItem?.item as? RopeItem) ?: return@regS2C
            item.clientPos = null
        }
        init {
            PersistentEvents.clientOnTick.on { (minecraft), _ ->
                val player = minecraft.player ?: return@on
                val item = player.mainHandItem.item as? RopeItem
                if (item == null) {
                    if (placementAssistRID != -1 || ghostBarRID != -1) {
                        RenderingData.client.removeClientsideRenderer(placementAssistRID)
                        RenderingData.client.removeClientsideRenderer(ghostBarRID)
                        placementAssistRID = -1
                        ghostBarRID = -1
                    }
                    return@on
                }
                if (placementAssistRID == -1) {
                    placementAssistRID = RenderingData.client.addClientsideRenderer(
                        PrecisePlacementAssistRenderer(7, 7.0) { true }
                    )
                }
                if (ghostBarRID == -1 && item.firstPos != null) {
                    ghostBarRID = RenderingData.client.addClientsideRenderer(
                        GhostBarRenderer({item.clientPos}, {player.mainHandItem.count.toDouble()})
                    )
                }
            }
        }
    }
}