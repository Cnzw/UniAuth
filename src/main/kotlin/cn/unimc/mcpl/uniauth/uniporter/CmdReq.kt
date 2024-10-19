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
import org.bukkit.Bukkit
import taboolib.common.platform.function.console
import taboolib.common.platform.function.info
import taboolib.common.util.sync
import taboolib.module.lang.sendError
import taboolib.module.lang.sendLang
import taboolib.platform.util.bukkitPlugin
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object CmdReq : UniporterHttpHandler {
    init {
        if (UniAuth.config.getBoolean("api.cmd")) {
            console().sendLang("console-api-cmd-enable")
        }
    }
    override fun handle(path: String?, route: Route?, context: ChannelHandlerContext?, request: FullHttpRequest?) {
        // 访问日志
        val inSocket: InetSocketAddress = context?.channel()?.remoteAddress() as InetSocketAddress
        Utils.debugLog(inSocket.hostName + " - " + request?.method()?.name() + " " + path)

        if (!UniAuth.config.getBoolean("api.cmd")) {
            context.writeAndFlush(Utils.build404Resp())?.addListener(ChannelFutureListener.CLOSE)
            return
        }
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
        if (!paramMap.containsKey("cmd")) {
            context.writeAndFlush(Utils.build400Resp())?.addListener(ChannelFutureListener.CLOSE)
            return
        }
        info("$path 执行指令 ${paramMap["cmd"].toString()}")

        Bukkit.getScheduler().callSyncMethod(bukkitPlugin) {
            console().performCommand(paramMap["cmd"].toString())
        }
        val completableFuture: CompletableFuture<List<String>> = CompletableFuture()

        try {
            completableFuture.get(5000, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            console().sendError("console-api-cmd-err")
            e.printStackTrace()
        }
        var result: List<String> = listOf()
        completableFuture.complete(result)

        // 构建返回
        val optJson = mapOf(
            "code" to 200,
            "data" to mapOf(
                "result" to result.joinToString("\n")
            )
        )
        // 输出返回
        context.writeAndFlush(Utils.build200RespByByteBuf(Gson().toJson(optJson).toByteArray()))
            ?.addListener(ChannelFutureListener.CLOSE)
        return
    }
}