package cn.unimc.mcpl.uniauth

import cn.unimc.mcpl.uniauth.uniporter.prometheus.Metrics
import io.prometheus.metrics.exporter.pushgateway.PushGateway
import io.prometheus.metrics.exporter.pushgateway.Scheme
import taboolib.common.platform.function.console
import taboolib.common.platform.function.getProxyPlayer
import taboolib.common.platform.function.submit
import taboolib.module.lang.asLangText
import taboolib.module.lang.sendLang
import java.net.URI

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

    fun taskPushGateway() {
        if (!UniAuth.config.getBoolean("prometheus.enable") || !UniAuth.config.getBoolean("prometheus.pushgateway")) {
            return
        }

        console().sendLang("console-prometheus-pushgateway-enable")

        val url = URI(UniAuth.config.getString("prometheus.pushgateway-url")!!)
        val pushGateway = PushGateway.builder()
            .address(url.host + url.path)
            .scheme(if (url.scheme == "https") Scheme.HTTPS else Scheme.HTTP)
            .job(UniAuth.config.getString("login.server-id"))
            .build()

        submit (period = 60*20){
            Metrics.collectMetrics()

            Utils.debugLog("Prometheus PushGateway Pushing")
            pushGateway.push()
        }
    }
}