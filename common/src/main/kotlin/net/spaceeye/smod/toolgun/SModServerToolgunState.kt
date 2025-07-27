package net.spaceeye.smod.toolgun

import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.ToolgunInstance

class SModServerToolgunState(instance: ToolgunInstance): ServerToolGunState(instance) {
    override fun playerHasAccess(player: ServerPlayer): Boolean = true
    override fun verifyPlayerAccessLevel(player: ServerPlayer, permission: String, fn: () -> Unit) {
        fn()
    }
}