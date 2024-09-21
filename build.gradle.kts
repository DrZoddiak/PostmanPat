plugins {
    id("xyz.jpenilla.run-paper") version "2.3.0"
    kotlin("jvm")
}

group = "me.zodd"
version = "2.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven(url = "https://oss.sonatype.org/content/groups/public/")
    maven (url = "https://repo.essentialsx.net/releases/")
    // DiscordSRV
    maven (url = "https://nexus.scarsz.me/content/repositories/public/")
    // VaultAPI
    maven (url = "https://jitpack.io")

    maven(url = "https://repo.olziedev.com/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly("com.discordsrv:discordsrv:1.28.1")
    compileOnly("net.dv8tion:JDA:5.0.2")
    compileOnly("net.essentialsx:EssentialsX:2.20.1")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("org.spongepowered:configurate-hocon:4.1.2")
    compileOnly("org.spongepowered:configurate-extra-kotlin:4.1.2")
    compileOnly("com.olziedev:playerbusinesses-api:1.5.1")
}

val targetJavaVersion = 21
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

tasks.runServer {
    minecraftVersion("1.21")
}

tasks.compileJava {
    options.encoding = "UTF-8"

    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}


kotlin {
    jvmToolchain(21)
}