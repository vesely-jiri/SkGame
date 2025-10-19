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

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("com.github.SkriptLang:Skript:2.13.0-pre1")
}

tasks {

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    shadowJar {
        archiveBaseName.set("SkGame")
        archiveClassifier.set("")
        archiveVersion.set("")
        //Comment/uncomment this to copy the jar to the plugins folder
        finalizedBy("copyToPlugins")
    }

    register<Copy>("copyToPlugins") {
        dependsOn("shadowJar")
        from(named("shadowJar").map { it.outputs.files})
        into("F:/Projects/Minecraft/Test 1.17/plugins")
    }
}
