package net.spaceeye.smod.toolgun

import net.minecraft.client.Minecraft
import net.spaceeye.smod.SMItems
import net.spaceeye.vmod.gui.ScreenWindow
import net.spaceeye.vmod.gui.additions.HUDAddition
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.ToolgunInstance
import net.spaceeye.vmod.toolgun.gui.MainToolgunGUIWindow
import net.spaceeye.vmod.toolgun.modes.DefaultHUD
import net.spaceeye.vmod.utils.FakeKProperty

class SModClientToolgunState(instance: ToolgunInstance): ClientToolGunState(instance) {
    init {
        toolgunGuiIsOpened = {Minecraft.getInstance().screen.let { it is MainToolgunGUIWindow }}
        playerIsUsingToolgun = {Minecraft.getInstance().player?.mainHandItem?.item == SMItems.SURVIVAL_TOOLGUN.get().asItem()}
        initHudAddition = {
            val hudAddition = HUDAddition().also { it.instance = instance }
            hudAddition.defaultHUD = DefaultHUD("SMod")
            ScreenWindow.addScreenAddition { hudAddition }
            renderHud = FakeKProperty({hudAddition.renderHUD}) {hudAddition.renderHUD = it}
        }
    }
}