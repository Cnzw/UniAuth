package cn.unimc.mcpl.uniauth

import cn.apisium.uniporter.Uniporter
import org.bukkit.Bukkit
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.pluginVersion
import taboolib.common.platform.function.runningPlatform
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile
import taboolib.module.metrics.Metrics

object UniAuth : Plugin() {

    @Config("config.yml")
    lateinit var config: ConfigFile

    var PapiEnabled: Boolean = false

    override fun onEnable() {

        if (config.getBoolean("debug")) {
            Utils.warnLog("console-debug")
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            PapiEnabled = true
        } else {
            Utils.warnLog("console-papi-not-load")
        }

        if (config.getBoolean("metrics")) Metrics(19486, pluginVersion, runningPlatform)

        if (config.getString("api.path")!! == "uniauth") {
            config.set("api.path", Utils.getRandomString(6))
            Utils.warnLog("console-api-path-default", config.getString("api.path")!!)
            config.saveToFile()
        }
        if (config.getString("api.key")!! == "123456") {
            config.set("api.key", Utils.getRandomString(12))
            Utils.warnLog("console-api-key-default", config.getString("api.path")!!)
            config.saveToFile()
        }

        val path = config.getString("api.path")!!
        Uniporter.registerHandler("$path/v1/ping", cn.unimc.mcpl.uniauth.uniporter.PingReq, true)
        Uniporter.registerHandler("$path/v1/server", cn.unimc.mcpl.uniauth.uniporter.ServerReq, true)
        Uniporter.registerHandler("$path/v1/player", cn.unimc.mcpl.uniauth.uniporter.PlayerReq, true)
        Uniporter.registerHandler("$path/v1/kick", cn.unimc.mcpl.uniauth.uniporter.KickReq, true)
        Uniporter.registerHandler("$path/v1/papi", cn.unimc.mcpl.uniauth.uniporter.PapiReq, true)
        Uniporter.registerHandler("$path/v1/cmd", cn.unimc.mcpl.uniauth.uniporter.CmdReq, true)
        Uniporter.registerHandler("$path/v1/list", cn.unimc.mcpl.uniauth.uniporter.ListReq, true)
        Uniporter.registerHandler("$path/v1/plugins", cn.unimc.mcpl.uniauth.uniporter.PluginsReq, true)
        Uniporter.registerHandler("$path/v1/login/scan", cn.unimc.mcpl.uniauth.uniporter.login.ScanReq, true)
        Uniporter.registerHandler("$path/v1/login/confirm", cn.unimc.mcpl.uniauth.uniporter.login.ConfirmReq, true)
        Uniporter.registerHandler("$path/v1/login/cancel", cn.unimc.mcpl.uniauth.uniporter.login.CancelReq, true)

        Uniporter.registerHandler("$path/v1/metrics", cn.unimc.mcpl.uniauth.uniporter.prometheus.Metrics, true)

        Tasks.taskLoginTimeout()
        Tasks.taskPrompt()
        Tasks.taskPushGateway()
    }
}