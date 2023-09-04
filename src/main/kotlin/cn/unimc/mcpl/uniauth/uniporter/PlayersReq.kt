package cn.unimc.mcpl.uniauth.uniporter

import cn.apisium.uniporter.router.api.Route
import cn.apisium.uniporter.router.api.UniporterHttpHandler
import cn.unimc.mcpl.uniauth.Utils
import com.alibaba.fastjson2.toJSONByteArray
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import taboolib.common.platform.function.info
import taboolib.common.platform.function.onlinePlayers
import taboolib.common.platform.function.pluginVersion
import java.net.InetSocketAddress

object PlayersReq: UniporterHttpHandler {
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

        val optJson = mapOf("data" to mapOf(
            "online" to onlinePlayers().count(),
            "players" to onlinePlayers().map {
                mapOf(
                    "uuid" to it.uniqueId.toString(),
                    "name" to it.displayName
                )
            }
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