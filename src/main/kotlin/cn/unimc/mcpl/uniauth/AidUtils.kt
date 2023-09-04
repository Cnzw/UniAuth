package cn.unimc.mcpl.uniauth


object AidUtils {
    // 100-999 偶数就是aid，奇数就是扫码过了
    // 100-998 50-499
    private var aid = mutableMapOf<Int, String>()
    fun addAid(name: String): Int {
        val tAid = (50..499).random() * 2
        this.aid.put(tAid, name)
        return tAid
    }
    fun addAid(id: Int, name: String) {
        this.aid.put(id, name)
    }
    fun delAid(name: String) {
        this.aid.values.remove(name)
    }
    fun delAid(id: Int) {
        this.aid.remove(id)
    }
    fun hasAid(id: Int): Boolean {
        return this.aid.containsKey(id)
    }
    fun getName(id: Int): String {
        return this.aid.getOrDefault(id, "invalid")
    }
    fun plusAid(id: Int) {
        val tName = this.getName(id)
        this.delAid(id)
        this.addAid(id + 1, tName)
    }
    fun hasName(name: String): Boolean {
        return this.aid.values.contains(name)
    }

    private val integerChars = '0'..'9'
    fun isNumber(input: String): Boolean {
        var dotOccurred = 0
        return input.all { it in integerChars || it == '.' && dotOccurred++ < 1 }
    }
    fun isInteger(input: String) = input.all { it in integerChars }
}