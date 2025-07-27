package net.spaceeye.smod.toolgun

import net.spaceeye.smod.toolgun.modes.state.ConnectionMode
import net.spaceeye.smod.toolgun.modes.state.WrenchMode
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.utils.Registry

object SModToolgunModes: Registry<BaseMode>() {
    init {
        register(WrenchMode::class)
        register(ConnectionMode::class)
    }
}