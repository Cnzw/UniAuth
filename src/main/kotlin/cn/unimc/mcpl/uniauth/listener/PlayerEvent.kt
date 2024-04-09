package cn.unimc.mcpl.uniauth.listener

import cn.unimc.mcpl.uniauth.AuthState
import cn.unimc.mcpl.uniauth.PlayerAuthStateUtils
import cn.unimc.mcpl.uniauth.UniAuth
import cn.unimc.mcpl.uniauth.Utils
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.lang.sendLang
import taboolib.module.nms.NMSMap
import taboolib.module.nms.sendMap
import java.awt.image.BufferedImage

// TODO 直接不监听
object PlayerEvent {
    @SubscribeEvent
    fun onPlayerJoin(ev: PlayerJoinEvent) {
        if (!UniAuth.config.getBoolean("login.enable")) return

        if (ev.player.hasPermission("uniauth.login.bypass")) {
            Utils.debugLog("玩家 ${ev.player.name} 拥有权限 uniauth.login.bypass，跳过登录")
            return
        }

        if (PlayerAuthStateUtils.checkSession(ev.player.name, ev.player.address)) {
            Utils.debugLog("玩家 ${ev.player.name} 通过 Session 登录")
            PlayerAuthStateUtils.setStateOnline(ev.player.name)
            adaptPlayer(ev.player).sendLang("player-login-session", ev.player.name)
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
        Utils.debugLog("玩家 ${ev.player.name} ACode已生成，为 $tacode")

        val hints = mapOf<EncodeHintType, Any>(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 0
        )
        val bitMatrix = MultiFormatWriter().encode(
            String.format(UniAuth.config.getString("login.qrcode-url")!!, tacode),
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

        adaptPlayer(ev.player).sendLang("player-login-prompt", ev.player.name)
    }

    @SubscribeEvent
    fun onPlayerQuit(ev: PlayerJoinEvent) {
        if (!UniAuth.config.getBoolean("login.enable")) return

        if (PlayerAuthStateUtils.checkState(ev.player.name) == AuthState.ONLINE) {
            PlayerAuthStateUtils.setStateOffline(ev.player.name)
        } else {
            ev.player.removePotionEffect(PotionEffectType.BLINDNESS)
            PlayerAuthStateUtils.setStateFail(ev.player.name)
        }
    }
}