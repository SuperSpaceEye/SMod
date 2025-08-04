package net.spaceeye.smod.vEntityExtensions

import net.spaceeye.vmod.vEntityManaging.VEExtensionTypes

object SModVEntityExtensions {
    init { with(VEExtensionTypes) {
        register(AnyWrenchable::class)
    } }
}