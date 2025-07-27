package net.spaceeye.smod.toolgun

import dev.architectury.utils.Env
import dev.architectury.utils.EnvExecutor
import net.spaceeye.smod.SM
import net.spaceeye.vmod.MOD_ID
import net.spaceeye.vmod.gui.ScreenWindow
import net.spaceeye.vmod.gui.additions.HUDAddition
import net.spaceeye.vmod.gui.additions.InfoAddition
import net.spaceeye.vmod.gui.additions.PresetsAddition
import net.spaceeye.vmod.toolgun.ToolgunInstance
import net.spaceeye.vmod.toolgun.gui.ClientSettingsGUI
import net.spaceeye.vmod.toolgun.gui.SettingPresets
import net.spaceeye.vmod.toolgun.gui.ToolgunGUI
import net.spaceeye.vmod.translate.CLIENT_SETTINGS
import net.spaceeye.vmod.translate.MAIN
import net.spaceeye.vmod.translate.SETTINGS_PRESETS

private var instance: ToolgunInstance? = null
    get() {
        if (field != null) return field
        field = ToolgunInstance(
            SM.MOD_ID,
            SModToolgunModes
        )

        field!!.server = SModServerToolgunState(field!!)
        field!!.server.instance = field!!

        EnvExecutor.runInEnv(Env.CLIENT) { Runnable {
            field!!.client = SModClientToolgunState(field!!)
            field!!.client.instance = field!!
            field!!.client.initState()

            field!!.client.addWindow(MAIN) {ToolgunGUI(it, field!!.client)}
            field!!.client.addWindow(CLIENT_SETTINGS) {ClientSettingsGUI(it)}
            field!!.client.addWindow(SETTINGS_PRESETS) {SettingPresets(it, field!!.client)}

            ScreenWindow.addScreenAddition { PresetsAddition().also { it.instance = field!! } }
            ScreenWindow.addScreenAddition { InfoAddition()   .also { it.instance = field!! } }
        } }

        return field
    }

val SMToolgun: ToolgunInstance get() = instance!!