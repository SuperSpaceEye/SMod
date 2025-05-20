package net.spaceeye.smod.items

import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.spaceeye.smod.ELOG
import net.spaceeye.smod.gui.GUIBuilder
import net.spaceeye.smod.gui.MainItemSettingsGUIWindow
import net.spaceeye.smod.renderers.clientOnly.GhostBarRenderer
import net.spaceeye.smod.utils.regC2S
import net.spaceeye.smod.utils.regS2C
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.reflectable.constructor
import net.spaceeye.vmod.rendering.RenderingData
import net.spaceeye.vmod.rendering.types.special.PrecisePlacementAssistRenderer
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.serverRaycast2PointsFnActivationBase
import net.spaceeye.vmod.utils.EmptyPacket
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.WrapperPacket
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.makeVEntity
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.roundToInt

//TODO think of a better name
//TODO "Item" is common object, maybe use nbt to save changed data?
abstract class TwoPointsItem(tab: CreativeModeTab, stacksTo: Int): Item(Properties().tab(tab).stacksTo(stacksTo)), GUIBuilder {
    class ItemData() {
        var firstPos: RaycastFunctions.RaycastResult? = null
        var syncData: Serializable? = null

        inline fun <reified T: Serializable> getOrPlaceSyncData(): T {
            return syncData?.let { it as T } ?: T::class.constructor()
        }
    }

    private var clientPos: Vector3d? = null
    open val cGhostWidth: Double get() = 1.0/8.0

    abstract fun makeVEntity(
        level: ServerLevel,
        shipId1: ShipId,
        shipId2: ShipId,
        ship1: ServerShip?,
        ship2: ServerShip?,
        sPos1: Vector3d,
        sPos2: Vector3d,
        rPos1: Vector3d,
        rPos2: Vector3d,
        length: Double,
        pr: RaycastFunctions.RaycastResult,
        rr: RaycastFunctions.RaycastResult
    ): VEntity

    abstract fun getSyncData(): Serializable?
    abstract fun setSyncData(data: FriendlyByteBuf, player: ServerPlayer)

    private fun openGUI() {
        Minecraft.getInstance().setScreen(MainItemSettingsGUIWindow(this))
    }

    @OptIn(ExperimentalContracts::class)
    inline fun <R> withData(player: Player, fn: ItemData.() -> R): R {
        contract {
            callsInPlace(fn, InvocationKind.EXACTLY_ONCE)
        }
        return sPlayersItemData.getOrPut(player) { ItemData() }.fn()
    }

    inline fun <reified T: Serializable, R> withSync(player: Player, fn: T.() -> R): R = withData(player) {
        return getOrPlaceSyncData<T>().fn()
    }

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> = withData(player) {
        if (usedHand != InteractionHand.MAIN_HAND) {return super.use(level, player, usedHand)}

        val raycast = RaycastFunctions.raycast(
            level,
            RaycastFunctions.Source(Vector3d(player.lookAngle), Vector3d(player.eyePosition)),
            raycastDistance
        )

        if (level.isClientSide && raycast.state.isAir && player.isShiftKeyDown) {
            openGUI()
        }

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
            val length = (rPos1 - rPos2).dist()

            if (player.mainHandItem.count < length.roundToInt()) {
                firstPos = null
                s2cResetPos.sendToClient(player, EmptyPacket())
                return@serverRaycast2PointsFnActivationBase null
            }

            level.makeVEntity(makeVEntity(level, shipId1, shipId2, ship1, ship2, sPos1, sPos2, rPos1, rPos2, length, pr, cr))
            firstPos = null
            s2cResetPos.sendToClient(player, EmptyPacket())
            InteractionResultHolder.consume<ItemStack>(player.mainHandItem.also { it.count -= length.roundToInt() })
        } ?: super.use(level, player, usedHand)
    }

    companion object {
        const val raycastDistance = 7.0
        const val numPreciseSides = 7

        var cPlacementAssistRID = -1
        var cGhostBarRID = -1
        var cLastItemStack: ItemStack? = null

        var sPlayersItemData = mutableMapOf<Player, ItemData>()

        data class S2CSendPos(var pos: Vector3d): AutoSerializable

        val s2cSendPos = regS2C<S2CSendPos>("send_pos", "two_points_item") { pkt ->
            val item = (Minecraft.getInstance().player?.mainHandItem?.item as? TwoPointsItem) ?: return@regS2C
            item.clientPos = pkt.pos
        }
        val s2cResetPos = regS2C<EmptyPacket>("reset_pos", "two_points_item") {
            val item = (Minecraft.getInstance().player?.mainHandItem?.item as? TwoPointsItem) ?: return@regS2C
            item.clientPos = null
        }
        val c2sSyncItem = regC2S<WrapperPacket>("sync_item_data", "two_points_item") { pkt, player ->
            val item = (player.mainHandItem?.item as? TwoPointsItem) ?: return@regC2S
            try {
                item.setSyncData(pkt.buf, player)
            } catch (e: Exception) {
                ELOG(e.stackTraceToString())
            }
        }
        init {
            PersistentEvents.clientOnTick.on { (minecraft), _ ->
                val player = minecraft.player ?: return@on
                val item = player.mainHandItem.item as? TwoPointsItem
                if (item == null || player.mainHandItem != cLastItemStack) {
                    if (cPlacementAssistRID != -1 || cGhostBarRID != -1) {
                        RenderingData.client.removeClientsideRenderer(cPlacementAssistRID)
                        RenderingData.client.removeClientsideRenderer(cGhostBarRID)
                        cPlacementAssistRID = -1
                        cGhostBarRID = -1
                    }
                    cLastItemStack = player.mainHandItem
                    return@on
                }
                if (cPlacementAssistRID == -1) {
                    cPlacementAssistRID = RenderingData.client.addClientsideRenderer(
                        PrecisePlacementAssistRenderer(numPreciseSides, raycastDistance) { true }
                    )
                }
                if (cGhostBarRID == -1 && item.clientPos != null) {
                    cGhostBarRID = RenderingData.client.addClientsideRenderer(
                        GhostBarRenderer({item.clientPos}, {player.mainHandItem.count.toDouble()}, {item.cGhostWidth}, raycastDistance, numPreciseSides)
                    )
                }
            }

            PersistentEvents.keyPress.on {
                (keyCode, scanCode, action, modifiers), _ ->
                if (action != GLFW.GLFW_PRESS) {return@on false}
                if (Minecraft.getInstance().screen !is MainItemSettingsGUIWindow) {return@on false}
                if (!ClientToolGunState.TOOLGUN_TOGGLE_HUD_KEY.matches(keyCode, scanCode) && keyCode != GLFW.GLFW_KEY_ESCAPE) { return@on false }

                Minecraft.getInstance().setScreen(null)

                val item = Minecraft.getInstance().player?.mainHandItem?.item as? TwoPointsItem ?: return@on true
                item.getSyncData()?.also { c2sSyncItem.sendToServer(WrapperPacket(it)) }

                return@on true
            }
        }
    }
}