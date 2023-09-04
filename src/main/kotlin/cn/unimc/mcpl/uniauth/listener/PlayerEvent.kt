package cn.unimc.mcpl.uniauth.listener

import cn.unimc.mcpl.uniauth.AidUtils
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.*
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
    @SubscribeEvent //TODO
    fun onPlayerJoin(ev: PlayerJoinEvent) {
        val aid = AidUtils.addAid(ev.player.name)
        info("玩家 ${ev.player.name} 的 aid 是 $aid")

        val hints = mapOf<EncodeHintType, Any>(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 0
        )
        val bitMatrix = MultiFormatWriter().encode(
            "https://www.bing.com/search?q=$aid",
            BarcodeFormat.QR_CODE, 128, 128, hints
        )
        val width = bitMatrix.width
        val height = bitMatrix.height
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                image.setRGB(x, y, if(bitMatrix.get(x, y)) 0 else 16777215)
            }
        }
        ev.player.sendMap(image, NMSMap.Hand.OFF)
    }

    @SubscribeEvent //TODO
    fun onPlayerCommandPreprocess(ev: PlayerCommandPreprocessEvent) {
        if (AidUtils.hasName(ev.player.name)) {
            ev.isCancelled
        }
    }
    @SubscribeEvent //TODO 配置文件
    fun onPlayerChat(ev: AsyncPlayerChatEvent) {
        if (AidUtils.hasName(ev.player.name)) {
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