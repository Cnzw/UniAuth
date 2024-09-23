package cn.unimc.mcpl.uniauth.uniporter

import cn.apisium.uniporter.router.api.Route
import cn.apisium.uniporter.router.api.UniporterHttpHandler
import cn.unimc.mcpl.uniauth.UniAuth
import cn.unimc.mcpl.uniauth.Utils
import com.google.gson.Gson
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.multipart.Attribute
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder
import io.netty.handler.codec.http.multipart.InterfaceHttpData
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import java.net.InetSocketAddress

object PapiReq : UniporterHttpHandler {
    override fun handle(path: String?, route: Route?, context: ChannelHandlerContext?, request: FullHttpRequest?) {
        // 访问日志
        val inSocket: InetSocketAddress = context?.channel()?.remoteAddress() as InetSocketAddress
        Utils.debugLog(inSocket.hostName + " - " + request?.method()?.name() + " " + path)
        // 验证 Header Authorization
        if (!Utils.verifyReqHandler(request?.headers())) {
            context.writeAndFlush(Utils.build401Resp())?.addListener(ChannelFutureListener.CLOSE)
            return
        }
        // 验证请求类型 POST
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
        // 获取验证 POST 参数
        if (!paramMap.containsKey("data")) {
            context.writeAndFlush(Utils.build400Resp())?.addListener(ChannelFutureListener.CLOSE)
            return
        }

        if (!UniAuth.PapiEnabled) {
            context.writeAndFlush(Utils.build404Resp())?.addListener(ChannelFutureListener.CLOSE)
            return
        }

        val player = if (paramMap.containsKey("name")) {
            Bukkit.getPlayer(paramMap["name"].toString())
        } else {
            null
        }
        // 构建返回
        val optJson = mapOf(
            "code" to 200,
            "data" to mapOf(
                "result" to PlaceholderAPI.setPlaceholders(player, paramMap["data"].toString())
            )
        )
        // 输出返回
        context.writeAndFlush(Utils.build200RespByByteBuf(Gson().toJson(optJson).toByteArray()))
            ?.addListener(ChannelFutureListener.CLOSE)
        return
    }
}