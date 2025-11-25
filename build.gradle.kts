import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    id("dev.kikugie.stonecutter") version "0.7.11"
}

group = property("maven_group")!!
version = property("mod_version")!!

java.toolchain.languageVersion = JavaLanguageVersion.of(21)
kotlin.compilerOptions.jvmTarget.set(JvmTarget.JVM_21)

repositories {
    maven("https://maven.notenoughupdates.org/releases/") {
        name = "not-enough-updates"
    }

    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1") {
        name = "DevAuth"
    }
}

loom {
    accessWidenerPath.set(file("src/main/resources/pridge.accesswidener"))
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")

    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")

    // Extra fabric api modules
    val apiModules = setOf(
        "fabric-resource-loader-v0",
        "fabric-lifecycle-events-v1",
        "fabric-events-interaction-v0",
        "fabric-command-api-v2",
        "fabric-registry-sync-v0",
        "fabric-rendering-v1",
        "fabric-message-api-v1"
    )

    apiModules.forEach {
        modImplementation(fabricApi.module(it, property("fapi_version").toString()))
    }

    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_main_api_version")}")

    modImplementation("org.notenoughupdates.moulconfig:${property("moulconfig_version")}")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:${property("devauth_version")}")
}

tasks {
    processResources {
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand(getProperties())
            expand(mutableMapOf("version" to project.version))
        }
    }
}