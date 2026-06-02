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

    val customBuildDir = providers.gradleProperty("skgameBuildDir").orNull
    if (customBuildDir != null) {
        layout.buildDirectory.set(file(customBuildDir))
    }

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

        // Keep only realistic server-platform SQLite natives.
        // Supported: Linux x86_64/aarch64 (glibc), Linux-Musl x86_64/aarch64 (Alpine),
        //            Windows x86_64, macOS x86_64/aarch64.
        exclude("org/sqlite/native/Linux-Android/**")
        exclude("org/sqlite/native/FreeBSD/**")
        exclude("org/sqlite/native/Linux/arm/**")
        exclude("org/sqlite/native/Linux/armv6/**")
        exclude("org/sqlite/native/Linux/armv7/**")
        exclude("org/sqlite/native/Linux/x86/**")
        exclude("org/sqlite/native/Linux/ppc64/**")
        exclude("org/sqlite/native/Linux-Musl/x86/**")
        exclude("org/sqlite/native/Windows/aarch64/**")
        exclude("org/sqlite/native/Windows/armv7/**")
        exclude("org/sqlite/native/Windows/x86/**")

        finalizedBy("copyToPlugins")
    }

    // Auto-deploy: set skgameDeployDir=<path/to/plugins> in ~/.gradle/gradle.properties
    val deployDir = providers.gradleProperty("skgameDeployDir").orNull
    register<Copy>("copyToPlugins") {
        dependsOn("shadowJar")
        from(named("shadowJar").map { it.outputs.files })
        into(deployDir ?: temporaryDir)
        onlyIf { deployDir != null }
    }
}
