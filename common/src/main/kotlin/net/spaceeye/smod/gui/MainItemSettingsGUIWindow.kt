package net.spaceeye.smod.gui

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import java.awt.Color

class MainItemSettingsGUIWindow(item: GUIBuilder): WindowScreen(ElementaVersion.V8) {
    private val mainWindow = UIBlock(Color(240, 240, 240)).constrain {
        x = CenterConstraint()
        y = CenterConstraint()

        width = 90f.percent()
        height = 90f.percent()
    } childOf window

    private val scrollComponent = ScrollComponent().constrain {
        x = 0.percent()
        y = 0.percent()

        width = 100.percent()
        height = 100.percent()
    } childOf mainWindow

    init {
        item.makeGUISettings(scrollComponent)
    }
}