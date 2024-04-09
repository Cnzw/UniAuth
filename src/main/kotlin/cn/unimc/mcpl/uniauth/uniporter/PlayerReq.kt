package cn.unimc.mcpl.uniauth.uniporter

import cn.apisium.uniporter.router.api.Route
import cn.apisium.uniporter.router.api.UniporterHttpHandler
import cn.unimc.mcpl.uniauth.Utils
import com.google.gson.Gson
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.QueryStringDecoder
import org.bukkit.Bukkit
import taboolib.common.platform.function.getProxyPlayer
import taboolib.platform.compat.getBalance
import taboolib.platform.compat.isEconomySupported
import java.net.InetSocketAddress
import java.util.UUID

object PlayerReq : UniporterHttpHandler {
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
        if (!paramMap.containsKey("name")
            || paramMap["name"]?.get(0).isNullOrBlank()
        ) {
            context.writeAndFlush(Utils.build400Resp())?.addListener(ChannelFutureListener.CLOSE)
            return
        }

        val player = getProxyPlayer(paramMap["name"]!![0])
        if (player == null) {
            context.writeAndFlush(Utils.build404Resp())?.addListener(ChannelFutureListener.CLOSE)
            return
        }

        // 构建返回
        val optJson = if (player!!.isOnline()) {
            mapOf(
                "code" to 200,
                "data" to mapOf(
                    "online" to player.isOnline(),
                    "name" to player.displayName,
                    "uuid" to player.uniqueId.toString(),
                    "ip" to player.address?.hostString,
                    "ping" to player.ping,
                    "uptime" to (System.currentTimeMillis() - player.lastPlayed) / 1000 / 60,
                    "firstJoinTime" to player.firstPlayed,
                    "health" to player.health,
                    "food" to player.foodLevel,
                    "saturation" to player.saturation,
                    "level" to player.level,
                    "exp" to player.exp,
                    "location" to mapOf(
                        "world" to player.world,
                        "x" to player.location.x,
                        "y" to player.location.y,
                        "z" to player.location.z,
                        "yaw" to player.location.yaw,
                        "pitch" to player.location.pitch
                    )
                ) + if (isEconomySupported) {
                    mapOf("money" to Bukkit.getPlayer(player.uniqueId).getBalance())
                } else {
                    mapOf()
                }
            )
        } else {
            mapOf(
                "code" to 200,
                "data" to mapOf(
                    "online" to player.isOnline(),
                    "name" to player.displayName,
                    "uuid" to player.uniqueId.toString(),
                    "firstJoinTime" to player.firstPlayed,
                ) + if (isEconomySupported) {
                    mapOf("money" to Bukkit.getOfflinePlayer(player.uniqueId).getBalance())
                } else {
                    mapOf()
                }
            )
        }
        // 输出返回
        context.writeAndFlush(Utils.build200RespByByteBuf(Gson().toJson(optJson).toByteArray()))
            ?.addListener(ChannelFutureListener.CLOSE)
        return
    }
}