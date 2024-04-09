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
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import taboolib.common.platform.function.onlinePlayers
import java.lang.management.ManagementFactory
import java.net.InetSocketAddress

// TODO 告知需要server扩展
object ServerReq : UniporterHttpHandler {
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

        val tps: String? = if (UniAuth.PapiEnabled && PlaceholderAPI.isRegistered("server")) {
            PlaceholderAPI.setPlaceholders(null, "%server_tps_5%")
        } else {
            null
        }
        // 构建返回
        val optJson = mapOf(
            "code" to 200,
            "data" to mapOf(
                "name" to Bukkit.getName(),
                "version" to Bukkit.getBukkitVersion(),
                "health" to mapOf(
                    "uptime" to ManagementFactory.getRuntimeMXBean().uptime / 1000 / 60,
                    "tps" to tps,
                    "totalMemory" to Runtime.getRuntime().totalMemory(),
                    "maxMemory" to Runtime.getRuntime().maxMemory(),
                    "freeMemory" to Runtime.getRuntime().freeMemory()
                ),
                "motd" to Bukkit.getMotd(),
                "maxPlayers" to Bukkit.getMaxPlayers(),
                "onlinePlayers" to onlinePlayers().count()
            )
        )
        context.writeAndFlush(Utils.build200RespByByteBuf(Gson().toJson(optJson).toByteArray()))
            ?.addListener(ChannelFutureListener.CLOSE)
        return
    }
}