package cn.unimc.mcpl.uniauth.listener

import cn.unimc.mcpl.uniauth.AidUtils
import cn.unimc.mcpl.uniauth.Utils
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import org.bukkit.Bukkit
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.*
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info
import taboolib.module.nms.NMSMap
import taboolib.module.nms.sendMap
import java.awt.image.BufferedImage

object PlayerEvent {
    @SubscribeEvent
    fun onPlayerQuit(ev: PlayerQuitEvent) {
        AidUtils.delAid(ev.player.name)
    }

    @SubscribeEvent
    fun onAsyncPlayerPreLogin(ev: AsyncPlayerPreLoginEvent) {
        if (ev.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) return

        if (Utils.compilePattern(Utils.config.getString("login.player-name-regex")!!).matcher(ev.name).matches()) {
            ev.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "") // TODO Lang
        }
    }
    @SubscribeEvent
    fun onPlayerLogin(ev: PlayerLoginEvent) {
        if (ev.player.hasPermission("uniauth.login.bypass")) return

        if (Bukkit.getPlayerExact(ev.player.name) != null) {
            ev.disallow(PlayerLoginEvent.Result.KICK_FULL, "") // TODO Lang
        }
    }
    @SubscribeEvent
    fun onPlayerJoin(ev: PlayerJoinEvent) {
        if (ev.player.hasPermission("uniauth.login.bypass")) return

        // TODO Session

        // TODO 登录超时处理

        // TODO cmd

        if (Utils.config.getBoolean("login.blind")) {
            ev.player.addPotionEffect(
                PotionEffect(
                    PotionEffectType.BLINDNESS,
                    Utils.config.getInt("login.timeout") * 20,
                    2,
                )
            )
        }

        val aid = AidUtils.addAid(ev.player.name)
        info("玩家 ${ev.player.name} 的 aid 是 $aid") // TODO debug

        val hints = mapOf<EncodeHintType, Any>(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 0
        )
        val bitMatrix = MultiFormatWriter().encode(
            "https://www.bing.com/search?q=$aid", //TODO 等小程序
            BarcodeFormat.QR_CODE, 128, 128, hints
        )
        val width = bitMatrix.width
        val height = bitMatrix.height
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                image.setRGB(x, y, if (bitMatrix.get(x, y)) 0 else 16777215)
            }
        }
        ev.player.sendMap(image, NMSMap.Hand.OFF)

        // TODO lang
    }

    @SubscribeEvent //TODO
    fun onPlayerCommandPreprocess(ev: PlayerCommandPreprocessEvent) {
        if (AidUtils.hasName(ev.player.name)) {
            ev.isCancelled
        }
    }
    @SubscribeEvent //TODO 配置文件
    fun onPlayerChat(ev: AsyncPlayerChatEvent) {
        if (!Utils.config.getBoolean("login.chat") && AidUtils.hasName(ev.player.name)) {
            ev.isCancelled
        }
    }
    @SubscribeEvent
    fun onPlayerInteract(ev: PlayerInteractEvent) {
        if (AidUtils.hasName(ev.player.name)) {
            ev.isCancelled
        }
    }
    @SubscribeEvent
    fun onInventoryOpen(ev: InventoryOpenEvent) {
        if (AidUtils.hasName(ev.player.name)) {
            ev.isCancelled
        }
    }
    @SubscribeEvent
    fun onInventoryClick(ev: InventoryClickEvent) {
        if (AidUtils.hasName(ev.whoClicked.name)) {
            ev.isCancelled
        }
    }
    @SubscribeEvent
    fun onEntityDamageByEntity(ev: EntityDamageByEntityEvent) {
        if (AidUtils.hasName(ev.damager.name) || AidUtils.hasName(ev.entity.name)) {
            ev.isCancelled
        }
    }
    @SubscribeEvent
    fun onPlayerTeleport(ev: PlayerTeleportEvent) {
        if (AidUtils.hasName(ev.player.name)) {
            ev.isCancelled
        }
    }
    @SubscribeEvent
    fun onPlayerDropItem(ev: PlayerDropItemEvent) {
        if (AidUtils.hasName(ev.player.name)) {
            ev.isCancelled
        }
    }
    @SubscribeEvent
    fun onEntityPickupItem(ev: EntityPickupItemEvent) {
        if (AidUtils.hasName(ev.entity.name)) {
            ev.isCancelled
        }
    }
}