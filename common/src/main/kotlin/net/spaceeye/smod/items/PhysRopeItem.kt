package net.spaceeye.smod.items

import net.minecraft.server.level.ServerLevel
import net.spaceeye.smod.SItems
import net.spaceeye.smod.vEntityExtensions.SModPhysRopeWrenchable
import net.spaceeye.vmod.rendering.types.PhysRopeRenderer
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.extensions.PhysRopeRenderable
import net.spaceeye.vmod.vEntityManaging.types.constraints.PhysRopeConstraint
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import java.awt.Color
import kotlin.math.roundToInt

class PhysRopeItem: TwoPointsItem(SItems.TAB, 64) {
    override fun makeVEntity(level: ServerLevel, shipId1: ShipId, shipId2: ShipId, ship1: ServerShip?, ship2: ServerShip?, sPos1: Vector3d, sPos2: Vector3d, rPos1: Vector3d, rPos2: Vector3d, length: Double, pr: RaycastFunctions.RaycastResult, rr: RaycastFunctions.RaycastResult): VEntity {
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

        return PhysRopeConstraint(sPos1, sPos2, sDir1, sDir2, shipId1, shipId2, Float.MAX_VALUE, Float.MAX_VALUE, length.toFloat(), length.roundToInt(), 50.0, 0.5, Math.toRadians(45.0))
            .addExtension(PhysRopeRenderable(PhysRopeRenderer(shipId1, shipId2, sPos1, sPos2, up1, up2, right1, right2, Color(255, 255, 255), length.roundToInt(), false, listOf())))
            .addExtension(SModPhysRopeWrenchable(length.roundToInt()))
    }
}