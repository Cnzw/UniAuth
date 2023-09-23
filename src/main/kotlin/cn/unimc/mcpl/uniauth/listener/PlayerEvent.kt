package cn.unimc.mcpl.uniauth.listener

import cn.unimc.mcpl.uniauth.AuthCache
import cn.unimc.mcpl.uniauth.AuthStatus
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
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.console
import taboolib.module.lang.asLangText
import taboolib.module.nms.NMSMap
import taboolib.module.nms.sendMap
import taboolib.platform.util.sendLang
import java.awt.image.BufferedImage

object PlayerEvent {
    @SubscribeEvent
    fun onPlayerQuit(ev: PlayerQuitEvent) {
        if (AuthCache.getStatus(ev.player.name) == AuthStatus.FAIL) return
        if (!AuthCache.isAuth(ev.player.name)) {
            ev.player.removePotionEffect(PotionEffectType.BLINDNESS)
            AuthCache.setStatus(ev.player.name, AuthStatus.FAIL, ev.player.address!!)
        } else {
            AuthCache.setStatus(ev.player.name, AuthStatus.OFFLINE, ev.player.address!!)
        }
    }

    @SubscribeEvent
    fun onAsyncPlayerPreLogin(ev: AsyncPlayerPreLoginEvent) {
        if (ev.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) return

        if (!Utils.compilePattern(Utils.config.getString("login.player-name-regex")!!).matcher(ev.name).matches()) {
            Utils.debugLog("玩家 ${ev.name} 游戏名未通过校验")
            ev.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                console().asLangText("kick-invalid-player-name", ev.name)
            )
        }
        Utils.debugLog("玩家 ${ev.name} 游戏名通过校验")
    }
    @SubscribeEvent
    fun onPlayerLogin(ev: PlayerLoginEvent) {
        if (ev.player.hasPermission("uniauth.login.bypass")) {
            Utils.debugLog("玩家 ${ev.player.name} 拥有绕过权限")
            return
        }

        if (Bukkit.getPlayerExact(ev.player.name) != null) { // TODO 需要测试
            ev.disallow(PlayerLoginEvent.Result.KICK_FULL, "") // TODO Lang
        }
    }
    @SubscribeEvent
    fun onPlayerJoin(ev: PlayerJoinEvent) {
        if (ev.player.hasPermission("uniauth.login.bypass")) {
            Utils.debugLog("玩家 ${ev.player.name} 拥有绕过权限")
            return
        }

        if (AuthCache.checkSession(ev.player.name, ev.player.address!!)) {
            Utils.debugLog("玩家 ${ev.player.name} 通过会话(Session)登录")
            AuthCache.setStatus(ev.player.name, AuthStatus.ONLINE)
            return
        }

        // TODO cmd

        if (Utils.config.getBoolean("login.blind")) {
            ev.player.addPotionEffect(
                PotionEffect(
                    PotionEffectType.BLINDNESS,
                    Utils.config.getInt("login.timeout") * 20,
                    99,
                    false,
                    false
                ) // TODO 1.13特性兼容
            )
        }

        val aid = AuthCache.create(ev.player)

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
        for (x in 0 until width) { // 拿到了二维码坐标，自行填充像素
            for (y in 0 until height) {
                image.setRGB(x, y, if (bitMatrix.get(x, y)) 0 else 16777215)
            }
        }
        ev.player.sendMap(image, NMSMap.Hand.OFF)

        ev.player.sendLang("player-log-prompt", ev.player.name) // 0 player_name 1 lastip
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onPlayerMove(ev: PlayerMoveEvent) {
        if (!AuthCache.isAuth(ev.player.name)) ev.isCancelled = true
    }

    @SubscribeEvent
    fun onPlayerCommandPreprocess(ev: PlayerCommandPreprocessEvent) {
        if (!AuthCache.isAuth(ev.player.name)
            && !Utils.config.getStringList("login.command-whitelist")
                .contains(ev.message.split(" ")[0].lowercase())
        ) {
            Utils.debugLog("玩家 ${ev.player.name} 执行的命令不在白名单内")
            ev.isCancelled = true
        }
    }

    @SubscribeEvent
    fun onPlayerChat(ev: AsyncPlayerChatEvent) {
        if (!Utils.config.getBoolean("login.chat") && !AuthCache.isAuth(ev.player.name)) {
            ev.isCancelled = true
        }
    }
    @SubscribeEvent
    fun onPlayerInteract(ev: PlayerInteractEvent) {
        if (!AuthCache.isAuth(ev.player.name)) ev.isCancelled = true
    }
    @SubscribeEvent
    fun onInventoryOpen(ev: InventoryOpenEvent) {
        if (!AuthCache.isAuth(ev.player.name)) ev.isCancelled = true
    }
    @SubscribeEvent
    fun onInventoryClick(ev: InventoryClickEvent) {
        if (!AuthCache.isAuth(ev.whoClicked.name)) ev.isCancelled = true
    }
    @SubscribeEvent
    fun onEntityDamageByEntity(ev: EntityDamageByEntityEvent) {
        if (!AuthCache.isAuth(ev.damager.name) || !AuthCache.isAuth(ev.entity.name)) {
            ev.isCancelled = true
        }
    }

    @SubscribeEvent
    fun onPlayerTeleport(ev: PlayerTeleportEvent) {
        if (!AuthCache.isAuth(ev.player.name)) ev.isCancelled = true
    }

    @SubscribeEvent
    fun onPlayerDropItem(ev: PlayerDropItemEvent) {
        if (!AuthCache.isAuth(ev.player.name)) ev.isCancelled = true
    }

    @SubscribeEvent
    fun onEntityPickupItem(ev: EntityPickupItemEvent) {
        if (!AuthCache.isAuth(ev.entity.name)) ev.isCancelled = true
    }
    // TODO 看看authme有没有漏的地方
}