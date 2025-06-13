package net.spaceeye.smod.blockentities

import com.google.common.io.Files
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.smod.ELOG
import net.spaceeye.smod.SBlockEntities
import net.spaceeye.smod.SM
import net.spaceeye.smod.utils.regC2S
import net.spaceeye.valkyrien_ship_schematics.interfaces.IShipSchematic
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipSchematicDataV1
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.guiElements.Button
import net.spaceeye.vmod.guiElements.makeText
import net.spaceeye.vmod.networking.DataStream
import net.spaceeye.vmod.networking.FakePacketContext
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.reflectable.constructor
import net.spaceeye.vmod.schematic.VModShipSchematicV2
import net.spaceeye.vmod.schematic.placeAt
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.state.PlayerSchematics
import net.spaceeye.vmod.translate.LOAD
import net.spaceeye.vmod.translate.get
import net.spaceeye.vmod.utils.Either
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.onServerTick
import org.joml.Quaterniond
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.util.toMinecraft
import java.awt.Color
import java.util.Locale
import java.util.UUID
import kotlin.collections.component1
import kotlin.collections.component2

const val PLAYER_REACH = 7.0

object VSchematicBuilderNetworking {
    var getSchemStream = object : DataStream<SendSchemRequest, SchemHolder>(
        "get_schem_stream",
        NetworkManager.Side.S2C,
        SM.MOD_ID,
        transmitterWrapper = ::onServerTick
    ) {
        override val partByteAmount: Int get() = VMConfig.SERVER.TOOLGUN.SCHEMATIC_PACKET_PART_SIZE
        override fun requestPacketConstructor(buf: FriendlyByteBuf) = SendSchemRequest::class.constructor(buf)
        override fun dataPacketConstructor() = SchemHolder()
        override fun receiverDataTransmissionFailed(failurePkt: RequestFailurePkt) {  }

        override fun uuidHasAccess(uuid: UUID, ctx: NetworkManager.PacketContext, req: SendSchemRequest) =
            ctx.player.position().closerThan(req.pos.toJOMLD().add(0.5, 0.5, 0.5).toMinecraft(), PLAYER_REACH)

        override fun transmitterRequestProcessor(req: SendSchemRequest, ctx: NetworkManager.PacketContext): Either<SchemHolder, RequestFailurePkt>? {
            val player = ctx.player as ServerPlayer
            val level = player.level as ServerLevel
            if (!player.position().closerThan(req.pos.toJOMLD().add(0.5, 0.5, 0.5).toMinecraft(), PLAYER_REACH)) {
                return Either.Right(RequestFailurePkt())
            }

            return Either.Left(SchemHolder(
                (level.getBlockEntity(req.pos) as? VSchematicBuilderBE)?.schematic?.serialize()?.serialize(),
                req.pos
            ))
        }

        override fun receiverDataTransmitted(uuid: UUID, data: SchemHolder, ctx: NetworkManager.PacketContext) {
            val be = Minecraft.getInstance().level?.getBlockEntity(data.pos) as? VSchematicBuilderBE
            val schematic = data.data?.let { buf -> VModShipSchematicV2().also{it.deserialize(buf)} }
            be?.schematic = schematic
            callbacks.remove(data.pos)?.invoke(schematic)
        }
    }

    var loadSchemStream = object : DataStream<SendSchemRequest, SchemHolder>(
        "load_schem_stream",
        NetworkManager.Side.C2S,
        SM.MOD_ID,
        receiverWrapper = ::onServerTick
    ) {
        override val partByteAmount: Int get() = VMConfig.CLIENT.TOOLGUN.SCHEMATIC_PACKET_PART_SIZE
        override fun requestPacketConstructor(buf: FriendlyByteBuf) = SendSchemRequest::class.constructor(buf)
        override fun dataPacketConstructor() = SchemHolder()
        override fun receiverDataTransmissionFailed(failurePkt: RequestFailurePkt) {  }

        override fun transmitterRequestProcessor(req: SendSchemRequest, ctx: NetworkManager.PacketContext): Either<SchemHolder, RequestFailurePkt>? {
            val be = Minecraft.getInstance().level?.getBlockEntity(req.pos) as? VSchematicBuilderBE
            val res = be?.schematic?.let { SchemHolder(it.serialize().serialize(), req.pos) }
            return if (res != null) { Either.Left(res) } else { Either.Right(RequestFailurePkt()) }
        }

        override fun receiverDataTransmitted(uuid: UUID, data: SchemHolder, ctx: NetworkManager.PacketContext) = onServerTick {
            val player = ctx.player as ServerPlayer
            val level = player.level as ServerLevel
            if (!ctx.player.position().closerThan(data.pos.toJOMLD().add(0.5, 0.5, 0.5).toMinecraft(), PLAYER_REACH)) return@onServerTick

            (level.getBlockEntity(data.pos) as? VSchematicBuilderBE)?.schematic = data.data?.let { buf -> VModShipSchematicV2().also { it.deserialize(buf) } }
        }
    }

    val c2sBeginLoadSchematic = regC2S<SendSchemRequest>("begin_load_schematic", "vschematic_builder_networking",
        {pkt, player -> player.position().toJOML().distance(pkt.pos.toJOMLD()) <= PLAYER_REACH}
    ) { pkt, player -> loadSchemStream.r2tRequestData.transmitData(pkt, FakePacketContext(player)) }

    val c2sBuildSchematic = regC2S<SendSchemRequest>("build_schematic", "vschematic_builder_networking",
        {pkt, player -> player.position().toJOML().distance(pkt.pos.toJOMLD()) <= PLAYER_REACH}) {
        pkt, player ->
        onServerTick {
            val level = player.level as ServerLevel
            (level.getBlockEntity(pkt.pos) as? VSchematicBuilderBE)?.buildSchematic(player)
        }
    }

    class SchemHolder(): Serializable {
        var data: ByteBuf? = null
        lateinit var pos: BlockPos

        constructor(data: ByteBuf?, pos: BlockPos): this() {
            this.data = data
            this.pos = pos
        }

        override fun serialize(): FriendlyByteBuf {
            val buf = getBuffer()

            buf.writeBlockPos(pos)
            buf.writeBoolean(data != null)
            if (data != null) {
                buf.writeByteArray(data!!.array())
            }

            return buf
        }
        override fun deserialize(buf: FriendlyByteBuf) {
            pos = buf.readBlockPos()
            if (buf.readBoolean()) {
                data = Unpooled.wrappedBuffer(buf.readByteArray())
            }
        }
    }

    data class SendSchemRequest(var pos: BlockPos): AutoSerializable

    val callbacks = mutableMapOf<BlockPos, (schematic: VModShipSchematicV2?) -> Unit>()

    init {
        VSchematicBuilderNetworking
    }
}

class VSchematicBuilderMenu(val level: ClientLevel, val pos: BlockPos): WindowScreen(ElementaVersion.V8) {
    override fun isPauseScreen(): Boolean = false

    val be = level.getBlockEntity(pos) as? VSchematicBuilderBE

    val screen = UIBlock() constrain {
        x = CenterConstraint()
        y = CenterConstraint()

        width = 90.percent
        height = 90.percent
    } childOf window

    val loadingText = makeText("It's loading bruh", Color.BLACK, 2f, 2f, screen)

    val itemsScroll = ScrollComponent().constrain {
        x = 2.pixels
        y = 2.pixels

        width  = 100.percent - 4.pixels
        height = 100.percent - 4.pixels
    } childOf screen

    init {
        VSchematicBuilderNetworking.callbacks[pos] = { Window.enqueueRenderOperation { makeGUI(it) } }
        VSchematicBuilderNetworking.getSchemStream.r2tRequestData.transmitData(VSchematicBuilderNetworking.SendSchemRequest(pos))
    }

    private fun makeSchematicsList() {
        val paths = PlayerSchematics.listSchematics().sortedWith { a, b ->
                       a.toString().lowercase(Locale.getDefault())
            .compareTo(b.toString().lowercase(Locale.getDefault()))}

        for (path in paths) {
            val block = UIBlock().constrain {
                x = 0f.pixels()
                y = SiblingConstraint()

                width = 100.percent()
                height = ChildBasedMaxSizeConstraint() + 2.pixels()
            } childOf itemsScroll

            Button(Color.GRAY.brighter(), LOAD.get()) {
                be!!.schematic = PlayerSchematics.loadSchematic(path) as VModShipSchematicV2
                VSchematicBuilderNetworking.c2sBeginLoadSchematic.sendToServer(VSchematicBuilderNetworking.SendSchemRequest(pos))
                makeGUI(be.schematic)
            }.constrain {
                x = 0.pixels()
                y = 0.pixels()

                width = ChildBasedSizeConstraint() + 4.pixels()
                height = ChildBasedSizeConstraint() + 4.pixels()
            } childOf block

            val name = Files.getNameWithoutExtension(path.fileName.toString())

            UIText(name, false).constrain {
                x = SiblingConstraint() + 2.pixels()
                y = 2.pixels()

                textScale = 1.pixels()
                color = Color.BLACK.toConstraint()
            } childOf block
        }
    }

    private fun makeGUI(schematic: VModShipSchematicV2?) {
        loadingText.hide()
        if (schematic == null) { return makeSchematicsList() }

        itemsScroll.clearChildren()

        val blockMap = VSchematicBuilderBE.schematicToRequirementsMap(schematic)

        Button(Color.GRAY, "Build") {
            VSchematicBuilderNetworking.c2sBuildSchematic.sendToServer(VSchematicBuilderNetworking.SendSchemRequest(pos))
        } constrain {
            x = 2.pixels()
            y = SiblingConstraint(1f) + 1.pixels()

            width = ChildBasedSizeConstraint() + 4.pixels()
            height = ChildBasedSizeConstraint() + 4.pixels()
        } childOf itemsScroll

        val blocks = blockMap.toList().sortedBy { (state, num) -> num }
        blocks.forEach { (state, num) ->
            makeText("${state.toString()} ${num}", Color.BLACK, 2f, 2f, itemsScroll)
        }
    }

    companion object {
        init {
            PersistentEvents.keyPress.on {
                    (keyCode, scanCode, action, modifiers), _ ->
                if (action != GLFW.GLFW_PRESS) {return@on false}
                val screen = Minecraft.getInstance().screen as? VSchematicBuilderMenu ?: return@on false
                if (!ClientToolGunState.TOOLGUN_TOGGLE_HUD_KEY.matches(keyCode, scanCode) && keyCode != GLFW.GLFW_KEY_ESCAPE) { return@on false }

                Minecraft.getInstance().setScreen(null)
                return@on true
            }
        }
    }
}

class VSchematicBuilderBE(pos: BlockPos, state: BlockState): BlockEntity(SBlockEntities.VSCHEMATIC_BUILDER.get(), pos, state) {
    var schematic: VModShipSchematicV2? = null

    fun buildSchematic(player: ServerPlayer) {
        val schematic = schematic ?: return
        val level = level as ServerLevel

        val containers = BlockPos.betweenClosed(blockPos.offset(-1, -1, -1), blockPos.offset(1, 1, 1))
            .mapNotNull { level.getBlockEntity(it) as? Container }
        if (containers.isEmpty()) {return}

        val required = schematicToRequirementsMap(schematic).toMutableMap()
        val verification = required.toMap().toMutableMap()

        containers.forEach {
            for (i in 0 until it.containerSize) {
                val stack = it.getItem(i)

                verification[stack.item]?.also { vn ->
                    val new = vn - stack.count
                    if (new <= 0) {
                        verification.remove(stack.item)
                        return@also
                    }
                    verification[stack.item] = new
                }

                if (verification.isEmpty()) {return@forEach}
            }
        }

        if (verification.isNotEmpty()) {return}

        containers.forEach {
            for (i in 0 until it.containerSize) {
                val stack = it.getItem(i)

                required[stack.item]?.also { vn ->
                    val new = vn - stack.count
                    val remainder = stack.count - vn

                    if (remainder > 0) {
                        val newStack = stack.copy()
                        newStack.count = remainder
                        it.setItem(i, newStack)
                    }

                    if (new <= 0) {
                        required.remove(stack.item)
                        return@also
                    }
                    required[stack.item] = new
                }

                if (required.isEmpty()) {return@forEach}
            }
        }

        val pos = Vector3d(blockPos) + 0.5 + Vector3d(schematic.info!!.maxObjectPos) * Vector3d(0, 1, 0) + Vector3d(0, 3, 0)

        schematic.placeAt(level, player, player.uuid, pos.toJomlVector3d(), Quaterniond()) {
            ELOG("absolute cinema")
        }
    }

    companion object {
        fun schematicToRequirementsMap(schematic: IShipSchematicDataV1): Map<Item, Int> {
            val blockMap = mutableMapOf<Item, Int>()
            val palette = schematic.blockPalette
            schematic.blockData.forEach { (shipId, data) ->
                data.forEach { x, y, z, item ->
                    val state = palette.fromId(item.paletteId)?.block?.asItem() ?: return@forEach

                    blockMap[state] = blockMap.getOrDefault(state, 0) + 1
                }
            }
            return blockMap
        }
    }
}