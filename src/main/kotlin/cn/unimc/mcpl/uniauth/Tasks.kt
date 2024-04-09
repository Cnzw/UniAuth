package cn.unimc.mcpl.uniauth

import taboolib.common.platform.function.console
import taboolib.common.platform.function.getProxyPlayer
import taboolib.common.platform.function.submit
import taboolib.module.lang.asLangText
import taboolib.module.lang.sendLang

object Tasks {

    private val loginTimeout = UniAuth.config.getInt("login.timeout") * 20 // tick

    fun taskLoginTimeout() {
        if (!UniAuth.config.getBoolean("login.enable")) return

        submit(period = 20, delay = loginTimeout.toLong()) {
            val tPlayer = PlayerAuthStateUtils.getAuthTimeoutPlayers()
            tPlayer.forEach {
                if (it.state == AuthState.SCAN) {
                    getProxyPlayer(it.name)?.kick(console().asLangText("kick-scan-timeout", it.name))
                } else {
                    getProxyPlayer(it.name)?.kick(console().asLangText("kick-login-timeout", it.name))
                }
                PlayerAuthStateUtils.setStateFail(it.name)
            }
        }
    }

    fun taskPrompt() {
        if (!UniAuth.config.getBoolean("login.enable")) return

        submit(period = loginTimeout.toLong(), delay = loginTimeout.toLong()) {
            val tPlayer = PlayerAuthStateUtils.getStateLoginPlayers()
            tPlayer.forEach {
                getProxyPlayer(it.name)?.sendLang("player-login-prompt", it.name)
            }
        }

        submit(period = loginTimeout.toLong(), delay = loginTimeout.toLong()) {
            val tPlayer = PlayerAuthStateUtils.getStateScanPlayers()
            tPlayer.forEach {
                getProxyPlayer(it.name)?.sendLang("player-scan-prompt", it.name)
            }
        }
    }


}