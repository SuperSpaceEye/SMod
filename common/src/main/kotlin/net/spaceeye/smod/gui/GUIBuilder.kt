package net.spaceeye.smod.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.smod.items.TwoPointsItem

interface GUIBuilder {
    fun makeGUISettings(parentWindow: UIContainer, data: TwoPointsItem.ItemData)
}