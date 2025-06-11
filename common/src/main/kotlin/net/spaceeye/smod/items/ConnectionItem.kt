package net.spaceeye.smod.items

import gg.essential.elementa.components.UIContainer
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.spaceeye.smod.SItems
import net.spaceeye.smod.vEntityExtensions.SModRopeWrenchable
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ClientLimits
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.rendering.types.RopeRenderer
import net.spaceeye.vmod.translate.CONNECTION_MODES
import net.spaceeye.vmod.translate.FIXED_ORIENTATION
import net.spaceeye.vmod.translate.FREE_ORIENTATION
import net.spaceeye.vmod.translate.HINGE_ORIENTATION
import net.spaceeye.vmod.translate.WIDTH
import net.spaceeye.vmod.translate.get
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.extensions.RenderableExtension
import net.spaceeye.vmod.vEntityManaging.types.constraints.ConnectionConstraint
import net.spaceeye.vmod.vEntityManaging.types.constraints.ConnectionConstraint.ConnectionModes
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import kotlin.math.roundToInt

class ConnectionItem: TwoPointsItem(SItems.TAB, 64) {
    class Data(): TagAndByteAutoSerializable {
        var width: Double by get(0, 1.0/8.0)
        var mode: ConnectionModes by get(1, ConnectionModes.FIXED_ORIENTATION)
    }
    override fun syncDataConstructor() = Data()

    override val cGhostWidth: Double get() = withSync<Data, Double>(Minecraft.getInstance().player!!.mainHandItem.orCreateTag) { width }

    override fun makeVEntity(data: ItemData, level: ServerLevel, shipId1: ShipId, shipId2: ShipId, ship1: ServerShip?, ship2: ServerShip?, sPos1: Vector3d, sPos2: Vector3d, rPos1: Vector3d, rPos2: Vector3d, sDir1: Vector3d, sDir2: Vector3d, sRot1: Quaterniond, sRot2: Quaterniond, length: Double, pr: RaycastFunctions.RaycastResult, rr: RaycastFunctions.RaycastResult): VEntity
    = with(data.getOrPutSyncData<Data>()) {
        return@with ConnectionConstraint(sPos1, sPos2, sDir1, sDir2, sRot1, sRot2, shipId1, shipId2, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, (rPos1 - rPos2).dist().toFloat(),
            mode)
            .addExtension(RenderableExtension(RopeRenderer(shipId1, shipId2, sPos1, sPos2, length.toDouble(), width, length.roundToInt(), false)))
            .addExtension(SModRopeWrenchable(length.roundToInt()))
    }

    override fun makeGUISettings(parentWindow: UIContainer, data: ItemData): Unit = with(data.getOrPutSyncData<Data>()) {
        makeTextEntry(WIDTH.get(), ::width, 2f, 2f, parentWindow, ClientLimits.instance.ropeRendererWidth)
        makeDropDown(CONNECTION_MODES.get(), parentWindow, 2f, 2f,
            listOf(
                DItem(FIXED_ORIENTATION.get(), mode == ConnectionModes.FIXED_ORIENTATION) {mode = ConnectionModes.FIXED_ORIENTATION},
                DItem(HINGE_ORIENTATION.get(), mode == ConnectionModes.HINGE_ORIENTATION) {mode = ConnectionModes.HINGE_ORIENTATION},
                DItem(FREE_ORIENTATION .get(), mode == ConnectionModes.FREE_ORIENTATION ) {mode = ConnectionModes.FREE_ORIENTATION },
                ))
    }

    override fun getSyncData(): TagAndByteAutoSerializable? = withSync<Data, Data>(Minecraft.getInstance().player!!.mainHandItem.orCreateTag) { this }
    override fun setSyncData(data: FriendlyByteBuf, tag: CompoundTag) = withSync<Data, Unit>(tag) { deserialize(data) }
}