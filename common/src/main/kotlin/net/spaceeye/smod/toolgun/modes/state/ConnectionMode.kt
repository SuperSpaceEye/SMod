package net.spaceeye.smod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.spaceeye.smod.SM
import net.spaceeye.smod.SMItems
import net.spaceeye.smod.toolgun.SModToolgunModes
import net.spaceeye.smod.toolgun.modes.gui.ConnectionGUI
import net.spaceeye.smod.toolgun.modes.hud.ConnectionHUD
import net.spaceeye.smod.vEntityExtensions.SModConnectionWrenchable
import net.spaceeye.smod.vEntityExtensions.SModRopeWrenchable
import net.spaceeye.vmod.vEntityManaging.extensions.RenderableExtension
import net.spaceeye.vmod.vEntityManaging.extensions.Strippable
import net.spaceeye.vmod.vEntityManaging.makeVEntity
import net.spaceeye.vmod.vEntityManaging.types.constraints.ConnectionConstraint
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.rendering.types.A2BRenderer
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.toolgun.gui.Presettable
import net.spaceeye.vmod.toolgun.gui.Presettable.Companion.presettable
import net.spaceeye.vmod.toolgun.modes.*
import net.spaceeye.vmod.toolgun.modes.extensions.*
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.serverRaycast2PointsFnActivation
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.transformDirectionWorldToShip
import org.joml.Quaterniond
import java.awt.Color
import kotlin.let
import kotlin.math.roundToInt

fun BaseMode.survivalCheck(
    player: ServerPlayer,
    requiredItem: Item,
    cost: Int,
    fnToActivate: () -> Unit
) {
    val inventory = player.inventory
    val positions = mutableListOf<Int>()

    var remaining = cost
    for ((i, stack) in inventory.items.withIndex()) {
        if (stack.item != requiredItem) continue
        positions.add(i)
        remaining -= stack.count
        if (remaining <= 0) break
    }
    if (remaining > 0) return
    remaining = cost

    for (pos in positions) {
        val stack = inventory.getItem(pos)
        if (stack.count <= remaining) {
            remaining -= stack.count
            inventory.setItem(pos, ItemStack.EMPTY)
            continue
        }
        stack.count -= remaining
        inventory.setItem(pos, stack)
    }

    fnToActivate()
}

class ConnectionMode: ExtendableToolgunMode(), ConnectionGUI, ConnectionHUD {
    @JsonIgnore private var i = 0

    var maxForce: Float by get(i++, -1f) { ServerLimits.instance.maxForce.get(it) }.presettable()
    var stiffness: Float by get(i++, -1f) { ServerLimits.instance.stiffness.get(it) }.presettable()
    var damping: Float by get(i++, -1f) { ServerLimits.instance.damping.get(it) }.presettable()

    var width: Double by get(i++, .2).presettable()
    var color: Color by get(i++, Color(62, 62, 62, 255)).presettable()
    var fullbright: Boolean by get(i++, false).presettable()

    var fixedDistance: Float by get(i++, -1.0f) { ServerLimits.instance.fixedDistance.get(it) }.presettable()
    var connectionMode: ConnectionConstraint.ConnectionModes by get(i++, ConnectionConstraint.ConnectionModes.FIXED_ORIENTATION).presettable()
    var primaryFirstRaycast: Boolean by get(i++, false)


    val posMode: PositionModes get() = getExtensionOfType<PlacementModesExtension>().posMode
    val precisePlacementAssistSideNum: Int get() = getExtensionOfType<PlacementModesExtension>().precisePlacementAssistSideNum
    val paMiddleFirstRaycast: Boolean get() = false //getExtensionOfType<PlacementAssistExtension>().middleFirstRaycast

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult)
    = serverRaycast2PointsFnActivation(posMode, precisePlacementAssistSideNum, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, sPos1, sPos2, rPos1, rPos2, pr, rr ->
        val wDir = (rPos2 - rPos1).normalize()
        val distance = if (fixedDistance < 0) {(rPos2 - rPos1).dist().toFloat()} else {fixedDistance}

        val costPerUnit = 1.0
        val cost = (distance * costPerUnit).roundToInt().let { if (distance * costPerUnit > it) it + 1 else it }
    survivalCheck(player, SMItems.CONNECTION_ITEM.get(), cost) {
        level.makeVEntity(ConnectionConstraint(
            sPos1, sPos2, //directions get scaled
            ship1?.let { transformDirectionWorldToShip(it, wDir) } ?: wDir.copy(),
            ship2?.let { transformDirectionWorldToShip(it, wDir) } ?: wDir.copy(),
            Quaterniond(ship1?.transform?.shipToWorldRotation ?: Quaterniond()),
            Quaterniond(ship2?.transform?.shipToWorldRotation ?: Quaterniond()),
            shipId1, shipId2, maxForce, stiffness, damping, distance, connectionMode
        ).addExtension(RenderableExtension(A2BRenderer(
            ship1?.id ?: -1L,
            ship2?.id ?: -1L,
            sPos1, sPos2,
            color, width, fullbright, RenderingUtils.whiteTexture))
        ).addExtension(Strippable()
        ).addExtension(SModConnectionWrenchable(cost)
        ))

        resetState()
    } }

    override fun eResetState() {
        previousResult = null
        primaryFirstRaycast = false
    }

    companion object {
        val paNetworkingObj = PlacementAssistNetworking("connection_networking", SM.MOD_ID)
        init {
            //"it" IS THE SAME ON CLIENT BUT ON SERVER IT CREATES NEW INSTANCE OF THE MODE
            SModToolgunModes.registerWrapper(ConnectionMode::class) {
                it.addExtension {
                    BasicConnectionExtension<ConnectionMode>("connection_mode"
                        ,allowResetting = true
                        ,leftFunction       = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                        ,leftClientCallback = { inst -> inst.primaryFirstRaycast = !inst.primaryFirstRaycast; inst.refreshHUD() }
                    )
                }.addExtension {
                    BlockMenuOpeningExtension<ConnectionMode> { inst -> inst.primaryFirstRaycast || inst.paMiddleFirstRaycast }
                }.addExtension {
                    PlacementModesExtension(true)
//                    PlacementAssistExtension(true, paNetworkingObj,
//                        { (it as ConnectionMode).primaryFirstRaycast },
//                        { (it as ConnectionMode).connectionMode == ConnectionConstraint.ConnectionModes.HINGE_ORIENTATION },
//                        { spoint1: Vector3d, spoint2: Vector3d, rpoint1: Vector3d, rpoint2: Vector3d, ship1: ServerShip, ship2: ServerShip?, shipId1: ShipId, shipId2: ShipId, rresults: Pair<RaycastFunctions.RaycastResult, RaycastFunctions.RaycastResult>, paDistanceFromBlock: Double ->
//                            ConnectionConstraint(
//                                spoint1, spoint2, //scale directions manually
//                                 rresults.first .globalNormalDirection!! / (ship1?.transform?.scaling?.x() ?: 1.0),
//                                -rresults.second.globalNormalDirection!! / (ship2?.transform?.scaling?.x() ?: 1.0),
//                                Quaterniond(ship1?.transform?.shipToWorldRotation ?: Quaterniond()),
//                                Quaterniond(ship2?.transform?.shipToWorldRotation ?: Quaterniond()),
//                                shipId1, shipId2, it.maxForce, it.stiffness, it.damping, paDistanceFromBlock.toFloat(), it.connectionMode
//                            ).addExtension(Strippable())
//                        }
//                    )
                }.addExtension { Presettable() }
            }
        }
    }
}