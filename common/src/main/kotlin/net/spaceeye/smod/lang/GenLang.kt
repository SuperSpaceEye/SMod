package net.spaceeye.smod.lang

import java.nio.file.Paths
import kotlin.collections.forEach
import kotlin.io.path.writeText
import kotlin.let
import kotlin.text.replace
import kotlin.text.split

fun main() {
    var result = "{"
    var lastKey = ""

    registeredComponents.forEach {
        val key = it.key.split(".").let { it[it.size-2] }
        val doNewline = key != lastKey
        lastKey = key
        result += "${if (doNewline) "\n" else ""}  \"${it.key}\": \"${it.enTranslation.replace("\n", "\\n")}\",\n"
    }

    val constData = """  
  "item.the_smod.survival_toolgun": "Survival Toolgun",
  "item.the_smod.rope": "Rope",
  "item.the_smod.phys_rope": "Physics Rope",
  "item.the_smod.connection_item": "Connection",
  
  "itemGroup.the_smod.smod_tab": "SMod"
"""
    result += constData
    result += "}"

    Paths.get("common/src/main/resources/assets/the_smod/lang/en_us.json").writeText(result)

    println(result)
}