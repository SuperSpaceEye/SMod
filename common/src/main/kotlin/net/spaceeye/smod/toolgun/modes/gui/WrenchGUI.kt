package net.spaceeye.smod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.smod.lang.WRENCH
import net.spaceeye.smod.toolgun.modes.state.WrenchMode
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.EGUIBuilder
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.translate.RADIUS
import net.spaceeye.vmod.translate.STRIP_ALL
import net.spaceeye.vmod.translate.STRIP_IN_RADIUS
import net.spaceeye.vmod.translate.STRIP_MODES
import net.spaceeye.vmod.translate.get

interface WrenchGUI: GUIBuilder, EGUIBuilder {
    override val itemName get() = WRENCH

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        this as WrenchMode

        //TODO
        makeTextEntry(RADIUS.get(), ::radius, 2f, 2f, parentWindow, ServerLimits.instance.stripRadius)
        makeDropDown(STRIP_MODES.get(), parentWindow, 2f, 2f, listOf(
            DItem(STRIP_ALL.get(), mode == WrenchMode.WrenchModes.StripAll) {mode = WrenchMode.WrenchModes.StripAll},
            DItem(STRIP_IN_RADIUS.get(), mode == WrenchMode.WrenchModes.StripInRadius) {mode = WrenchMode.WrenchModes.StripInRadius}
        ))
    }
}