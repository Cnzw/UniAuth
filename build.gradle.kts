import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import io.izzel.taboolib.gradle.*
import io.izzel.taboolib.gradle.Basic
import io.izzel.taboolib.gradle.Bukkit
import io.izzel.taboolib.gradle.BukkitHook
import io.izzel.taboolib.gradle.BukkitNMS
import io.izzel.taboolib.gradle.BukkitNMSUtil
import io.izzel.taboolib.gradle.MinecraftChat
import io.izzel.taboolib.gradle.Metrics
import io.izzel.taboolib.gradle.BukkitUtil
import io.izzel.taboolib.gradle.CommandHelper


plugins {
    java
    id("io.izzel.taboolib") version "2.0.18"
    id("org.jetbrains.kotlin.jvm") version "1.8.22"
}

taboolib {
    env {
        install(Basic)
        install(Bukkit)
        install(BukkitHook)
        install(BukkitNMS)
        install(BukkitNMSUtil)
        install(MinecraftChat)
        install(Metrics)
        install(BukkitUtil)
        install(CommandHelper)
    }
    description {
        name = "UniAuth"
        desc("TabooLib 是一个高效的 Minecraft 插件开发框架")
        contributors {
            name("Cnzw")
        }
        links {
            name("https://ua.unimc.com")
        }
        dependencies {
            name("Uniporter")
            name("PlaceholderAPI").optional(true)
        }
    }
    version { taboolib = "6.2.0-beta14" }
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
}

dependencies {
    compileOnly("ink.ptms.core:v11200:11200")
    compileOnly("ink.ptms:nms-all:1.0.0")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.Apisium:Uniporter:1.3.4-SNAPSHOT")
    compileOnly("com.google.code.gson:gson:2.11.0")
    taboo("com.google.zxing:javase:3.5.3")
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
