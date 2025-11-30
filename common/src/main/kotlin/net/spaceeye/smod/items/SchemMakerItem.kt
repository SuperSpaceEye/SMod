package net.spaceeye.smod.items

import dev.architectury.networking.NetworkManager
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.plus
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.spaceeye.smod.ELOG
import net.spaceeye.smod.MOD_ID
import net.spaceeye.smod.SMItems
import net.spaceeye.smod.SM
import net.spaceeye.smod.blockentities.VSchematicBuilderNetworking.SchemHolder
import net.spaceeye.smod.blockentities.VSchematicBuilderNetworking.SendSchemRequest
import net.spaceeye.valkyrien_ship_schematics.ShipSchematic
import net.spaceeye.valkyrien_ship_schematics.interfaces.IShipSchematic
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.events.SessionEvents
import net.spaceeye.vmod.guiElements.Button
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.StrLimit
import net.spaceeye.vmod.networking.DataStream
import net.spaceeye.vmod.networking.regS2C
import net.spaceeye.vmod.reflectable.constructor
import net.spaceeye.vmod.schematic.SchematicActionsQueue.CopySchematicSettings
import net.spaceeye.vmod.schematic.VModShipSchematicV2
import net.spaceeye.vmod.schematic.makeFrom
import net.spaceeye.vmod.toolgun.modes.state.PlayerSchematics
import net.spaceeye.vmod.toolgun.modes.state.SchemMode
import net.spaceeye.vmod.translate.CANCEL
import net.spaceeye.vmod.translate.FILENAME
import net.spaceeye.vmod.translate.SAVE
import net.spaceeye.vmod.translate.get
import net.spaceeye.vmod.utils.BlockPos
import net.spaceeye.vmod.utils.Either
import net.spaceeye.vmod.utils.EmptyPacket
import net.spaceeye.vmod.utils.addCustomServerClosable
import org.valkyrienskies.mod.common.getShipManagingPos
import java.awt.Color
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SchemMakerItem: Item(Properties().`arch$tab`(SMItems.TAB).stacksTo(1)) {
    override fun useOn(useOnContext: UseOnContext): InteractionResult {
        val level = useOnContext.level as? ServerLevel ?: return super.useOn(useOnContext)
        val pos = useOnContext.clickedPos
        val ship = level.getShipManagingPos(pos)

        val stack = useOnContext.itemInHand

        if (ship == null) {
            stack.tag = null
            return InteractionResult.PASS
        }

        (useOnContext.player as? ServerPlayer)?.sendSystemMessage(Component.literal("Selected valid ship"))

        val newSchem = VModShipSchematicV2()
        newSchem.makeFrom(level, useOnContext.player as? ServerPlayer, useOnContext.player?.uuid ?: UUID(0L, 0L), ship,
            CopySchematicSettings(
                false, false, logger = SM.logger
            )) {
            val player = useOnContext.player as? ServerPlayer ?: return@makeFrom
            schematics[player.uuid] = newSchem
            s2cSendSchem.sendToClient(player, EmptyPacket())
        }

        return InteractionResult.PASS
    }

    //TODO this is stupid
    companion object {
        var schematics = ConcurrentHashMap<UUID, IShipSchematic?>()

        init {
            addCustomServerClosable { schematics.clear() }
        }

        var saveSchemStream = object : DataStream<SendSchemRequest, SchemHolder>(
            "save_schem_stream",
            NetworkManager.Side.S2C,
            SM.MOD_ID
        ) {
            override val partByteAmount: Int get() = VMConfig.SERVER.TOOLGUN.SCHEMATIC_PACKET_PART_SIZE
            override fun requestPacketConstructor(buf: FriendlyByteBuf) = SendSchemRequest::class.constructor(buf)
            override fun dataPacketConstructor() = SchemHolder()
            override fun receiverDataTransmissionFailed(failurePkt: RequestFailurePkt) {
                ELOG("Client Save Schem Transmission Failed")
            }

            override fun uuidHasAccess(uuid: UUID, ctx: NetworkManager.PacketContext, req: SendSchemRequest): Boolean {
                val player = ctx.player as? ServerPlayer ?: return false
                return player.mainHandItem.item is SchemMakerItem
            }
            override fun transmitterRequestProcessor(req: SendSchemRequest, ctx: NetworkManager.PacketContext): Either<SchemHolder, RequestFailurePkt>? {
                val res = schematics[ctx.player!!.uuid]?.let { SchemHolder(ShipSchematic.writeSchematicToBuffer(it)!!, BlockPos(0, 0, 0)) }
                return if (res != null) { Either.Left(res) } else { Either.Right(RequestFailurePkt()) }
            }

            override fun receiverDataTransmitted(uuid: UUID, data: SchemHolder, ctx: NetworkManager.PacketContext) {
                val serverSchem = ShipSchematic.getSchematicFromBytes(data.data!!.array()) ?: return

                SessionEvents.clientOnTick.on { (minecraft), unsub ->
                    unsub()
                    minecraft.setScreen(SaveScreen(SaveForm(serverSchem)))
                }
            }
        }

        val s2cSendSchem = regS2C<EmptyPacket>(MOD_ID, "send_schem", "schem_maker") {pkt ->
            saveSchemStream.r2tRequestData.transmitData(SendSchemRequest(BlockPos(0, 0, 0)))
        }

        class SaveForm(val schem: IShipSchematic): UIBlock(Color.GRAY.brighter()) {
            var filename = ""

            init {
                constrain {
                    x = CenterConstraint()
                    y = CenterConstraint()

                    width = 150.pixels()
                    height = 50.pixels()
                }

                val entry = makeTextEntry(FILENAME.get(), ::filename, 2f, 2f, this, StrLimit(50))
                entry.focus()

                Button(Color.GRAY.brighter().brighter(), SAVE.get()) {
                    parent.removeChild(this)
                    SchemMode.saveSchem(PlayerSchematics.listSchematics(), filename, schem)
                    Minecraft.getInstance().setScreen(null)
                }.constrain {
                    x = 2.pixels()
                    y = SiblingConstraint() + 2.pixels()
                } childOf this

                Button(Color.GRAY.brighter().brighter(), CANCEL.get()) {
                    parent.removeChild(this)
                    Minecraft.getInstance().setScreen(null)
                }.constrain {
                    x = 2.pixels()
                    y = SiblingConstraint() + 2.pixels()
                } childOf this
            }
        }

        class SaveScreen(form: SaveForm): WindowScreen(ElementaVersion.V8) {
            override fun isPauseScreen(): Boolean = false

            init {
                form childOf window
            }
        }
    }
}