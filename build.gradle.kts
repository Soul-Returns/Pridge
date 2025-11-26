plugins {
    java
    id("fabric-loom") version "1.13-SNAPSHOT"
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    id("dev.kikugie.stonecutter") version "0.7.11"
}

version = "${property("mod.version")}+${stonecutter.current.version}"
base.archivesName = property("archives_base_name") as String


val requiredJava = when {
    stonecutter.eval(stonecutter.current.version, ">=1.21.5") -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_1_8
}

repositories {
    maven("https://maven.notenoughupdates.org/releases/") {
        name = "not-enough-updates"
    }

    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1") {
        name = "DevAuth"
    }
}

afterEvaluate {
    // Remove the -dev classifier from the jar
    tasks.named<Jar>("jar") {
        archiveClassifier.set("")
    }
}

dependencies {
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

    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")

    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")

    apiModules.forEach {
        modImplementation(fabricApi.module(it, property("fabric_api") as String))
    }

    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")

    modImplementation("org.notenoughupdates.moulconfig:${property("moulconfig_version")}")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:${property("devauth_version")}")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json") // Useful for interface injection
    accessWidenerPath = rootProject.file("src/main/resources/pridge.accesswidener")

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true") // Exports transformed classes for debugging
        runDir = "../../run" // Shares the run directory between versions
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
}

tasks {
    processResources {
        inputs.property("id", project.property("mod.id"))
        inputs.property("name", project.property("mod.name"))
        inputs.property("version", project.property("mod.version"))
        inputs.property("minecraft", project.property("mod.mc_dep"))

        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "minecraft" to project.property("mod.mc_dep")
        )

        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile }, remapSourcesJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}
