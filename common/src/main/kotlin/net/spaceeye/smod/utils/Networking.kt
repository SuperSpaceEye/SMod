package net.spaceeye.smod.utils

import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.smod.SM
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.networking.idWithConnc
import net.spaceeye.vmod.networking.idWithConns
import net.spaceeye.vmod.networking.makeC2S
import net.spaceeye.vmod.networking.makeS2C

@Suppress inline fun <reified T: Serializable>regC2S(name: String, connName: String, crossinline doProcess: (ServerPlayer) -> Boolean, crossinline rejectCallback: (ServerPlayer) -> Unit = {}, crossinline fn: (pkt: T, player: ServerPlayer) -> Unit) = name idWithConnc { makeC2S(ResourceLocation(SM.MOD_ID, "c2s_${connName}_$it"), doProcess, rejectCallback, fn)}
@Suppress inline fun <reified T: Serializable>regC2S(name: String, connName: String, crossinline fn: (pkt: T, player: ServerPlayer) -> Unit) = name idWithConnc { makeC2S(ResourceLocation(SM.MOD_ID, "c2s_${connName}_$it"), fn)}
@Suppress inline fun <reified T: Serializable>regS2C(name: String, connName: String, crossinline fn: (pkt: T) -> Unit) = name idWithConns { makeS2C( ResourceLocation(SM.MOD_ID, "s2c_${connName}_$it"), fn)}
