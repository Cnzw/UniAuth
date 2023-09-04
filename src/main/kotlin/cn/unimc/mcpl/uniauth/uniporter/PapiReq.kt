package cn.unimc.mcpl.uniauth.uniporter

import cn.apisium.uniporter.router.api.Route
import cn.apisium.uniporter.router.api.UniporterHttpHandler
import cn.unimc.mcpl.uniauth.Utils
import com.alibaba.fastjson2.toJSONByteArray
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.multipart.Attribute
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder
import io.netty.handler.codec.http.multipart.InterfaceHttpData
import me.clip.placeholderapi.PlaceholderAPI
import taboolib.common.platform.function.info
import taboolib.common.platform.function.pluginVersion
import java.net.InetSocketAddress


object PapiReq: UniporterHttpHandler {
    override fun handle(path: String?, route: Route?, context: ChannelHandlerContext?, request: FullHttpRequest?) {
        // 访问日志
        val inSocket: InetSocketAddress = context?.channel()?.remoteAddress() as InetSocketAddress
        info(inSocket.hostName + " - " + request?.method()?.name() + " " + path)
        // 验证header
        if (Utils.verifyReqHandler(request?.headers())) {
            context.writeAndFlush(Utils.build401Resp())?.addListener(ChannelFutureListener.CLOSE)
            return
        }
        // 验证 GET 请求
        if (request?.method() != HttpMethod.POST) {
            context.writeAndFlush(Utils.build405Resp())?.addListener(ChannelFutureListener.CLOSE)
            return
        }
        // 获取 POST 参数
        val paramList = HttpPostRequestDecoder(request).bodyHttpDatas
        val paramMap = mutableMapOf<String, Any>()
        for (item in paramList) {
            if (item.httpDataType == InterfaceHttpData.HttpDataType.Attribute) {
                val data = item as Attribute
                paramMap[data.name] = data.value
            }
        }
        // 验证 POST 参数
        if (!paramMap.containsKey("data")) {
            val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST)
            response.headers().set("x-uniauth-version", pluginVersion).set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
            context.writeAndFlush(response)?.addListener(ChannelFutureListener.CLOSE)
            return
        }

        val optJson = mapOf(
            "data" to mapOf(
                "result" to PlaceholderAPI.setPlaceholders(null, paramMap["data"].toString())
            )
        )
        val response: FullHttpResponse = DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK,
            Unpooled.copiedBuffer(optJson.toJSONByteArray())
        )
        response.headers().set("x-uniauth-version", pluginVersion).set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
        context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
    }

}