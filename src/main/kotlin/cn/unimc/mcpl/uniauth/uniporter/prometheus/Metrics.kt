package cn.unimc.mcpl.uniauth.uniporter.prometheus

import cn.apisium.uniporter.router.api.Route
import cn.apisium.uniporter.router.api.UniporterHttpHandler
import cn.unimc.mcpl.uniauth.UniAuth
import cn.unimc.mcpl.uniauth.Utils
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import io.prometheus.metrics.core.metrics.Gauge
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter
import io.prometheus.metrics.model.registry.PrometheusRegistry
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import taboolib.common.platform.function.console
import taboolib.common.platform.function.pluginVersion
import taboolib.module.lang.sendWarn
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress

object Metrics : UniporterHttpHandler {

    val memory: Gauge = Gauge.builder()
        .name("memory")
        .help("Memory")
        .labelNames("type")
        .register()

    val onlinePlayers: Gauge = Gauge.builder()
        .name("online_players")
        .help("Online player count")
        .labelNames("world")
        .register()

    val tps: Gauge = Gauge.builder()
        .name("tps")
        .help("TPS")
        .register()

    val chunks: Gauge = Gauge.builder()
        .name("loaded_chunks")
        .help("Loaded chunks count")
        .labelNames("world")
        .register()

    val entities: Gauge = Gauge.builder()
        .name("entities")
        .help("Entities count")
        .labelNames("world", "type")
        .register()

    fun collectMetrics() {
        memory.labelValues("max").set(Runtime.getRuntime().maxMemory().toDouble() / 1048576)
        memory.labelValues("free").set(Runtime.getRuntime().freeMemory().toDouble() / 1048576)
        memory.labelValues("total").set(Runtime.getRuntime().totalMemory().toDouble() / 1048576)
        memory.labelValues("used").set((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()).toDouble() / 1048576)

        val entityStrings = UniAuth.config.getStringList("prometheus.entities")
        for (world in Bukkit.getWorlds()) {
            val allEntities = world.entities

            if (entityStrings.isNotEmpty()) {
                if (entityStrings.contains("all")) {
                    val entitiesMap = allEntities.groupBy { it.type.name }
                    for ((entityType, entitiesObj) in entitiesMap) {
                        entities.labelValues(world.name, entityType.lowercase()).set(entitiesObj.size.toDouble())
                    }
                } else {
                    for (entityString in entityStrings) {
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val cls = Class.forName("org.bukkit.entity.${entityString}") as Class<out Entity>
                            val filteredEntities = allEntities.filter { it.javaClass == cls }
                            entities.labelValues(world.name, entityString.lowercase()).set(filteredEntities.size.toDouble())
                        } catch (e: ClassNotFoundException) {
                            console().sendWarn("console-promotheus-entity-not-found", entityString)
                            continue
                        }
                    }
                }
            }

            entities.labelValues(world.name, "total").set(allEntities.size.toDouble())
            onlinePlayers.labelValues(world.name).set(world.players.size.toDouble())
            chunks.labelValues(world.name).set(world.loadedChunks.size.toDouble())
        }

        if (UniAuth.PapiEnabled && PlaceholderAPI.isRegistered("server")) {
            tps.set(PlaceholderAPI.setPlaceholders(null, "%server_tps_1%").replace("*", "").toDouble())
        } else {
            console().sendWarn("console-papi-not-load")
            tps.set(20.0)
        }
    }

    override fun handle(path: String?, route: Route?, context: ChannelHandlerContext?, request: FullHttpRequest?) {
        // 访问日志
        val inSocket: InetSocketAddress = context?.channel()?.remoteAddress() as InetSocketAddress
        Utils.debugLog(inSocket.hostName + " - " + request?.method()?.name() + " " + path)

        if (!UniAuth.config.getBoolean("prometheus.enable")) {
            context.writeAndFlush(Utils.build404Resp())?.addListener(ChannelFutureListener.CLOSE)
            return
        }
        // 验证 Header Authorization
        if (!Utils.verifyReqHandler(request?.headers())) {
            context.writeAndFlush(Utils.build401Resp())?.addListener(ChannelFutureListener.CLOSE)
            return
        }
        // 构建返回
        collectMetrics()

        val metricsTextFormat = PrometheusTextFormatWriter(true)
        val outputStream = ByteArrayOutputStream()
        metricsTextFormat.write(outputStream, PrometheusRegistry.defaultRegistry.scrape())
        // 输出返回
        val response: FullHttpResponse = DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK,
            Unpooled.copiedBuffer(outputStream.toByteArray())
        )
        response.headers()
            .set("x-uniauth-version", pluginVersion)
            .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
        context.writeAndFlush(response)
            ?.addListener(ChannelFutureListener.CLOSE)
        return
    }
}