package cn.unimc.mcpl.uniauth

import io.netty.handler.codec.http.*
import taboolib.common.platform.function.pluginVersion

object Utils {
    fun getRandomString(length: Int) : String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
    fun verifyReqHandler(headers: HttpHeaders?): Boolean {
        return (headers != null && headers.get("Authorization") == UniAuth.config.getString("api.key", "123456"))
    }
    fun build401Resp(): DefaultHttpResponse{
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
    fun build400Resp(): DefaultHttpResponse{
        val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST)
        response.headers()
            .set("x-uniauth-version", pluginVersion)
            .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
        return response
    }
}