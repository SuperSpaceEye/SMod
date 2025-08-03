package net.spaceeye.smod.lang

import net.minecraft.network.chat.TranslatableComponent

val registeredComponents = mutableListOf<MyTranslatableComponent>()

data class MyTranslatableComponent(val enTranslation: String, val key: String) {
    init {
        registeredComponents.add(this)
    }

    fun asMC() = TranslatableComponent(key)
}
fun makeComponent(default: String, key: String) = MyTranslatableComponent(default, key).asMC()

private const val path = "smod.gui."
private fun t(default: String) = makeComponent(default, path +default.lowercase().replace(" ", "_").replace("'", ""))
private fun s(default: String) = makeComponent(default, path +"setting."       +default.lowercase().replace(" ", "_").replace("'", ""))
private fun x(default: String) = makeComponent(default, path +"text."          +default.lowercase().replace(" ", "_").replace("'", ""))
private fun v(default: String) = makeComponent(default, path +"server_setting."+default.lowercase().replace(" ", "_").replace("'", ""))
private fun a(default: String) = makeComponent(default, path +"tabs."          +default.lowercase().replace(" ", "_").replace("'", ""))
private fun p(default: String) = makeComponent(default, path +"popup."         +default.lowercase().replace(" ", "_").replace("'", ""))

private fun t(default: String, key: String) = makeComponent(default, path +key)
private fun s(default: String, key: String) = makeComponent(default, path +"setting."+key)
private fun x(default: String, key: String) = makeComponent(default, path +"text."+key)
private fun v(default: String, key: String) = makeComponent(default, path +"server_setting."+key)
private fun a(default: String, key: String) = makeComponent(default, path +"tabs."+key)

val WRENCH = t("Wrench")

val WRENCH_HUD_1 = x("LMB - wrench", "wrench_hud_1")

val NOT_ENOUGH_ITEMS = x("Need ==NUM_ITEMS== more \\\"==ITEM_NAME==\\\"!", "not_enough_items")