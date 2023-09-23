package cn.unimc.mcpl.uniauth

import org.bukkit.Bukkit
import taboolib.common.platform.Schedule
import taboolib.common.platform.function.console
import taboolib.common.platform.function.onlinePlayers
import taboolib.module.lang.asLangText
import taboolib.module.lang.sendLang

object Task {

    @Schedule(period = 20, delay = 1000)
    fun taskLoginTimeout() {
        val tPlayer = AuthCache.getAuthTimeoutPlayers()
        tPlayer.forEach {
            AuthCache.setStatus(it, AuthStatus.FAIL, Bukkit.getPlayerExact(it)!!.address!!)
            Bukkit.getPlayerExact(it)?.kickPlayer(console().asLangText("kick-timeout"))
        }
    }

    @Schedule(period = 100) // TODO
    fun taskPrompt() {
        for (player in onlinePlayers()) {
            if (!AuthCache.isAuth(player.name)) {
                if (AuthCache.getStatus(player.name) == AuthStatus.LOG) player.sendLang(
                    "player-log-prompt",
                    player.name
                )
                else if (AuthCache.getStatus(player.name) == AuthStatus.SCAN) player.sendLang(
                    "player-scan-prompt",
                    player.name
                )
            }
        }
    }
}
