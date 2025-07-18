package net.spaceeye.smod.items

import net.minecraft.network.chat.TextComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import net.spaceeye.smod.SItems
import net.spaceeye.valkyrien_ship_schematics.containers.CompoundTagSerializable
import net.spaceeye.vmod.schematic.VModShipSchematicV2
import net.spaceeye.vmod.schematic.makeFrom
import org.valkyrienskies.mod.common.getShipManagingPos
import java.util.UUID

class SchemMakerItem: Item(Properties().tab(SItems.TAB).stacksTo(1)) {
    override fun useOn(useOnContext: UseOnContext): InteractionResult {
        val level = useOnContext.level as? ServerLevel ?: return super.useOn(useOnContext)
        val pos = useOnContext.clickedPos
        val ship = level.getShipManagingPos(pos)

        val stack = useOnContext.itemInHand

        if (ship == null) {
            stack.tag = null
            (useOnContext.player as? ServerPlayer)?.sendMessage(TextComponent("Reset selection"), UUID(0L, 0L))
            return InteractionResult.PASS
        }

        (useOnContext.player as? ServerPlayer)?.sendMessage(TextComponent("Selected valid ship"), UUID(0L, 0L))

        val newSchem = VModShipSchematicV2()
        newSchem.makeFrom(level, useOnContext.player as? ServerPlayer, useOnContext.player?.uuid ?: UUID(0L, 0L), ship) {
            stack.orCreateTag.put("schematic", (newSchem.serialize() as CompoundTagSerializable).tag!!)
            (useOnContext.player as? ServerPlayer)?.sendMessage(TextComponent("Saved schematic to stick"), UUID(0L, 0L))
        }

        return InteractionResult.PASS
    }
}