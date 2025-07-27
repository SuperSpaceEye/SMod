package net.spaceeye.smod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.smod.toolgun.SModToolgunModes
import net.spaceeye.smod.toolgun.modes.gui.WrenchGUI
import net.spaceeye.smod.toolgun.modes.hud.WrenchHUD
import net.spaceeye.smod.vEntityExtensions.SModWrenchable
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.vEntityManaging.VEntityId
import net.spaceeye.vmod.vEntityManaging.getAllVEntityIdsOfShipId
import net.spaceeye.vmod.vEntityManaging.getVEntity
import net.spaceeye.vmod.vEntityManaging.getVEntityIdsOfPosition
import net.spaceeye.vmod.vEntityManaging.removeVEntity
import net.spaceeye.vmod.vEntityManaging.util.ExtendableVEntity
import org.valkyrienskies.mod.common.getShipManagingPos
import kotlin.math.ceil
import kotlin.math.max

class WrenchMode: ExtendableToolgunMode(), WrenchHUD, WrenchGUI {
    enum class WrenchModes {
        StripAll,
        StripInRadius
    }
    @JsonIgnore private var i = 0

    var radius: Double by get(i++, 1.0) { ServerLimits.instance.stripRadius.get(it) }
    var mode: WrenchModes by get(i++, WrenchModes.StripAll)

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {return}
        when (mode) {
            WrenchModes.StripAll -> stripAll(level as ServerLevel, raycastResult)
            WrenchModes.StripInRadius -> stripInRadius(level as ServerLevel, raycastResult)
        }
    }

    private fun stripAll(level: ServerLevel, rr: RaycastFunctions.RaycastResult) {
        val ship = level.getShipManagingPos(rr.blockPosition) ?: return

        level.getAllVEntityIdsOfShipId(ship.id).forEach {
            val mc = level.getVEntity(it)
            if (mc !is ExtendableVEntity || mc.getExtensionsOfType<SModWrenchable>().isEmpty()) { return@forEach }
            mc.getExtensionsOfType<SModWrenchable>().forEach {
                level.addFreshEntity(ItemEntity(level, rr.globalHitPos!!.x, rr.globalHitPos!!.y, rr.globalHitPos!!.z, it.getItemStack()))
            }
            level.removeVEntity(it)
        }
    }

    private fun stripInRadius(level: ServerLevel, rr: RaycastFunctions.RaycastResult) {
        val b = rr.blockPosition
        val r = max(ceil(radius).toInt(), 1)

        for (x in b.x-r .. b.x+r) {
        for (y in b.y-r .. b.y+r) {
        for (z in b.z-r .. b.z+r) {
            val list = level.getVEntityIdsOfPosition(BlockPos(x, y, z)) ?: continue
            val temp = mutableListOf<VEntityId>()
            temp.addAll(list)
            temp.forEach {const ->
                val mc = level.getVEntity(const)
                if (mc !is ExtendableVEntity || mc.getExtensionsOfType<SModWrenchable>().isEmpty()) { return@forEach }
                mc.getAttachmentPoints().forEach {
                    if ((it - rr.globalHitPos!!).dist() <= radius) {
                        mc.getExtensionsOfType<SModWrenchable>().forEach {
                            level.addFreshEntity(ItemEntity(level, rr.globalHitPos!!.x, rr.globalHitPos!!.y, rr.globalHitPos!!.z, it.getItemStack()))
                        }
                        level.removeVEntity(const)
                    }
                }
            }
        } } }
    }

    companion object {
        init {
            SModToolgunModes.registerWrapper(WrenchMode::class) {
                it.addExtension {
                    BasicConnectionExtension<WrenchMode>("wrench_mode"
                        ,leftFunction = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                    )
                }
            }
        }
    }
}