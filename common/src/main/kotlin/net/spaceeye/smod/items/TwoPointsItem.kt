package net.spaceeye.smod.items

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
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
import net.minecraft.world.level.block.Blocks
import net.spaceeye.smod.ELOG
import net.spaceeye.smod.gui.GUIBuilder
import net.spaceeye.smod.gui.TwoPointsItemSettingsGUIWindow
import net.spaceeye.smod.items.TwoPointsItem.ItemData.Companion.fromTag
import net.spaceeye.smod.renderers.clientOnly.GhostBarRenderer
import net.spaceeye.smod.utils.regC2S
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.reflectable.TagAutoSerializable
import net.spaceeye.vmod.reflectable.constructor
import net.spaceeye.vmod.rendering.RenderingData
import net.spaceeye.vmod.rendering.types.special.PrecisePlacementAssistRenderer
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.serverRaycast2PointsFnActivationBase
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.WrapperPacket
import net.spaceeye.vmod.utils.getMyVector3d
import net.spaceeye.vmod.utils.putMyVector3d
import net.spaceeye.vmod.utils.vs.transformDirectionWorldToShip
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.makeVEntity
import org.joml.Quaterniond
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.roundToInt
import kotlin.reflect.KClass

fun RaycastFunctions.RaycastResult.toTag(): CompoundTag {
    return CompoundTag().also { t ->
        t.putMyVector3d("origin", origin)
        t.putMyVector3d("lookVec", lookVec)
        t.putLong("blockPosition", blockPosition.asLong())
        worldHitPos?.let { t.putMyVector3d("worldHitPos", it) }
        globalHitPos?.let { t.putMyVector3d("globalHitPos", it) }
        worldCenteredHitPos?.let { t.putMyVector3d("worldCenteredHitPos", it) }
        globalCenteredHitPos?.let { t.putMyVector3d("globalCenteredHitPos", it) }
        hitNormal?.let { t.putMyVector3d("hitNormal", it) }
        worldNormalDirection?.let { t.putMyVector3d("worldNormalDirection", it) }
        globalNormalDirection?.let { t.putMyVector3d("globalNormalDirection", it) }
        t.putLong("shipId", shipId)
    }
}

fun CompoundTag.getMyVector3dNullable(prefix: String): Vector3d? {
    return if (
        !contains(prefix + "x") ||
        !contains(prefix + "y") ||
        !contains(prefix + "z")
    ) {
        null
    } else {
        getMyVector3d(prefix)
    }
}

fun raycastResultFromTag(t: CompoundTag): RaycastFunctions.RaycastResult {
    return RaycastFunctions.RaycastResult(
        Blocks.AIR.defaultBlockState(),
        t.getMyVector3d("origin"),
        t.getMyVector3d("lookVec"),
        BlockPos.of(t.getLong("blockPosition")),
        t.getMyVector3dNullable("worldHitPos"),
        t.getMyVector3dNullable("globalHitPos"),
        t.getMyVector3dNullable("worldCenteredHitPos"),
        t.getMyVector3dNullable("globalCenteredHitPos"),
        t.getMyVector3dNullable("hitNormal"),
        t.getMyVector3dNullable("worldNormalDirection"),
        t.getMyVector3dNullable("globalNormalDirection"),
        null,
        t.getLong("shipId")
    )
}

interface TagAndByteAutoSerializable: TagAutoSerializable, AutoSerializable

//TODO think of a better name
abstract class TwoPointsItem(tab: CreativeModeTab, stacksTo: Int): Item(Properties().tab(tab).stacksTo(stacksTo)), GUIBuilder {
    //server sync tag data to client
    class ItemData() {
        var firstPos: RaycastFunctions.RaycastResult? = null
        var syncData: TagAndByteAutoSerializable? = null

        inline fun <reified T: TagAndByteAutoSerializable> getOrPutSyncData(): T {
            return syncData?.let { it as T } ?: let {syncData = (T::class as KClass<Serializable>).constructor() as T; syncData as T }
        }

        fun toTag(tag: CompoundTag) = tag.also { t ->
            if (firstPos != null) t.put("firstPos", firstPos!!.toTag())      else t.remove("firstPos")
            if (syncData != null) t.put("syncData", syncData!!.tSerialize()) else t.remove("syncData")
        }

        companion object {
            fun TagAndByteAutoSerializable.fromTag(tag: CompoundTag) = ItemData().also { i ->
                i.firstPos = tag.get("firstPos")?.let { raycastResultFromTag(it as CompoundTag) }
                i.syncData = tag.get("syncData")?.let { this.also{ i -> i.tDeserialize(it as CompoundTag)} }
            }
        }
    }
    abstract fun syncDataConstructor(): TagAndByteAutoSerializable

    private val clientPos: Vector3d? get() = Minecraft.getInstance().player!!.mainHandItem.orCreateTag.get("firstPos")?.let { it as? CompoundTag }?.getMyVector3dNullable("globalHitPos")
    open val cGhostWidth: Double get() = 1.0/8.0

    abstract fun makeVEntity(data: ItemData, level: ServerLevel, shipId1: ShipId, shipId2: ShipId, ship1: ServerShip?, ship2: ServerShip?, sPos1: Vector3d, sPos2: Vector3d, rPos1: Vector3d, rPos2: Vector3d, sDir1: Vector3d, sDir2: Vector3d, sRot1: Quaterniond, sRot2: Quaterniond, length: Double, pr: RaycastFunctions.RaycastResult, rr: RaycastFunctions.RaycastResult): VEntity

    abstract fun getSyncData(): TagAndByteAutoSerializable?
    abstract fun setSyncData(data: FriendlyByteBuf, tag: CompoundTag)

    private fun openGUI() {
        Minecraft.getInstance().setScreen(TwoPointsItemSettingsGUIWindow(this))
    }

    //creates ItemData from tag -> gives to fn -> serializes changes back to tag
    @OptIn(ExperimentalContracts::class)
    inline fun <R> withData(tag: CompoundTag, crossinline fn: ItemData.() -> R): R {
        contract {
            callsInPlace(fn, InvocationKind.EXACTLY_ONCE)
        }
        return syncDataConstructor().fromTag(tag).let{ item ->
            item.fn().also {
                item.toTag(tag)
            }
        }
    }

    inline fun <reified T: TagAndByteAutoSerializable, R> withSync(tag: CompoundTag, crossinline fn: T.() -> R): R = withData(tag) { getOrPutSyncData<T>().fn() }

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> = withData(player.mainHandItem.orCreateTag) {
        if (usedHand != InteractionHand.MAIN_HAND) { return@withData super.use(level, player, usedHand) }

        val raycast = RaycastFunctions.raycast(
            level,
            RaycastFunctions.Source(Vector3d(player.lookAngle), Vector3d(player.eyePosition)),
            raycastDistance
        )

        //TODO this is dumb. first part is fine, but menu should be opened with a keybind.
        // also another keybind to cycle modes? (connection mode normal/placement assist/join)
        if (raycast.state.isAir) {
            if (player.isShiftKeyDown) {
                firstPos = null
                syncData = null
            } else {
                firstPos = null
            }
            return@withData InteractionResultHolder.consume<ItemStack>(player.mainHandItem)
        } else {
            if (player.isShiftKeyDown) {
                if (level.isClientSide) {
                    openGUI()
                }
                return@withData InteractionResultHolder.consume<ItemStack>(player.mainHandItem)
            }
        }

        if (level.isClientSide) {return@withData super.use(level, player, usedHand)}
        level as ServerLevel
        player as ServerPlayer

        return@withData serverRaycast2PointsFnActivationBase(PositionModes.PRECISE_PLACEMENT, numPreciseSides, level, raycast, {
            nr ->
            firstPos?.let {
                Pair(true, it.also {
                    it.ship = level.shipObjectWorld.allShips.getById(it.shipId)
                })
            } ?: let {
                firstPos = nr
                Pair(false, null)
            } }, { firstPos = null }) {
                level, shipId1, shipId2, ship1, ship2, sPos1, sPos2, rPos1, rPos2, pr, cr ->
            val length = (rPos1 - rPos2).dist()

            if (player.mainHandItem.count < length.roundToInt()) {
                firstPos = null
                return@serverRaycast2PointsFnActivationBase null
            }
            val wDir = (rPos1 - rPos2).normalize()
            val sDir1 = ship1?.let { transformDirectionWorldToShip(it, wDir) } ?: wDir.copy()
            val sDir2 = ship2?.let { transformDirectionWorldToShip(it, wDir) } ?: wDir.copy()

            val sRot1 = Quaterniond(ship1?.transform?.shipToWorldRotation ?: Quaterniond())
            val sRot2 = Quaterniond(ship2?.transform?.shipToWorldRotation ?: Quaterniond())

            level.makeVEntity(makeVEntity(this, level, shipId1, shipId2, ship1, ship2, sPos1, sPos2, rPos1, rPos2, sDir1, sDir2, sRot1, sRot2, length, pr, cr))
            firstPos = null
            InteractionResultHolder.consume<ItemStack>(player.mainHandItem.also { it.count -= length.roundToInt() })
        } ?: super.use(level, player, usedHand)
    }

    companion object {
        const val raycastDistance = 7.0
        const val numPreciseSides = 7

        var cPlacementAssistRID = -1
        var cGhostBarRID = -1
        var cLastItemStack: ItemStack? = null

        val c2sSyncItem = regC2S<WrapperPacket>("sync_item_data", "two_points_item") { pkt, player ->
            val item = (player.mainHandItem?.item as? TwoPointsItem) ?: return@regC2S
            try {
                item.setSyncData(pkt.buf, player.mainHandItem.orCreateTag)
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
                val screen = Minecraft.getInstance().screen as? TwoPointsItemSettingsGUIWindow ?: return@on false
                if (!ClientToolGunState.TOOLGUN_TOGGLE_HUD_KEY.matches(keyCode, scanCode) && keyCode != GLFW.GLFW_KEY_ESCAPE) { return@on false }

                Minecraft.getInstance().setScreen(null)

                val item = Minecraft.getInstance().player?.mainHandItem?.item as? TwoPointsItem ?: return@on true
                screen.data.toTag(Minecraft.getInstance().player?.mainHandItem!!.orCreateTag)
                item.getSyncData()?.also { c2sSyncItem.sendToServer(WrapperPacket(it)) }

                return@on true
            }
        }
    }
}