package cn.unimc.mcpl.uniauth

import java.net.InetSocketAddress

enum class AuthState {
    LOGIN,
    SCAN,
    ONLINE,
    OFFLINE,
    FAIL
}

data class PlayerAuthState(
    var acode: Int,
    var name: String,
    var timestamp: Long,
    var state: AuthState,
    var lastip: InetSocketAddress
)

// AuthCode - 记录玩家状态
object PlayerAuthStateUtils {
    private val playerAuthStateList = mutableListOf<PlayerAuthState>()

    fun addAcode(name: String, ip: InetSocketAddress): Int {
        // 检查玩家是否已存在 如果存在则改变acode
        this.playerAuthStateList.forEach { it ->
            if (it.name == name) {
                // 生成不重复的acode
                do {
                    it.acode = (1000..9999).random()
                } while (this.playerAuthStateList.any { it.acode == (1000..9999).random() })
                it.timestamp = System.currentTimeMillis()
                it.state = AuthState.LOGIN
                it.lastip = ip
                return it.acode
            }
        }
        var tacode: Int
        do {
            tacode = (1000..9999).random()
        } while (this.playerAuthStateList.any { it.acode == (1000..9999).random() })
        if (name == "KID1412") tacode = 8888 // 开发调试用
        this.playerAuthStateList.add(
            PlayerAuthState(tacode, name, System.currentTimeMillis(), AuthState.LOGIN, ip)
        )
        return tacode
    }

    fun checkSession(name: String, ip: InetSocketAddress): Boolean {
        if (!UniAuth.config.getBoolean("login.session")) return false

        return this.playerAuthStateList.any{
            it.name == name
                    && it.state == AuthState.OFFLINE
                    && it.lastip == ip
                    && System.currentTimeMillis() - it.timestamp < UniAuth.config.getInt("login.session-timeout") * 1000
        }
    }

    fun checkState(name: String): AuthState {
        return this.playerAuthStateList.firstOrNull { it.name == name }?.state ?: AuthState.OFFLINE
    }

    fun setStateOnline(name: String) {
        this.playerAuthStateList.forEach {
            if (it.name == name) {
                it.state = AuthState.ONLINE
                it.timestamp = System.currentTimeMillis()
            }
        }
    }

    fun setStateOffline(name: String) {
        this.playerAuthStateList.forEach {
            if (it.name == name) {
                it.state = AuthState.OFFLINE
                it.timestamp = System.currentTimeMillis()
            }
        }
    }
    fun setStateFail(name: String) {
        this.playerAuthStateList.forEach {
            if (it.name == name) {
                it.state = AuthState.FAIL
                it.timestamp = System.currentTimeMillis()
            }
        }
    }

    fun setStateScan(name: String) {
        this.playerAuthStateList.forEach {
            if (it.name == name) {
                it.state = AuthState.SCAN
                it.timestamp = System.currentTimeMillis()
            }
        }
    }

    fun getAuthTimeoutPlayers(): List<PlayerAuthState> {
        return this.playerAuthStateList.filter {
            (it.state == AuthState.LOGIN || it.state == AuthState.SCAN)
                    && System.currentTimeMillis() - it.timestamp > UniAuth.config.getInt("login.timeout") * 1000
        }
    }

    fun getStateLoginPlayers(): List<PlayerAuthState> {
        return this.playerAuthStateList.filter {
            it.state == AuthState.LOGIN
        }
    }

    fun getStateScanPlayers(): List<PlayerAuthState> {
        return this.playerAuthStateList.filter {
            it.state == AuthState.SCAN
        }
    }
}