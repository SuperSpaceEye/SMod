package net.spaceeye.smod.toolgun.modes.util

import net.minecraft.client.resources.language.I18n
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.spaceeye.smod.lang.NOT_ENOUGH_ITEMS
import net.spaceeye.smod.utils.regS2C
import net.spaceeye.vmod.gui.additions.ErrorAddition
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.translate.get
import kotlin.math.roundToInt

object SurvivalUtils {
    var costPerUnit = 1.0

    val s2cNotEnoughItems = regS2C<NotEnoughItemsPkt>("not_enough_items", "survival_conn") {
        ErrorAddition.Companion.addHUDError(
            NOT_ENOUGH_ITEMS.get()
                .replace("==NUM_ITEMS==", it.num.toString())
                .replace("==ITEM_NAME==", I18n.get(it.name))
        )
    }

    data class NotEnoughItemsPkt(var num: Int, var name: String): AutoSerializable

    @JvmStatic
    fun survivalGate(
        player: ServerPlayer,
        requiredItem: Item,
        cost: Int,
    ): Int {
        val inventory = player.inventory
        val positions = mutableListOf<Int>()

        var remaining = cost
        for ((i, stack) in inventory.items.withIndex()) {
            if (stack.item != requiredItem) continue
            positions.add(i)
            remaining -= stack.count
            if (remaining <= 0) break
        }
        if (remaining > 0) return remaining
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

        return 0
    }

    @JvmStatic
    fun survivalGateWithCallback(
        player: ServerPlayer,
        requiredItem: Item,
        cost: Int,
    ): Boolean {
        val remaining = survivalGate(player, requiredItem, cost)
        if (remaining == 0) {return true}
        s2cNotEnoughItems.sendToClient(player, NotEnoughItemsPkt(remaining, requiredItem.descriptionId))
        return false
    }

    @JvmStatic
    fun costCalc(dist: Double, amountPerUnit: Double = costPerUnit): Int =
        (dist * amountPerUnit)
            .roundToInt()
            .let { if (dist * amountPerUnit > it) it + 1 else it }
            .coerceAtLeast(1)
}