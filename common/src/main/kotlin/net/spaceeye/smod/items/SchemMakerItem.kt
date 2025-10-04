package net.spaceeye.smod.items

import net.minecraft.client.multiplayer.ClientLevel
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
import net.spaceeye.smod.SMItems
import net.spaceeye.smod.SM
import net.spaceeye.valkyrien_ship_schematics.containers.CompoundTagSerializable
import net.spaceeye.vmod.schematic.SchematicActionsQueue.CopySchematicSettings
import net.spaceeye.vmod.schematic.VModShipSchematicV2
import net.spaceeye.vmod.schematic.makeFrom
import org.valkyrienskies.mod.common.getShipManagingPos
import java.util.UUID

class SchemMakerItem: Item(Properties().`arch$tab`(SMItems.TAB).stacksTo(1)) {
    override fun use(
        level: Level,
        player: Player,
        interactionHand: InteractionHand
    ): InteractionResultHolder<ItemStack?>? {
        if (player.isShiftKeyDown && level is ClientLevel) {

        }
        return super.use(level, player, interactionHand)
    }

    override fun useOn(useOnContext: UseOnContext): InteractionResult {
        val level = useOnContext.level as? ServerLevel ?: return super.useOn(useOnContext)
        val pos = useOnContext.clickedPos
        val ship = level.getShipManagingPos(pos)

        val stack = useOnContext.itemInHand

        if (ship == null) {
            stack.tag = null
            (useOnContext.player as? ServerPlayer)?.sendSystemMessage(Component.literal("Reset selection"))
            return InteractionResult.PASS
        }

        (useOnContext.player as? ServerPlayer)?.sendSystemMessage(Component.literal("Selected valid ship"))

        val newSchem = VModShipSchematicV2()
        newSchem.makeFrom(level, useOnContext.player as? ServerPlayer, useOnContext.player?.uuid ?: UUID(0L, 0L), ship,
            CopySchematicSettings(
                false, false, logger = SM.logger
            )) {
            stack.orCreateTag.put("schematic", (newSchem.serialize() as CompoundTagSerializable).tag!!)
            (useOnContext.player as? ServerPlayer)?.sendSystemMessage(Component.literal("Saved schematic to stick"))
        }

        return InteractionResult.PASS
    }
}