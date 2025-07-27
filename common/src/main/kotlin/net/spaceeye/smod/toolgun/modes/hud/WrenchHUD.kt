package net.spaceeye.smod.toolgun.modes.hud

import net.spaceeye.smod.lang.WRENCH_HUD_1
import net.spaceeye.smod.toolgun.modes.state.WrenchMode
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.get

interface WrenchHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {
        this as WrenchMode
        makeText(WRENCH_HUD_1.get())
    }
}