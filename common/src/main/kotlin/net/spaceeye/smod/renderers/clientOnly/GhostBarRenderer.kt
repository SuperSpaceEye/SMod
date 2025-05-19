package net.spaceeye.smod.renderers.clientOnly

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.smod.items.RopeItem
import net.spaceeye.smod.items.RopeItem.Companion.raycastDistance
import net.spaceeye.vmod.rendering.RenderTypes
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.toolgun.modes.util.calculatePrecise
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.getShipManagingPos
import java.awt.Color

class GhostBarRenderer(
    val pos: () -> Vector3d?,
    val maxDistance: () -> Double
): BaseRenderer() {
    override fun renderData(
        poseStack: PoseStack,
        camera: Camera,
        timestamp: Long
    ) {
        val sPos1 = pos() ?: return

        val level = Minecraft.getInstance().level!!
        val player = Minecraft.getInstance().player!!

        val rr = RaycastFunctions.renderRaycast(
            level,
            RaycastFunctions.Source(Vector3d(player.lookAngle), Vector3d(camera.position)),
            raycastDistance
        )

        val sPos2 = if (rr.state.isAir) {
            Vector3d(player.eyePosition) + Vector3d(player.lookAngle) * raycastDistance
        } else { calculatePrecise(rr, RopeItem.numPreciseSides) }

        val ship1 = level.getShipManagingPos(sPos1.x, sPos1.y, sPos1.z) as? ClientShip
        val ship2 = level.getShipManagingPos(sPos2.x, sPos2.y, sPos2.z) as? ClientShip

        val rPos1 = ship1?.let { posShipToWorldRender(it, sPos1) } ?: sPos1
        val rPos2 = ship2?.let { posShipToWorldRender(it, sPos2) } ?: sPos2

        val color = if ((rPos1 - rPos2).dist() <= maxDistance()) {
            Color(0, 255, 0, 127)
        } else {
            Color(255, 0, 0, 127)
        }

        val tesselator = Tesselator.getInstance()
        val vBuffer = tesselator.builder

        vBuffer.begin(VertexFormat.Mode.QUADS, RenderTypes.setupFullRendering())
        RenderSystem.setShaderTexture(0, RenderingUtils.whiteTexture)
        poseStack.pushPose()

        val tpos1 = rPos1 - Vector3d(camera.position)
        val tpos2 = rPos2 - Vector3d(camera.position)

        RenderingUtils.Quad.makeFlatRectFacingCamera(vBuffer, poseStack.last().pose(),
            color.red, color.green, color.blue, color.alpha, LightTexture.FULL_BRIGHT, 1.0/8.0,
            tpos1, tpos2
        )

        poseStack.popPose()
        tesselator.end()
        RenderTypes.clearFullRendering()
    }

    override fun copy(oldToNew: Map<ShipId, Ship>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): BaseRenderer? = throw AssertionError()
    override fun scaleBy(by: Double) = throw AssertionError()
    override fun serialize(): FriendlyByteBuf = throw AssertionError()
    override fun deserialize(buf: FriendlyByteBuf) = throw AssertionError()
}