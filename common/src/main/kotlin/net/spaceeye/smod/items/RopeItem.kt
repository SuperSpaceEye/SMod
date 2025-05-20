package net.spaceeye.smod.items

import gg.essential.elementa.components.UIContainer
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.smod.SItems
import net.spaceeye.smod.vEntityExtensions.SModRopeWrenchable
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ClientLimits
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.rendering.types.RopeRenderer
import net.spaceeye.vmod.translate.WIDTH
import net.spaceeye.vmod.translate.get
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.extensions.RenderableExtension
import net.spaceeye.vmod.vEntityManaging.types.constraints.RopeConstraint
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import kotlin.math.roundToInt

class RopeItem: TwoPointsItem(SItems.TAB, 64) {
    class Data(): AutoSerializable {
        var width: Double by get(0, 1.0/8.0)
    }

    override val cGhostWidth: Double get() = withSync<Data, Double>(Minecraft.getInstance().player!!) { width }

    override fun makeVEntity(level: ServerLevel, shipId1: ShipId, shipId2: ShipId, ship1: ServerShip?, ship2: ServerShip?, sPos1: Vector3d, sPos2: Vector3d, rPos1: Vector3d, rPos2: Vector3d, length: Double, pr: RaycastFunctions.RaycastResult, rr: RaycastFunctions.RaycastResult): VEntity
    = withSync<Data, VEntity>(Minecraft.getInstance().player!!) {
        return RopeConstraint(sPos1, sPos2, shipId1, shipId2, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, (rPos1 - rPos2).dist().toFloat())
            .addExtension(RenderableExtension(RopeRenderer(shipId1, shipId2, sPos1, sPos2, length.toDouble(), width, length.roundToInt(), false)))
            .addExtension(SModRopeWrenchable(length.roundToInt()))
    }

    override fun makeGUISettings(parentWindow: UIContainer) = withSync<Data, Unit>(Minecraft.getInstance().player!!) {
        makeTextEntry(WIDTH.get(), ::width, 2f, 2f, parentWindow, ClientLimits.instance.ropeRendererWidth)
    }

    override fun getSyncData(): Serializable? = withData(Minecraft.getInstance().player!!) { syncData }
    override fun setSyncData(data: FriendlyByteBuf, player: ServerPlayer) = withSync<Data, Unit>(player) { deserialize(data) }
}