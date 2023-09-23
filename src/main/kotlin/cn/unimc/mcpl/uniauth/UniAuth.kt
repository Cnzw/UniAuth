package cn.unimc.mcpl.uniauth


import cn.apisium.uniporter.Uniporter
import org.bukkit.Bukkit
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.console
import taboolib.common.platform.function.pluginVersion
import taboolib.common.platform.function.runningPlatform
import taboolib.module.lang.sendWarn
import taboolib.module.metrics.Metrics
import taboolib.platform.BukkitPlugin

object UniAuth : Plugin() {

    val plugin by lazy { BukkitPlugin.getInstance() }
    var hasPapi: Boolean = false

    override fun onEnable() {
        console().sendWarn("console-debug")

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            hasPapi = true
        } else {
            console().sendWarn("console-papi-not-load")
        }

        if (Utils.config.getBoolean("metrics")) Metrics(19486, pluginVersion, runningPlatform)

        if (Utils.config.getString("api.path")!! == "uniauth") {
            Utils.config.set("api.path", Utils.getRandomString(6))
            console().sendWarn("console-api-path-default", Utils.config.getString("api.path")!!)
        }
        if (Utils.config.getString("api.key")!! == "123456") {
            Utils.config.set("api.key", Utils.getRandomString(12))
            console().sendWarn("console-api-key-default", Utils.config.getString("api.key")!!)
        }
        Utils.config.saveToFile()

        val path = Utils.config.getString("api.path")!!
        Uniporter.registerHandler("$path/v1/ping", cn.unimc.mcpl.uniauth.uniporter.PingReq, true)
        Uniporter.registerHandler("$path/v1/server", cn.unimc.mcpl.uniauth.uniporter.ServerReq, true)
        Uniporter.registerHandler("$path/v1/players", cn.unimc.mcpl.uniauth.uniporter.PlayersReq, true)
        Uniporter.registerHandler("$path/v1/kick", cn.unimc.mcpl.uniauth.uniporter.KickReq, true)
        Uniporter.registerHandler("$path/v1/papi", cn.unimc.mcpl.uniauth.uniporter.PapiReq, true)
        Uniporter.registerHandler("$path/v1/login/scan", cn.unimc.mcpl.uniauth.uniporter.login.ScanReq, true)
        Uniporter.registerHandler("$path/v1/login/confirm", cn.unimc.mcpl.uniauth.uniporter.login.ConfirmReq, true)
        Uniporter.registerHandler("$path/v1/login/cancel", cn.unimc.mcpl.uniauth.uniporter.login.CancelReq, true)
    }
}
