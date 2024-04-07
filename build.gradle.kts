import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("io.izzel.taboolib") version "2.0.11"
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
}

kotlin {
    sourceSets.all {
        languageSettings {
            languageVersion = "2.0"
        }
    }
}

taboolib {
    env {
        // 安装模块
        install(UNIVERSAL, BUKKIT)
        install(BUKKIT_HOOK, CHAT, CONFIGURATION, LANG, METRICS, NMS, NMS_UTIL)
    }
    version { taboolib = "6.1.1-beta17" }
    description {
        name("UniAuth")
        desc("TabooLib 是一个高效的 Minecraft 插件开发框架")
        contributors {
            name("Cnzw")
        }
        dependencies {
            name("Uniporter")
            name("PlaceholderAPI").optional(true)
        }
        links {
            name("homepage").url("https://ua.unimc.com")
        }
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
}

dependencies {
    compileOnly("ink.ptms:nms-all:1.0.0")
    compileOnly("ink.ptms.core:v11200:11200")
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.github.Apisium:Uniporter:1.3.4-SNAPSHOT")
    compileOnly("com.google.code.gson:gson:2.10.1")
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
