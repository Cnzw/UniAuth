package cn.unimc.mcpl.uniauth.uniporter

import cn.apisium.uniporter.router.api.Route
import cn.apisium.uniporter.router.api.UniporterHttpHandler
import cn.unimc.mcpl.uniauth.Utils
import com.alibaba.fastjson2.toJSONByteArray
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import taboolib.common.platform.function.getProxyPlayer
import taboolib.common.platform.function.info
import taboolib.common.platform.function.pluginVersion
import java.net.InetSocketAddress

object KickReq: UniporterHttpHandler {
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
        if (request?.method() != HttpMethod.GET) {
            context.writeAndFlush(Utils.build405Resp())?.addListener(ChannelFutureListener.CLOSE)
            return
        }
        // 获取验证 GET 参数
        val paramMap = QueryStringDecoder(request?.uri()).parameters()
        if (!paramMap.containsKey("name")
            || paramMap["name"]!![0].isNullOrBlank()
        ) {
            val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST)
            response.headers().set("x-uniauth-version", pluginVersion).set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
            context.writeAndFlush(response)?.addListener(ChannelFutureListener.CLOSE)
            return
        }
        val name = paramMap["name"]!![0]
        val player = getProxyPlayer(name)
        player?.let {
            player.kick(paramMap["reason"]?.get(0)) //TODO kick
            val optJson = mapOf("data" to mapOf(
                "success" to true
            ))
            val response: FullHttpResponse = DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(optJson.toJSONByteArray())
            )
            response.headers().set("x-uniauth-version", pluginVersion).set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
            context.writeAndFlush(response)?.addListener(ChannelFutureListener.CLOSE)
        } ?: let {
            val optJson = mapOf("data" to mapOf(
                "success" to false,
                "msg" to "player not found"
            ))
            val response: FullHttpResponse = DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(optJson.toJSONByteArray())
            )
            response.headers().set("x-uniauth-version", pluginVersion).set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
            context.writeAndFlush(response)?.addListener(ChannelFutureListener.CLOSE)
        }
    }

}