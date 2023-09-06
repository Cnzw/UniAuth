package cn.unimc.mcpl.uniauth

import io.netty.handler.codec.http.*
import taboolib.common.platform.function.info
import taboolib.common.platform.function.pluginVersion
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import java.util.regex.Pattern

object Utils {
    @Config("config.yml")
    lateinit var config: Configuration
    fun debugLog(msg: String) {
        if (this.config.getBoolean("debug")) info(msg)
    }

    fun getRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    fun verifyReqHandler(headers: HttpHeaders?): Boolean {
        return (headers != null && headers.get("Authorization") == this.config.getString("api.key")!!)
    }

    fun build401Resp(): DefaultHttpResponse {
        val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED)
        response.headers()
            .set("x-uniauth-version", pluginVersion)
            .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
        return response
    }
    fun build405Resp(): DefaultHttpResponse{
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

    fun compilePattern(pattern: String): Pattern {
        return try {
            Pattern.compile(pattern)
        } catch (e: Exception) {
            warning("编译表达式 '$pattern' 失败，允许所有字符")
            Pattern.compile(".*?")
        }
    }
}