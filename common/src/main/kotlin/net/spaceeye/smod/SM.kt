package net.spaceeye.smod

import dev.architectury.platform.Platform
import net.spaceeye.smod.vEntityExtensions.SModVEntityExtensions
import net.spaceeye.vmod.VM
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

fun ILOG(s: String) = SM.logger.info(s)
fun WLOG(s: String) = SM.logger.warn(s)
fun DLOG(s: String) = SM.logger.debug(s)
fun ELOG(s: String) = SM.logger.error(s)

object SM {
    const val MOD_ID: String = "the_smod"
    val logger: Logger = LogManager.getLogger(MOD_ID)!!

    @JvmStatic
    fun init() {
        if (Platform.isDevelopmentEnvironment()) {
            VM.STUPID_FUCKING_THING_DOESNT_WORK = true
        }
        SModVEntityExtensions
        SItems.register()
    }
}
