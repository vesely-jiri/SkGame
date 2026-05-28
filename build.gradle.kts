plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

group = "cz.nox.skgame"
version = "1.0.0"
description = "Skript addon for creating, managing and handling minigames"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains:annotations:23.0.0")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("com.github.SkriptLang:Skript:2.13.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation("dev.dejvokep:boosted-yaml:1.3.7")
}

val gitSha: String = try {
    ProcessBuilder("git", "rev-parse", "--short", "HEAD")
        .redirectErrorStream(true)
        .start()
        .inputStream
        .bufferedReader()
        .readLine()
        ?.trim() ?: "unknown"
} catch (e: Exception) {
    "unknown"
}

tasks {

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
        filesMatching("build-info.properties") {
            expand("version" to project.version, "gitSha" to gitSha)
        }
        from("scripts") {
            into("scripts")
        }
    }

    shadowJar {
        archiveBaseName.set("SkGame")
        archiveClassifier.set("")
        archiveVersion.set("")
        relocate("dev.dejvokep.boostedyaml", "cz.nox.skgame.lib.boostedyaml")
        //Comment/uncomment this to copy the jar to the plugins folder
        finalizedBy("copyToPlugins")
    }

    register<Copy>("copyToPlugins") {
        dependsOn("shadowJar")
        from(named("shadowJar").map { it.outputs.files})
        into("F:/Projects/Minecraft/Test 1.17/plugins")
    }
}
