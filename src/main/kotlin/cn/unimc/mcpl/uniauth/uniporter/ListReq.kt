package cn.unimc.mcpl.uniauth.uniporter

import cn.apisium.uniporter.router.api.Route
import cn.apisium.uniporter.router.api.UniporterHttpHandler
import cn.unimc.mcpl.uniauth.Utils
import com.google.gson.Gson
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import org.bukkit.Bukkit
import taboolib.common.platform.function.onlinePlayers
import taboolib.platform.compat.getBalance
import taboolib.platform.compat.isEconomySupported
import java.net.InetSocketAddress

object ListReq : UniporterHttpHandler {
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
        // 构建返回
        val optJson = mapOf(
            "code" to 200,
            "data" to mapOf(
                "online" to onlinePlayers().count(),
                "players" to onlinePlayers().map {
                    mapOf(
                        "online" to it.isOnline(),
                        "name" to it.displayName,
                        "uuid" to it.uniqueId.toString(),
                        "ip" to it.address?.hostString,
                        "ping" to it.ping,
                        "uptime" to (System.currentTimeMillis() - it.lastPlayed) / 1000 / 60,
                        "firstJoinTime" to it.firstPlayed,
                        "health" to it.health,
                        "food" to it.foodLevel,
                        "saturation" to it.saturation,
                        "level" to it.level,
                        "exp" to it.exp,
                        "location" to mapOf(
                            "world" to it.world,
                            "x" to it.location.x,
                            "y" to it.location.y,
                            "z" to it.location.z,
                            "yaw" to it.location.yaw,
                            "pitch" to it.location.pitch
                        )
                    ) + if (isEconomySupported) {
                        mapOf("money" to Bukkit.getPlayer(it.uniqueId).getBalance())
                    } else {
                        mapOf()
                    }
                }
            )
        )
        // 输出返回
        context.writeAndFlush(Utils.build200RespByByteBuf(Gson().toJson(optJson).toByteArray()))
            ?.addListener(ChannelFutureListener.CLOSE)
        return
    }
}