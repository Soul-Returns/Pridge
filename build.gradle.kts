import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
    id("fabric-loom") version "1.13-SNAPSHOT"
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

    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true") // Exports transformed classes for debugging
        runDir = "../../run" // Shares the run directory between versions
    }
}

// Fix for multiple versions using same Minecraft version
abstract class LockService : BuildService<BuildServiceParameters.None>

val genSourcesLock = gradle.sharedServices.registerIfAbsent("genSourcesLock", LockService::class) {
    maxParallelUsages.set(1) // Only allow one genSources task to run at a time
}

afterEvaluate {
    tasks.withType<net.fabricmc.loom.task.GenerateSourcesTask>().configureEach {
        // Disable parallel execution to prevent conflicts when multiple versions share the same Minecraft version
        // This is necessary because loom uses the same cache location for the same MC version
        usesService(genSourcesLock)

        // Also add explicit task ordering to satisfy Gradle's validation
        val currentTaskPath = this.path
        val currentTask = this
        rootProject.subprojects.forEach { subproject ->
            if (subproject != project) {
                subproject.tasks.withType<net.fabricmc.loom.task.GenerateSourcesTask>().configureEach {
                    if (this.path < currentTaskPath) {
                        currentTask.mustRunAfter(this)
                    }
                }
            }
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")

    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")

    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

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