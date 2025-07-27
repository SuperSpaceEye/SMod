package net.spaceeye.smod

import net.spaceeye.smod.blockentities.VSchematicBuilderNetworking
import net.spaceeye.smod.toolgun.SMToolgun
import net.spaceeye.smod.toolgun.SModToolgunModes
import net.spaceeye.smod.vEntityExtensions.SModVEntityExtensions
import net.spaceeye.smod.vEntityExtensions.SModWrenchableExtensions
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

fun ILOG(s: String) = SM.logger.info(s)
fun WLOG(s: String) = SM.logger.warn(s)
fun DLOG(s: String) = SM.logger.debug(s)
fun ELOG(s: String) = SM.logger.error(s)

const val MOD_ID = "the_smod"
object SM {
    const val MOD_ID: String = "the_smod"
    val logger: Logger = LogManager.getLogger(MOD_ID)!!

    @JvmStatic
    fun init() {
        SModVEntityExtensions
        SModWrenchableExtensions

        SBlocks.register()
        SBlockEntities.register()
        SMItems.register()

        SModToolgunModes
        SMToolgun

        VSchematicBuilderNetworking
    }
}
