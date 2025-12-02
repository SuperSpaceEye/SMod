package net.spaceeye.smod.blockentities

import com.google.common.io.Files
import com.google.gson.Gson
import com.google.gson.JsonElement
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
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.Container
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.smod.ELOG
import net.spaceeye.smod.SBlockEntities
import net.spaceeye.smod.SM
import net.spaceeye.smod.utils.regC2S
import net.spaceeye.smod.schemCompat.SMSchemCompatObj
import net.spaceeye.valkyrien_ship_schematics.containers.CompoundTagSerializable
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipSchematicDataV1
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.events.SessionEvents
import net.spaceeye.vmod.guiElements.Button
import net.spaceeye.vmod.guiElements.makeText
import net.spaceeye.vmod.networking.DataStream
import net.spaceeye.vmod.networking.FakePacketContext
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.reflectable.constructor
import net.spaceeye.vmod.schematic.SchematicActionsQueue.PasteSchematicSettings
import net.spaceeye.vmod.schematic.VModShipSchematicV2
import net.spaceeye.vmod.schematic.placeAt
import net.spaceeye.vmod.toolgun.ToolgunKeybinds
import net.spaceeye.vmod.toolgun.modes.state.PlayerSchematics
import net.spaceeye.vmod.translate.LOAD
import net.spaceeye.vmod.translate.get
import net.spaceeye.vmod.translate.makeFake
import net.spaceeye.vmod.utils.Either
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.Tuple
import net.spaceeye.vmod.utils.Tuple3
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getNow_ms
import org.joml.Quaterniond
import org.joml.Vector3i
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.util.toMinecraft
import java.awt.Color
import java.util.Locale
import java.util.UUID
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.abs
import kotlin.math.min

const val PLAYER_REACH = 7.0

private fun onServerTick(fn: () -> Unit) {
    SessionEvents.serverOnTick.on { _, unsub ->
        fn.invoke()
        unsub.invoke()
    }
}

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
            val level = player.serverLevel()
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
            val res = SchemHolder(be?.schematic?.serialize()?.serialize(), req.pos)
            return Either.Left(res)
        }

        override fun receiverDataTransmitted(uuid: UUID, data: SchemHolder, ctx: NetworkManager.PacketContext) = onServerTick {
            val player = ctx.player as ServerPlayer
            val level = player.serverLevel()
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
            val level = player.serverLevel()
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

    data class SendSchemRequest(var x: Int, var y: Int, var z: Int): AutoSerializable {
        constructor(pos: BlockPos): this(pos.x, pos.y, pos.z)
        var pos: BlockPos
            get() = BlockPos(x, y, z)
            set(value) {x = value.x; y = value.y; z = value.z}
    }

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

    val loadingText = makeText("Loading...", Color.BLACK, 2f, 2f, screen)

    val itemsScroll = ScrollComponent().constrain {
        x = 2.pixels
        y = 2.pixels

        width  = 100.percent - 4.pixels
        height = 100.percent - 4.pixels
    } childOf screen

    val returnButton: Button = Button(Color.GRAY.brighter(), "Return") {
        itemsScroll.clearChildren()
        be!!.schematic = null
        makeGUI(null)
        VSchematicBuilderNetworking.c2sBeginLoadSchematic.sendToServer(VSchematicBuilderNetworking.SendSchemRequest(pos))
    } constrain {
        x = (2f).pixels(true)
        y = (2f).pixels()
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
                be!!.schematic = PlayerSchematics.loadSchematic(path) as? VModShipSchematicV2 ?: return@Button
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
                if (!ToolgunKeybinds.TOOLGUN_TOGGLE_HUD_KEY.matches(keyCode, scanCode) && keyCode != GLFW.GLFW_KEY_ESCAPE) { return@on false }

                Minecraft.getInstance().setScreen(null)
                return@on true
            }
        }
    }
}

object AllowedFrameBlocks: SimpleJsonResourceReloadListener(Gson(), "allowed_frame_blocks") {
    val allowedBlocks = mutableSetOf<BlockState>()

    override fun apply(
        objects: Map<ResourceLocation?, JsonElement?>,
        resourceManager: ResourceManager,
        profilerFiller: ProfilerFiller
    ) {
        allowedBlocks.clear()
        objects.forEach { (k, v) ->
            if (k == null || v == null) {return@forEach}
            try {
                if (v.isJsonArray) {
                    v.asJsonArray.forEach { parse(it) }
                } else if (v.isJsonObject) {
                    parse(v)
                } else {
                    throw IllegalArgumentException()
                }
            } catch (e: Exception) {
                ELOG("Couldn't parse $v")
            }
        }
    }

    private fun parse(element: JsonElement) {
        try {
            val str = element.asString
            allowedBlocks.add(BuiltInRegistries.BLOCK.get(ResourceLocation(str)).defaultBlockState())
        } catch (e: Exception) {
            ELOG("couldn't parse $element\n${e.stackTraceToString()}")
        }
    }
}

class VSchematicBuilderBE(pos: BlockPos, state: BlockState): BlockEntity(SBlockEntities.VSCHEMATIC_BUILDER.get(), pos, state) {
    var schematic: VModShipSchematicV2? = null

    var lastTime = 0L
    @Volatile var building = false

    override fun saveAdditional(tag: CompoundTag) {
        val stag = (schematic?.serialize() as? CompoundTagSerializable)?.tag ?: return
        tag.put("schematic", stag)
    }

    override fun load(tag: CompoundTag) {
        schematic = null
        val stag = tag.get("schematic") as? CompoundTag ?: return
        schematic = VModShipSchematicV2().also { it.deserialize(CompoundTagSerializable(stag).serialize()) }
        building = false
    }

    private fun findFrame(axis: Vector3i, getContainers: Boolean = false): Pair<Vector3i, List<Container>> {
        val level = level as ServerLevel

        var pos = BlockPos.MutableBlockPos(blockPos.x, blockPos.y, blockPos.z)
        var next = BlockPos.MutableBlockPos(pos.x, pos.y, pos.z)

        val containers = mutableListOf<Container>()
        while (true) {
            next = next.setWithOffset(next, axis.x, axis.y, axis.z)
            if (!AllowedFrameBlocks.allowedBlocks.contains(level.getBlockState(next))) break
            pos.set(next)
            if (!getContainers) continue

            val sides = axis.absolute(Vector3i()).sub(1, 1, 1)
            if (sides.x == -1) {
                level.getBlockEntity(pos.offset( 1,  0,  0))?.also { if (it is Container) containers.add(it) }
                level.getBlockEntity(pos.offset(-1,  0,  0))?.also { if (it is Container) containers.add(it) }
            }
            if (sides.y == -1) {
                level.getBlockEntity(pos.offset( 0,  1,  0))?.also { if (it is Container) containers.add(it) }
                level.getBlockEntity(pos.offset( 0, -1,  0))?.also { if (it is Container) containers.add(it) }
            }
            if (sides.z == -1) {
                level.getBlockEntity(pos.offset( 0,  0,  1))?.also { if (it is Container) containers.add(it) }
                level.getBlockEntity(pos.offset( 0,  0, -1))?.also { if (it is Container) containers.add(it) }
            }
        }

        val offset = Vector3i(pos.x - blockPos.x, pos.y - blockPos.y, pos.z - blockPos.z)
        return Pair(offset, containers)
    }

    private fun getLimits(): Pair<Vector3i, List<Container>> {
        var x = 0
        var y = 0
        var z = 0

        var potential = listOf(
            Vector3i( 0,  1,  0),
            Vector3i( 0, -1,  0),
            Vector3i( 1,  0,  0),
            Vector3i(-1,  0,  0),
            Vector3i( 0,  0,  1),
            Vector3i( 0,  0, -1),
        ).map { findFrame(it) }

        val selected = listOf(
            if (potential[0].first.lengthSquared() >= potential[1].first.lengthSquared()) {potential[0]} else {potential[1]},
            if (potential[2].first.lengthSquared() >= potential[3].first.lengthSquared()) {potential[2]} else {potential[3]},
            if (potential[4].first.lengthSquared() >= potential[5].first.lengthSquared()) {potential[4]} else {potential[5]}
        )

        y = selected[0].first.y
        x = selected[1].first.x
        z = selected[2].first.z

        if (x == 0 || y == 0 || z == 0) { return Pair(Vector3i(x, y, z), listOf()) }

        val containers = selected.map { findFrame(it.first.let { it.div(abs(it.x + it.y + it.z)) }, true).second }.flatten()

        return Pair(Vector3i(x, y, z), containers)
    }

    fun buildSchematic(player: ServerPlayer?) {
        if (building) { return }
        //TODO config
        if (getNow_ms() - lastTime < 1000) { return }
        lastTime = getNow_ms()
        val schematic = schematic ?: return run { player?.sendSystemMessage(makeFake("Schematic is null, how?")) }
        val level = level as ServerLevel

        val schemSize = (schematic.info?.maxObjectPos ?: return run { player?.sendSystemMessage(makeFake("Schematic is incomplete, how?")) }).mul(2.0, JVector3d())
        //TODO get containers separately
        val (maxSize, subContainers) = getLimits()
        val containers = listOfNotNull(
            level.getBlockEntity(blockPos.offset( 0,  1,  0)) as? Container,
            level.getBlockEntity(blockPos.offset( 0, -1,  0)) as? Container,
            level.getBlockEntity(blockPos.offset( 1,  0,  0)) as? Container,
            level.getBlockEntity(blockPos.offset(-1,  0,  0)) as? Container,
            level.getBlockEntity(blockPos.offset( 0,  0,  1)) as? Container,
            level.getBlockEntity(blockPos.offset( 0,  0, -1)) as? Container
        ).toMutableList().also { it.addAll(subContainers) }

        if (containers.isEmpty()) {return run { player?.sendSystemMessage(makeFake("No containers to get blocks from for schematic.")) }}
        if (maxSize.x == 0 || maxSize.y == 0 || maxSize.z == 0) {return run { player?.sendSystemMessage(makeFake("Frame size ${maxSize.absolute()} is too small for schematic size $schemSize")) }}
        if (schemSize.x > abs(maxSize.x) || schemSize.y > abs(maxSize.y) || schemSize.z > abs(maxSize.z)) {return run { player?.sendSystemMessage(makeFake("Frame size ${maxSize.absolute()} is too small for schematic size $schemSize")) }}

        val toPos = Vector3d(blockPos) + Vector3d(maxSize).sign() + Vector3d(maxSize) / 2

        val required = schematicToRequirementsMap(schematic).toMutableMap()
        val notConsumed = required.toMutableMap()
        val cache = mutableListOf<Tuple3<Container, Int, Int>>()

        containers.forEach {
            for (i in 0 until it.containerSize) {
                val stack = it.getItem(i)

                required[stack.item]?.also { vn ->
                    val new = vn - stack.count
                    val remainder = (stack.count - vn).coerceAtLeast(0)

                    cache.add(Tuple.of(it, i, remainder))

                    if (new <= 0) {
                        required.remove(stack.item)
                        return@also
                    }
                    required[stack.item] = new
                }

                if (required.isEmpty()) {return@forEach}
            }
        }

        if (required.isNotEmpty()) {return run { player?.sendSystemMessage(makeFake("Not enough materials in containers to create schematic")) }}

        cache.forEach { (container, position, amount) ->
            when {
                amount <= 0 -> container.setItem(position, ItemStack.EMPTY)
                else        -> container.setItem(position, container.getItem(position).copy().also { it.count = amount })
            }
        }

        building = true
        //TODO not sure if i like it
        schematic.placeAt(level, player, player?.uuid ?: UUID(0L, 0L), toPos.toJomlVector3d(), Quaterniond(),
            PasteSchematicSettings(false, false, logger = SM.logger,
                statePlacedCallback = {pos, state ->
                    val remaining = notConsumed[state.block.asItem()] ?: return@PasteSchematicSettings ELOG("This should be impossible, Schematic has pasted a block state that isn't in notConsumed list. If you see this then pls tell me how you caused this.")
                    notConsumed[state.block.asItem()] = remaining - 1
                    if (remaining == 1) {
                        notConsumed.remove(state.block.asItem())
                        return@PasteSchematicSettings
                    } else if (remaining <= 0) {
                        notConsumed.remove(state.block.asItem())
                        ELOG("This should be impossible, Schematic has pasted more blocks than it should have. If you see this then pls tell me how you caused this.")
                    }
                },
                nonfatalErrorsHandler = { numErrors, _, _ -> ELOG("zamn, $numErrors errors...")},
                externalVSchemSupportProvider = SMSchemCompatObj
            )
        ) {
            building = false
            if (notConsumed.isEmpty()) return@placeAt

            //TODO recompute containers cuz they could become invalid and void items
            containers.forEach { container ->
                for (i in 0 until container.containerSize) {
                    if (notConsumed.isEmpty()) { return@placeAt }

                    val stack = container.getItem(i)
                    val (amount, key) = if (stack.isEmpty) {
                        val key = notConsumed.keys.first()
                        notConsumed[key]!! to key
                    } else {
                        val key = stack.item
                        (notConsumed[key] ?: continue) to key
                    }

                    val saveAmount = min(amount, key.maxStackSize)
                    val newStack = ItemStack(key, saveAmount)
                    container.setItem(i, newStack)

                    if (saveAmount == amount) { notConsumed.remove(key); continue }
                    notConsumed[key] = amount - saveAmount
                }
            }
            if (notConsumed.isEmpty()) return@placeAt

            val spawnPos = Vector3d(blockPos) + 0.5 + Vector3d(0, 1.5, 0)

            notConsumed.forEach { item, remaining ->
                var remaining = remaining
                while (remaining > 0) {
                    val amount = min(remaining, item.maxStackSize)
                    remaining -= amount
                    level.addFreshEntity(ItemEntity(level, spawnPos.x, spawnPos.y, spawnPos.z, ItemStack(item, amount)))
                }
            }
        }
    }

    companion object {
        fun schematicToRequirementsMap(schematic: IShipSchematicDataV1): Map<Item, Int> {
            val blockMap = mutableMapOf<Item, Int>()
            val palette = schematic.blockPalette
            schematic.blockData.forEach { (shipId, data) ->
                data.forEach { x, y, z, item ->
                    val block = palette.fromId(item.paletteId)?.block ?: return@forEach
                    if (block.defaultBlockState().isAir) {return@forEach}
                    val state = block.asItem() ?: return@forEach

                    blockMap[state] = blockMap.getOrDefault(state, 0) + 1
                }
            }
            return blockMap
        }
    }
}