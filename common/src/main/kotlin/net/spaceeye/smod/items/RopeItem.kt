package net.spaceeye.smod.items

import com.fasterxml.jackson.annotation.JsonIgnore
import gg.essential.elementa.components.UIContainer
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.spaceeye.smod.SMItems
import net.spaceeye.smod.vEntityExtensions.SModRopeWrenchable
import net.spaceeye.vmod.guiElements.CheckBox
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.TextEntry
import net.spaceeye.vmod.guiElements.makeCheckBox
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ClientLimits
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.rendering.types.RopeRenderer
import net.spaceeye.vmod.rendering.types.TubeRopeRenderer
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.translate.CENTERED_IN_BLOCK
import net.spaceeye.vmod.translate.CENTERED_ON_SIDE
import net.spaceeye.vmod.translate.HITPOS_MODES
import net.spaceeye.vmod.translate.NORMAL
import net.spaceeye.vmod.translate.PRECISE_PLACEMENT
import net.spaceeye.vmod.translate.SEGMENTS
import net.spaceeye.vmod.translate.SIDES
import net.spaceeye.vmod.translate.WIDTH
import net.spaceeye.vmod.translate.get
import net.spaceeye.vmod.utils.FakeKProperty
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.extensions.RenderableExtension
import net.spaceeye.vmod.vEntityManaging.types.constraints.RopeConstraint
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import java.awt.Color
import kotlin.math.roundToInt

class RopeItem: TwoPointsItem(SMItems.TAB, 64) {
    class Data(): TagAndByteAutoSerializable {
        @JsonIgnore private var i = 0

        var width: Double by get(i++, 1.0/8.0)
        var sides: Int by get(i++, 4)
        var segments: Int by get(i++, -1)
        var useTubeRenderer: Boolean by get(i++, true)
        var posMode: PositionModes by get(i++, PositionModes.PRECISE_PLACEMENT)
        var lerpBetweenRotations by get(i++, false)
    }
    override fun syncDataConstructor() = Data()

    override val cGhostWidth: Double get() = withSync<Data, Double>(Minecraft.getInstance().player!!.mainHandItem.orCreateTag) { width }
    override val cPosMode: PositionModes get() = withSync<Data, PositionModes>(Minecraft.getInstance().player?.mainHandItem?.orCreateTag ?: CompoundTag()) { posMode }

    override fun makeVEntity(data: ItemData, level: ServerLevel, shipId1: ShipId, shipId2: ShipId, ship1: ServerShip?, ship2: ServerShip?, sPos1: Vector3d, sPos2: Vector3d, rPos1: Vector3d, rPos2: Vector3d, sDir1: Vector3d, sDir2: Vector3d, sRot1: Quaterniond, sRot2: Quaterniond, length: Double, pr: RaycastFunctions.RaycastResult, rr: RaycastFunctions.RaycastResult): VEntity
    = with(data.getOrPutSyncData<Data>()) {
        val sDir1 = pr.globalNormalDirection!!
        val sDir2 = rr.globalNormalDirection!!

        var up1 = if (sDir1.y < 0.01 && sDir1.y > -0.01) { Vector3d(0, 1, 0) } else { Vector3d(1, 0, 0) }
        var up2 = if (sDir2.y < 0.01 && sDir2.y > -0.01) { Vector3d(0, 1, 0) } else { Vector3d(1, 0, 0) }

        var right1 = sDir1.cross(up1)
        var right2 = (-sDir2).cross(up2)

        val segments = if (segments > 0) length.roundToInt().coerceAtMost(segments) else length.roundToInt()

        val onTheOutside = posMode != PositionModes.CENTERED_IN_BLOCK

        return@with RopeConstraint(sPos1, sPos2, shipId1, shipId2, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, (rPos1 - rPos2).dist().toFloat())
            .addExtension(RenderableExtension(
                when(useTubeRenderer) {
                    true  -> TubeRopeRenderer(shipId1, shipId2, sPos1, sPos2, up1, up2, right1, right2, length, Color.WHITE, width, sides, segments, false, lerpBetweenRotations, onTheOutside, RenderingUtils.ropeTexture)
                    false -> RopeRenderer(shipId1, shipId2, sPos1, sPos2, length, width, segments, false, RenderingUtils.ropeTexture)
                }
            ))
            .addExtension(SModRopeWrenchable(length.roundToInt()))
    }

    override fun makeGUISettings(parentWindow: UIContainer, data: ItemData): Unit = with(data.getOrPutSyncData<Data>()) {
        val sidesT: () -> TextEntry
        val lerp: () -> CheckBox

        makeTextEntry(WIDTH.get(), ::width, 2f, 2f, parentWindow, ClientLimits.instance.ropeRendererWidth)
        makeTextEntry(SEGMENTS.get(), ::segments, 2f, 2f, parentWindow)

        makeDropDown(HITPOS_MODES.get(), parentWindow, 2f, 2f, listOf(
            DItem(NORMAL.get(),            posMode == PositionModes.NORMAL)            { posMode = PositionModes.NORMAL            },
            DItem(CENTERED_IN_BLOCK.get(), posMode == PositionModes.CENTERED_IN_BLOCK) { posMode = PositionModes.CENTERED_IN_BLOCK },
            DItem(CENTERED_ON_SIDE.get(),  posMode == PositionModes.CENTERED_ON_SIDE)  { posMode = PositionModes.CENTERED_ON_SIDE  },
            DItem(PRECISE_PLACEMENT.get(), posMode == PositionModes.PRECISE_PLACEMENT) { posMode = PositionModes.PRECISE_PLACEMENT })
        )

        makeCheckBox("Use Tube Renderer", FakeKProperty(
            {useTubeRenderer}, {useTubeRenderer = it; if (it) {sidesT().unhide(); lerp().unhide()} else {sidesT().hide(); lerp().hide()}}
        ), 2f, 2f, parentWindow)

        makeTextEntry(SIDES.get(), ::sides, 2f, 2f, parentWindow).also { sidesT = {it} }
        makeCheckBox("Lerp Between Rotations", ::lerpBetweenRotations, 2f, 2f, parentWindow).also { lerp = {it} }
    }

    override fun getSyncData(): TagAndByteAutoSerializable? = withSync<Data, Data>(Minecraft.getInstance().player!!.mainHandItem.orCreateTag) { this }
    override fun setSyncData(data: FriendlyByteBuf, tag: CompoundTag) = withSync<Data, Unit>(tag) { deserialize(data) }
}