package net.spaceeye.smod.toolgun

import net.spaceeye.smod.toolgun.modes.state.*
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.utils.Registry

object SModToolgunModes: Registry<BaseMode>() {
    init {
        register(WrenchMode::class)
        register(ConnectionMode::class)
        register(RopeMode::class)
        register(PhysRopeMode::class)
    }
}