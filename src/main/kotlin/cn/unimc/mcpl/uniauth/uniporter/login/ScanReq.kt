package cn.unimc.mcpl.uniauth.uniporter.login

import cn.apisium.uniporter.router.api.Route
import cn.apisium.uniporter.router.api.UniporterHttpHandler
import cn.unimc.mcpl.uniauth.AidUtils
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


object ScanReq: UniporterHttpHandler {
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
        if (!paramMap.containsKey("aid")
            || paramMap["aid"]!![0].isNullOrBlank()
            || !AidUtils.isInteger(paramMap["aid"]!![0])
            || paramMap["aid"]!![0].toInt() % 2 == 1
            ) {
            val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST)
            response.headers().set("x-uniauth-version", pluginVersion).set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
            context.writeAndFlush(response)?.addListener(ChannelFutureListener.CLOSE)
            return
        }
        // 验证 aid 参数
        val aid = paramMap["aid"]!![0].toInt()
        if (!AidUtils.hasAid(aid)) {
            val optJson = mapOf("data" to mapOf(
                "success" to false,
                "msg" to "aid not found"
            ))
            val response: FullHttpResponse = DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(optJson.toJSONByteArray())
            )
            response.headers().set("x-uniauth-version", pluginVersion).set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
            context.writeAndFlush(response)?.addListener(ChannelFutureListener.CLOSE)
            return
        }
        // 处理
        val name = AidUtils.getName(aid)
        val player = getProxyPlayer(name)!!
        player.sendMessage("扫码成功，等待授权...")
        AidUtils.plusAid(aid)
        info("玩家 $name 扫码 aid: $aid->${aid + 1}")
        // 构建返回
        val optJson = mapOf("data" to mapOf(
            "success" to true,
            "player_name" to player.displayName,
            "ip" to player.address?.hostName
        ))
        val response: FullHttpResponse = DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK,
            Unpooled.copiedBuffer(optJson.toJSONByteArray())
        )
        response.headers().set("x-uniauth-version", pluginVersion).set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
        context.writeAndFlush(response)?.addListener(ChannelFutureListener.CLOSE)
        return
    }

}
