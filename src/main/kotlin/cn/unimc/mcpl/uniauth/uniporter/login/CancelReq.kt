package cn.unimc.mcpl.uniauth.uniporter.login

import cn.apisium.uniporter.router.api.Route
import cn.apisium.uniporter.router.api.UniporterHttpHandler
import cn.unimc.mcpl.uniauth.AuthState
import cn.unimc.mcpl.uniauth.PlayerAuthStateUtils
import cn.unimc.mcpl.uniauth.Utils
import com.google.gson.Gson
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.QueryStringDecoder
import org.bukkit.Bukkit
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.console
import taboolib.common.util.sync
import taboolib.module.lang.asLangText
import java.net.InetSocketAddress

object CancelReq : UniporterHttpHandler {
    override fun handle(path: String?, route: Route?, context: ChannelHandlerContext?, request: FullHttpRequest?) {
        // 访问日志
        val inSocket: InetSocketAddress = context?.channel()?.remoteAddress() as InetSocketAddress
        Utils.debugLog(inSocket.hostName + " - " + request?.method()?.name() + " " + path)
        // 验证 Header Authorization
        if (!Utils.verifyReqHandler(request?.headers())) {
            context.writeAndFlush(Utils.build401Resp())?.addListener(ChannelFutureListener.CLOSE)
            return
        }
        // 验证请求类型 GET
        if (request?.method() != HttpMethod.GET) {
            context.writeAndFlush(Utils.build405Resp())?.addListener(ChannelFutureListener.CLOSE)
            return
        }
        // 获取验证 GET 参数
        val paramMap = QueryStringDecoder(request?.uri()).parameters()
        if (!paramMap.containsKey("code")
            || paramMap["code"]?.get(0).isNullOrBlank()
        ) {
            context.writeAndFlush(Utils.build400Resp())?.addListener(ChannelFutureListener.CLOSE)
            return
        }
        // 验证 code
        if (PlayerAuthStateUtils.checkState(paramMap["code"]!![0].toInt()) != AuthState.SCAN) {
            context.writeAndFlush(Utils.build404Resp())?.addListener(ChannelFutureListener.CLOSE)
            return
        }

        val name = PlayerAuthStateUtils.getName(paramMap["code"]!![0].toInt())!!
        val player = Bukkit.getPlayer(name)
        // 监听器里已经写了Fail代码
        // PlayerAuthStateUtils.setStateFail(name)
        sync { player.kickPlayer(console().asLangText("kick-login-cancel")) }

        // 构建返回
        val optJson = mapOf(
            "code" to 200,
            "data" to mapOf(
                "result" to true
            )
        )
        context.writeAndFlush(Utils.build200RespByByteBuf(Gson().toJson(optJson).toByteArray()))
            ?.addListener(ChannelFutureListener.CLOSE)
        return
    }
}