package cn.unimc.mcpl.uniauth

import org.bukkit.Bukkit
import taboolib.common.platform.Schedule

object Task {

    @Schedule(period = 20, delay = 1000)
    fun taskLoginTimeout() {
        val tPlayer = AuthCache.getAuthTimeoutPlayers()
        tPlayer.forEach {
            AuthCache.setStatus(it, AuthStatus.FAIL)
            Bukkit.getPlayerExact(it)?.kickPlayer("") // TODO Lang
        }
    }
}