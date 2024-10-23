package cn.unimc.mcpl.uniauth.updater

import cn.unimc.mcpl.uniauth.UniAuth
import cn.unimc.mcpl.uniauth.Utils
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.warning
import taboolib.expansion.createHelper

enum class PluginStatus {
    NOT_FOUND,
    NEED_UPDATE,
    LATEST
}
data class PluginInfo(
    val name: String,
    val version: String,
    val status: PluginStatus,
    val pluginPortal: PPPlugin?
)

data class PPPlugin(
    @SerializedName("_id")
    val id: String,
    val name: String,
    val platforms: MutableMap<PPMarketplacePlatform, PPPlatformPlugin>,
) {
    val highestPriorityPlatform get() = PPMarketplacePlatform.values().firstOrNull { platforms.containsKey(it) } ?: platforms.keys.first()
    val downloadableName get() = name.replace(Regex("[/\\\\]"), "")
    val totalDownloads: Int get() = platforms.values.sumOf { platform -> platform.downloads }

    fun getFirstPlatform(): PPPlatformPlugin? = platforms.values.firstOrNull()

    fun getImageURL(): String? = platforms.values
        .firstOrNull {
            it.imageURL?.contains(".png") == true || it.imageURL?.contains(".jpg") == true
        }?.imageURL ?: platforms.values.firstOrNull()?.imageURL

    fun getDescription(): String? = getFirstPlatform()?.description?.replace("\n", " ")

    fun getPageUrl(): String {
        val currentPlatform = platforms[highestPriorityPlatform] ?: return "https://pluginportal.link"

        return when (highestPriorityPlatform) {
            PPMarketplacePlatform.MODRINTH -> "https://modrinth.com/plugin/${currentPlatform.id}"
            PPMarketplacePlatform.SPIGOTMC -> "https://www.spigotmc.org/resources/${currentPlatform.id}"
            PPMarketplacePlatform.HANGAR -> "https://hangar.papermc.io/${currentPlatform.author}/${currentPlatform.id}"
        }
    }
}
enum class PPMarketplacePlatform {
    MODRINTH,
    HANGAR,
    SPIGOTMC
}
data class PPPlatformPlugin(
    val id: String,
    val name: String,
    val author: String?,
    val description: String?,
    val downloads: Int,
    val imageURL: String?,
    val download: PPPlatformDownload?,
    val lastUpdated: String?,
)
data class PPPlatformDownload(
    val url: String,
    val version: String,
)

@CommandHeader(name = "uniauthupdater", aliases = ["updater"], permission = "uniauth.updater")
object UpdaterCommands {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val check = subCommand {
        dynamic("插件名") {
            suggestion<CommandSender>(uncheck = true) { _, _ ->
                Bukkit.getPluginManager().plugins.map { it.name }
            }
            execute<CommandSender> { sender, _, argument ->
                Utils.infoCmdSender(sender,"updater-checking")

                if (argument.lowercase() !in Bukkit.getPluginManager().plugins.map { it.name.lowercase() }) {
                    Utils.infoCmdSender(sender,"updater-plugin-not-found", argument)
                } else {
                    val pluginInfo = checkPluginsUpdate(arrayOf(Bukkit.getPluginManager().getPlugin(argument)))[0]
                    outputPluginInfo(pluginInfo, sender)
                }

                Utils.infoCmdSender(sender,"updater-checked")
            }
        }
        execute<CommandSender> { sender, _, _ ->
            Utils.infoCmdSender(sender,"updater-checking")

            for (pluginInfo in checkPluginsUpdate(Bukkit.getPluginManager().plugins)) {
                outputPluginInfo(pluginInfo, sender)
            }

            Utils.infoCmdSender(sender,"updater-checked")
        }
    }

    private fun outputPluginInfo(pluginInfo: PluginInfo, sender: CommandSender) {
        when (pluginInfo.status) {
            PluginStatus.NEED_UPDATE -> {
                Utils.infoCmdSender(sender,
                    "updater-update-available",
                    pluginInfo.name,
                    pluginInfo.pluginPortal!!.highestPriorityPlatform,
                    pluginInfo.version,
                    pluginInfo.pluginPortal.getFirstPlatform()?.download!!.version,
                    pluginInfo.pluginPortal.getFirstPlatform()?.download!!.url
                )
            }
            PluginStatus.LATEST -> {}
            PluginStatus.NOT_FOUND -> {
                Utils.infoCmdSender(sender,
                    "updater-update-notfound",
                    pluginInfo.name
                )
            }
        }
    }

    private fun checkPluginsUpdate(plugins: Array<Plugin>): List<PluginInfo> {
        val result = mutableListOf<PluginInfo>()

        for (plugin in plugins) {
            val searchResult = searchByPluginPortal(plugin.name)
            if (searchResult != null) {
                if (searchResult.name.lowercase() != plugin.name.lowercase()) {
                    Utils.debugLog("${searchResult.name}: ${searchResult.highestPriorityPlatform.name} 搜索结果插件名称 ${searchResult.name} 不对应，判定不存在")
                    result.add(PluginInfo(plugin.name, plugin.description.version, PluginStatus.NOT_FOUND, null))
                }
                if (searchResult.getFirstPlatform()?.download?.version != plugin.description.version) {
                    Utils.debugLog("${searchResult.name}: ${searchResult.highestPriorityPlatform.name} 搜索结果插件版本 ${searchResult.getFirstPlatform()?.download?.version} 本地插件版本 ${plugin.description.version} 不对应，判定需要更新")
                    result.add(PluginInfo(plugin.name, plugin.description.version, PluginStatus.NEED_UPDATE, searchResult))
                } else {
                    Utils.debugLog("${searchResult.name}: ${searchResult.highestPriorityPlatform.name} 搜索结果插件版本 ${searchResult.getFirstPlatform()?.download?.version} 本地插件版本 ${plugin.description.version} 对应，判定无需更新")
                    result.add(PluginInfo(plugin.name, plugin.description.version, PluginStatus.LATEST, searchResult))
                }
            } else {
                Utils.debugLog("${plugin.name}: 搜索结果为空，判定不存在")
                result.add(PluginInfo(plugin.name, plugin.description.version, PluginStatus.NOT_FOUND, null))
            }
        }

        return result
    }

    private fun searchByPluginPortal(name: String): PPPlugin? {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("${UniAuth.config.getString("updater.url") ?: "https://api.pluginportal.link"}/v1/plugins?prefix=${name}&limit=1")
            .header("User-Agent", "UniAuth/1.0.0 (https://ua.unimc.com)")
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            return if (response.isSuccessful) {
                val pluginInfo = Gson().fromJson(response.body?.string(), Array<PPPlugin>::class.java).getOrNull(0)
                pluginInfo
            } else {
                null
            }
        } catch (e: Exception) {
            warning("获取搜索结果失败: ${request.url}")
            e.printStackTrace()
            return null
        }
    }
}