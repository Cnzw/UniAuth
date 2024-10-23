package cn.unimc.mcpl.uniauth

import cn.unimc.mcpl.uniauth.updater.UpdaterCommands
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.expansion.createHelper

@CommandHeader("uniauth", ["ua"], permission = "uniauth.help")
object Commands {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val updater = UpdaterCommands
}