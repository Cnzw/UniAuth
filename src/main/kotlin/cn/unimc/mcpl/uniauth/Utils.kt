package cn.unimc.mcpl.uniauth

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import org.bukkit.command.CommandSender
import taboolib.common.platform.function.*
import taboolib.module.lang.Language
import taboolib.module.lang.Language.getLocale
import taboolib.module.lang.LanguageFile
import taboolib.module.lang.TypeText
import taboolib.platform.util.sendLang
import java.util.*

object Utils {
    fun debugLog(msg: String) {
        if (UniAuth.config.getBoolean("debug")) info("[DEBUG] $msg")
    }

    private fun getLocaleFile(): LanguageFile? {
        val locale = getLocale()
        return Language.languageFile.entries.firstOrNull { it.key.equals(locale, true) }?.value
            ?: Language.languageFile[Language.default]
            ?: Language.languageFile.values.firstOrNull()
    }

    fun infoLog(node: String, vararg args: Any) {
        info(getLangText(node, *args))
    }

    fun warnLog(node: String, vararg args: Any) {
        warning(getLangText(node, *args))
    }

    fun errorLog(node: String, vararg args: Any) {
        severe(getLangText(node, *args))
    }

    fun getLangText(node: String, vararg args: Any): String {
        val file = getLocaleFile()
        return (file!!.nodes[node] as? TypeText)?.asText(console(), *args) ?: "{$node}"
    }

    fun infoCmdSender(sender: CommandSender, node: String, vararg args: Any) {
        if (sender.name == "CONSOLE") infoLog(node, *args)
        else sender.sendLang(node, *args)
    }

    fun getRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    fun verifyReqHandler(headers: HttpHeaders?): Boolean {
        return (headers != null && headers.get("Authorization") == UniAuth.config.getString("api.key")!!)
    }

    fun build401Resp(): DefaultHttpResponse {
        val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED)
        response.headers()
            .set("x-uniauth-version", pluginVersion)
            .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
        return response
    }
    fun build405Resp(): DefaultHttpResponse {
        val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED)
        response.headers()
            .set("x-uniauth-version", pluginVersion)
            .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
        return response
    }

    fun build400Resp(): DefaultHttpResponse {
        val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST)
        response.headers()
            .set("x-uniauth-version", pluginVersion)
            .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
        return response
    }

    fun build404Resp(): DefaultHttpResponse {
        val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND)
        response.headers()
            .set("x-uniauth-version", pluginVersion)
            .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
        return response
    }

    fun build200RespByByteBuf(content: ByteArray): FullHttpResponse {
        val response: FullHttpResponse = DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK,
            Unpooled.copiedBuffer(content)
        )
        response.headers()
            .set("x-uniauth-version", pluginVersion)
            .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
        return response
    }

    fun getOfflineUUID(name: String): UUID {
        return UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray())
    }
}