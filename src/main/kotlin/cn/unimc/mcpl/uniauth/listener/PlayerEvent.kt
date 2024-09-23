package cn.unimc.mcpl.uniauth.listener

import cn.unimc.mcpl.uniauth.AuthState
import cn.unimc.mcpl.uniauth.PlayerAuthStateUtils
import cn.unimc.mcpl.uniauth.UniAuth
import cn.unimc.mcpl.uniauth.Utils
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.lang.sendLang
import taboolib.module.nms.NMSMap
import taboolib.module.nms.sendMap
import taboolib.platform.type.BukkitProxyEvent
import java.awt.image.BufferedImage

data class UniAuthLoginEvent(
    val player: Player,
    val code: Int // 0=跳过登录 1=Session 登录
): BukkitProxyEvent()

data class UniAuthLogoutEvent(
    val player: Player,
    val state: AuthState
): BukkitProxyEvent()

object PlayerEvent {
    @SubscribeEvent
    fun onPlayerJoin(ev: PlayerJoinEvent) {
        if (!UniAuth.config.getBoolean("login.enable")) return

        if (ev.player.hasPermission("uniauth.login.bypass")) {
            Utils.debugLog("玩家 ${ev.player.name} 拥有权限 uniauth.login.bypass，跳过登录")
            UniAuthLoginEvent(ev.player, 0).call()
            return
        }

        if (PlayerAuthStateUtils.checkSession(ev.player.name, ev.player.address)) {
            Utils.debugLog("玩家 ${ev.player.name} 通过 Session 登录")
            PlayerAuthStateUtils.setStateOnline(ev.player.name)
            adaptPlayer(ev.player).sendLang("player-login-session", ev.player.name)
            UniAuthLoginEvent(ev.player, 1).call()
            return
        }

        if (UniAuth.config.getBoolean("login.blind")) {
            ev.player.addPotionEffect(
                PotionEffect(
                    PotionEffectType.BLINDNESS,
                    UniAuth.config.getInt("login.timeout") * 20,
                    99,
                    false,
                    false
                )
            )
        }

        val tacode = PlayerAuthStateUtils.addAcode(ev.player.name, ev.player.address)
        Utils.debugLog("玩家 ${ev.player.name} Acode $tacode 状态改变，为 LOGIN")

        val hints = mapOf<EncodeHintType, Any>(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 0
        )
        val bitMatrix = MultiFormatWriter().encode(
            String.format(UniAuth.config.getString("login.qrcode-url")!!, tacode, UniAuth.config.getString("login.server-id")),
            BarcodeFormat.QR_CODE,
            128,
            128,
            hints
        )
        val width = bitMatrix.width
        val height = bitMatrix.height
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until width) { // 拿到了二维码坐标，自行填充像素
            for (y in 0 until height) {
                image.setRGB(x, y, if (bitMatrix.get(x, y)) 0 else 16777215)
            }
        }

        ev.player.sendMap(image, NMSMap.Hand.OFF) // TODO
        UniAuthLoginEvent(ev.player, tacode).call()

        adaptPlayer(ev.player).sendLang("player-login-prompt", ev.player.name)
    }

    @SubscribeEvent
    fun onPlayerQuit(ev: PlayerQuitEvent) {
        if (!UniAuth.config.getBoolean("login.enable")) return

        if (PlayerAuthStateUtils.checkState(ev.player.name) == AuthState.ONLINE) {
            PlayerAuthStateUtils.setStateOffline(ev.player.name)
            Utils.debugLog("玩家 ${ev.player.name} 状态改变，为 OFFLINE")
            UniAuthLogoutEvent(ev.player, AuthState.OFFLINE).call()
        } else {
            ev.player.removePotionEffect(PotionEffectType.BLINDNESS)
            PlayerAuthStateUtils.setStateFail(ev.player.name)
            Utils.debugLog("玩家 ${ev.player.name} 状态改变，为 FAIL")
            UniAuthLogoutEvent(ev.player, AuthState.FAIL).call()
        }
    }
}