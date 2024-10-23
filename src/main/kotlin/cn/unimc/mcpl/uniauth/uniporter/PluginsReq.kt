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
import java.net.InetSocketAddress

object PluginsReq: UniporterHttpHandler {
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

        val plugins = Bukkit.getPluginManager().plugins
        // 构建返回
        val optJson = mapOf(
            "code" to 200,
            "data" to mapOf(
                "count" to plugins.size,
                "plugins" to plugins.map {
                    mapOf(
                        "enabled" to it.isEnabled,
                        "name" to it.name,
                        "description" to it.description.description,
                        "version" to it.description.version,
                        "authors" to it.description.authors,
                        "depends" to it.description.depend,
                        "soft_depends" to it.description.softDepend,
                        "website" to it.description.website
                    )
                }
            )
        )
        context.writeAndFlush(Utils.build200RespByByteBuf(Gson().toJson(optJson).toByteArray()))
            ?.addListener(ChannelFutureListener.CLOSE)
        return
    }
}