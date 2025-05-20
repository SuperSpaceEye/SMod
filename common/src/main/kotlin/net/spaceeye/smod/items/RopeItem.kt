package net.spaceeye.smod.items

import net.minecraft.server.level.ServerLevel
import net.spaceeye.smod.SItems
import net.spaceeye.smod.vEntityExtensions.SModRopeWrenchable
import net.spaceeye.vmod.rendering.types.RopeRenderer
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.extensions.RenderableExtension
import net.spaceeye.vmod.vEntityManaging.types.constraints.RopeConstraint
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import kotlin.math.roundToInt

class RopeItem: TwoPointsItem(SItems.TAB, 64) {
    override fun makeVEntity(level: ServerLevel, shipId1: ShipId, shipId2: ShipId, ship1: ServerShip?, ship2: ServerShip?, sPos1: Vector3d, sPos2: Vector3d, rPos1: Vector3d, rPos2: Vector3d, length: Double, pr: RaycastFunctions.RaycastResult, rr: RaycastFunctions.RaycastResult): VEntity {
        return RopeConstraint(sPos1, sPos2, shipId1, shipId2, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, (rPos1 - rPos2).dist().toFloat())
            .addExtension(RenderableExtension(RopeRenderer(shipId1, shipId2, sPos1, sPos2, length.toDouble(), 1.0/8.0, length.roundToInt(), false)))
            .addExtension(SModRopeWrenchable(length.roundToInt()))
    }
}