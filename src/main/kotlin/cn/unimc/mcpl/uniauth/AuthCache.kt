package cn.unimc.mcpl.uniauth

import org.bukkit.entity.Player
import java.net.InetSocketAddress
import java.time.Instant

data class Aid(
    var aid: Int,
    var name: String,
    var timestamp: Long,
    var status: AuthStatus,
    var lastip: InetSocketAddress // TODO ip相关逻辑
)

enum class AuthStatus {
    LOG,
    SCAN,
    ONLINE,
    OFFLINE,
    FAIL
}

object AuthCache {
    private var aid = mutableListOf<Aid>()

    fun create(player: Player): Int {
        return this.create(player.name, player.address!!)
    }

    fun create(name: String, ip: InetSocketAddress): Int {

        this.aid.forEach {
            if (it.name == name) {
                it.timestamp = Instant.now().epochSecond
                it.status = AuthStatus.LOG
                Utils.debugLog("玩家 $name 已有AID ${it.aid}")
                return it.aid
            }
        }
        var aid: Int
        do {
            aid = (1000..9999).random()
        } while (this.getName(aid) != null)
        this.aid.add(Aid(aid, name, Instant.now().epochSecond, AuthStatus.LOG, ip))
        Utils.debugLog("玩家 $name 获得AID $aid")
        return aid
    }

    fun isAuth(name: String): Boolean {
        return this.aid.count { it.name == name && (it.status == AuthStatus.LOG || it.status == AuthStatus.SCAN) } != 1
    }
    fun isAuth(aid: Int): Boolean {
        return this.aid.count { it.aid == aid && (it.status == AuthStatus.LOG || it.status == AuthStatus.SCAN) } != 1
    }

    fun getName(aid: Int): String? {
        this.aid.forEach {
            if (it.aid == aid) return it.name
        }
        return null
    }

    fun getAid(name: String): Int? {
        this.aid.forEach {
            if (it.name == name) return it.aid
        }
        return null
    }

    fun setStatus(aid: Int, status: AuthStatus): Boolean {
        Utils.debugLog("AID $aid 的登录状态被设置为 ${status.name}")
        this.aid.forEach {
            if (it.aid == aid) {
                it.status = status
                it.timestamp = Instant.now().epochSecond
                return true
            }
        }
        return false
    }

    fun setStatus(aid: Int, status: AuthStatus, ip: InetSocketAddress): Boolean {
        Utils.debugLog("AID $aid 的登录状态被设置为 ${status.name}，Lastip被设置为 $ip")
        this.aid.forEach {
            if (it.aid == aid) {
                it.status = status
                it.timestamp = Instant.now().epochSecond
                it.lastip = ip
                return true
            }
        }
        return false
    }

    fun setStatus(name: String, status: AuthStatus): Boolean {
        Utils.debugLog("玩家 $name 的登录状态被设置为 ${status.name}")
        this.aid.forEach {
            if (it.name == name) {
                it.status = status
                it.timestamp = Instant.now().epochSecond
                return true
            }
        }
        return false
    }

    fun setStatus(name: String, status: AuthStatus, ip: InetSocketAddress): Boolean {
        Utils.debugLog("玩家 $name 的登录状态被设置为 ${status.name}，Lastip被设置为 $ip")
        this.aid.forEach {
            if (it.name == name) {
                it.status = status
                it.timestamp = Instant.now().epochSecond
                it.lastip = ip
                return true
            }
        }
        return false
    }

    fun getStatus(name: String): AuthStatus {
        this.aid.forEach {
            if (it.name == name) return it.status
        }
        return AuthStatus.FAIL
    }

    fun getStatus(aid: Int): AuthStatus {
        this.aid.forEach {
            if (it.aid == aid) return it.status
        }
        return AuthStatus.FAIL
    }

    fun checkSession(name: String, ip: InetSocketAddress): Boolean {
        if (Utils.config.getInt("login.session") == 0) return false
        return this.aid.any {
            it.name == name
                    && it.status == AuthStatus.OFFLINE
                    && Instant.now().epochSecond - it.timestamp < Utils.config.getInt("login.session")
                    && it.lastip == ip
        }
    }


    fun getAuthTimeoutPlayers(): List<String> {
        val tPlayer = mutableListOf<String>()
        this.aid.forEach {
            if (
                Instant.now().epochSecond - it.timestamp > Utils.config.getInt("login.timeout")
                && (it.status == AuthStatus.LOG || it.status == AuthStatus.SCAN)
            ) tPlayer.add(it.name)
        }
        return tPlayer
    }
}