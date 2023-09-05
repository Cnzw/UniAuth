package cn.unimc.mcpl.uniauth


import cn.apisium.uniporter.Uniporter
import org.bukkit.Bukkit
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.pluginVersion
import taboolib.common.platform.function.runningPlatform
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.metrics.Metrics
import taboolib.platform.BukkitPlugin

object UniAuth : Plugin() {

    @Config("config.yml")
    lateinit var config: Configuration
    //TODO 语言文件和自定义发送方式
    val plugin by lazy { BukkitPlugin.getInstance() }
    var hasPapi: Boolean = false

    override fun onEnable() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            hasPapi = true
        } else {
            warning("软依赖 PlaceholderAPI 并未加载，部分功能受限")
        }

        if (config.getBoolean("metrics", true)) Metrics(19486, pluginVersion, runningPlatform)

        if (config.getString("api.path")!! == "uniauth") {
            config.set("api.path", Utils.getRandomString(6))
            warning("检测到 api.path 是默认路径，已生成随机路径")
        }
        if (config.getString("api.key")!! == "123456") {
            config.set("api.key", Utils.getRandomString(12))
            warning("检测到 api.key 是默认密钥，已生成随机密钥")
        }
        
        val path = config.getString("api.path")!!
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
