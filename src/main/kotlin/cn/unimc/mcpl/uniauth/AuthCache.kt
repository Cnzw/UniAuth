package cn.unimc.mcpl.uniauth

import org.bukkit.entity.Player
import java.net.InetSocketAddress
import java.time.Instant

data class Aid(
    var aid: Int,
    var name: String,
    var timestamp: Long,
    var status: AuthStatus,
    var lastip: InetSocketAddress
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
        val tAid = (100..999).random()
        this.create(tAid, player)
        return tAid
    }

    fun create(aid: Int, player: Player): Int {
        this.aid.add(Aid(aid, player.name, Instant.now().epochSecond, AuthStatus.LOG, player.address!!))
        return aid
    }

    fun isAuth(name: String): Boolean {
        return this.aid.any { it.name == name && (it.status == AuthStatus.LOG || it.status == AuthStatus.SCAN) }
    }

    fun isAuth(aid: Int): Boolean {
        return this.aid.any { it.aid == aid && (it.status == AuthStatus.LOG || it.status == AuthStatus.SCAN) }
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
        this.aid.forEach {
            if (it.aid == aid) {
                it.status = status
                it.timestamp = Instant.now().epochSecond
                return true
            }
        }
        return false
    }

    fun setStatus(name: String, status: AuthStatus): Boolean {
        this.aid.forEach {
            if (it.name == name) {
                it.status = status
                it.timestamp = Instant.now().epochSecond
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

    fun checkSession(name: String): Boolean {
        if (Utils.config.getInt("login.session") == 0) return false
        return this.aid.any {
            it.name == name
                    && it.status == AuthStatus.OFFLINE
                    && Instant.now().epochSecond - it.timestamp < Utils.config.getInt("config.session")
        }
    }

    fun getAuthTimeoutPlayers(): List<String> {
        var tPlayer = mutableListOf<String>()
        this.aid.forEach {
            if (
                Instant.now().epochSecond - it.timestamp > Utils.config.getInt("config.timeout")
                && (it.status == AuthStatus.LOG || it.status == AuthStatus.SCAN)
            ) tPlayer.add(it.name)
        }
        return tPlayer
    }
}