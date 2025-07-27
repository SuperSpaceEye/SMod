package net.spaceeye.smod.items

import com.fasterxml.jackson.annotation.JsonIgnore
import gg.essential.elementa.components.UIContainer
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.spaceeye.smod.SMItems
import net.spaceeye.smod.vEntityExtensions.SModPhysRopeWrenchable
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.rendering.types.PhysRopeRenderer
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.translate.ANGLE_LIMIT
import net.spaceeye.vmod.translate.CENTERED_ON_SIDE
import net.spaceeye.vmod.translate.HITPOS_MODES
import net.spaceeye.vmod.translate.NORMAL
import net.spaceeye.vmod.translate.PRECISE_PLACEMENT
import net.spaceeye.vmod.translate.RADIUS
import net.spaceeye.vmod.translate.get
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.extensions.PhysRopeRenderable
import net.spaceeye.vmod.vEntityManaging.types.constraints.PhysRopeConstraint
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import java.awt.Color
import kotlin.math.roundToInt

class PhysRopeItem: TwoPointsItem(SMItems.TAB, 64) {
    class Data(): TagAndByteAutoSerializable {
        @JsonIgnore private var i = 0

        var radius: Double by get(i++, 0.5) { ServerLimits.instance.physRopeRadius.get(it) }
        var angleLimit: Double by get(i++, 45.0) { ServerLimits.instance.physRopeAngleLimit.get(it) }
        var posMode: PositionModes by get(i++, PositionModes.PRECISE_PLACEMENT) //todo
    }
    override fun syncDataConstructor() = Data()

    override val cGhostWidth: Double get() = withSync<Data, Double>(Minecraft.getInstance().player!!.mainHandItem.orCreateTag) { radius }
    override val cPosMode: PositionModes get() = withSync<Data, PositionModes>(Minecraft.getInstance().player!!.mainHandItem.orCreateTag) { posMode }

    override fun makeVEntity(data: ItemData, level: ServerLevel, shipId1: ShipId, shipId2: ShipId, ship1: ServerShip?, ship2: ServerShip?, sPos1: Vector3d, sPos2: Vector3d, rPos1: Vector3d, rPos2: Vector3d, sDir1: Vector3d, sDir2: Vector3d, sRot1: Quaterniond, sRot2: Quaterniond, length: Double, pr: RaycastFunctions.RaycastResult, rr: RaycastFunctions.RaycastResult): VEntity
    = with(data.getOrPutSyncData<Data>()) {
        val sDir1 = pr.globalNormalDirection!!
        val sDir2 = rr.globalNormalDirection!!

        var up1 = if (sDir1.y < 0.01 && sDir1.y > -0.01) { Vector3d(0, 1, 0) } else { Vector3d(1, 0, 0) }
        var up2 = if (sDir2.y < 0.01 && sDir2.y > -0.01) { Vector3d(0, 1, 0) } else { Vector3d(1, 0, 0) }

        var right1 = sDir1.cross(up1)
        var right2 = (-sDir2).cross(up2)

        //rendering fuckery
        if (sDir1.y < -0.5) {
            up1 = -up1
            right1 = -right1
        }

        if (sDir2.y > 0.5) {
            up2 = -up2
            right2 = -right2
        }

        return@with PhysRopeConstraint(sPos1, sPos2, sDir1, sDir2, shipId1, shipId2, Float.MAX_VALUE, Float.MAX_VALUE, length.toFloat(), length.roundToInt(), 50.0, radius, Math.toRadians(angleLimit))
            .addExtension(PhysRopeRenderable(PhysRopeRenderer(shipId1, shipId2, sPos1, sPos2, up1, up2, right1, right2, Color(255, 255, 255), length.roundToInt(), false, listOf(), RenderingUtils.ropeTexture)))
            .addExtension(SModPhysRopeWrenchable(length.roundToInt()))
    }

    override fun makeGUISettings(parentWindow: UIContainer, data: ItemData): Unit = with(data.getOrPutSyncData<Data>()) {
        makeTextEntry(RADIUS.get(), ::radius, 2f, 2f, parentWindow, ServerLimits.instance.physRopeRadius)
        makeTextEntry(ANGLE_LIMIT.get(), ::angleLimit, 2f, 2f, parentWindow, ServerLimits.instance.physRopeAngleLimit)

        makeDropDown(HITPOS_MODES.get(), parentWindow, 2f, 2f, listOf(
            DItem(NORMAL.get(),            posMode == PositionModes.NORMAL)            { posMode = PositionModes.NORMAL            },
            DItem(CENTERED_ON_SIDE.get(),  posMode == PositionModes.CENTERED_ON_SIDE)  { posMode = PositionModes.CENTERED_ON_SIDE  },
            DItem(PRECISE_PLACEMENT.get(), posMode == PositionModes.PRECISE_PLACEMENT) { posMode = PositionModes.PRECISE_PLACEMENT })
        )
    }

    override fun getSyncData(): TagAndByteAutoSerializable? = withSync<Data, Data>(Minecraft.getInstance().player!!.mainHandItem.orCreateTag) { this }
    override fun setSyncData(data: FriendlyByteBuf, tag: CompoundTag) = withSync<Data, Unit>(tag) { deserialize(data) }
}