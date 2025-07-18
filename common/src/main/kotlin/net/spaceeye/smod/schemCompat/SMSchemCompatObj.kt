package net.spaceeye.smod.schemCompat

import net.spaceeye.smod.SM
import net.spaceeye.vmod.compat.schem.BaseVSchemCompatProvider
import net.spaceeye.vmod.compat.schem.ClockworkSchemCompat
import net.spaceeye.vmod.compat.schem.CreateContraptionsCompat
import net.spaceeye.vmod.compat.schem.CreateKineticsCompat
import net.spaceeye.vmod.compat.schem.TakeoffSchemCompat
import net.spaceeye.vmod.compat.schem.TrackworkSchemCompat

object SMSchemCompatObj: BaseVSchemCompatProvider(SM.logger) {
    init {
        safeAdd("vs_clockwork") { ClockworkSchemCompat() }
        safeAdd("trackwork") { TrackworkSchemCompat() }
        safeAdd("takeoff") { TakeoffSchemCompat() }

        safeAdd("create") { CreateContraptionsCompat() }
        safeAdd("create") { CreateKineticsCompat() }

        //survival compat
        safeAdd("create") { CreateIMultiBlockEntitySurvivalCompat() }
    }
}