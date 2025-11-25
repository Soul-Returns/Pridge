package io.github.ricciow

import io.github.notenoughupdates.moulconfig.managed.ManagedConfig
import io.github.ricciow.command.CommandManager
import io.github.ricciow.config.PridgeConfig
import io.github.ricciow.format.FormatManager
import io.github.ricciow.format.SpecialFunctions
import io.github.ricciow.rendering.ImagePreviewRenderer
import io.github.ricciow.sounds.DynamicSoundPlayer
import io.github.ricciow.util.BOT_IGN_MISSING
import io.github.ricciow.util.ChatUtils
import io.github.ricciow.util.PridgeLogger
import io.github.ricciow.util.TextParser
import kotlinx.io.IOException
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Identifier
import java.nio.file.Files
import java.nio.file.Path

object Pridge : ClientModInitializer {
    const val MOD_ID = "pridge"
    val CONFIG_DIR: Path = FabricLoader.getInstance().configDir.resolve(MOD_ID)

    inline val mc: MinecraftClient
        get() = MinecraftClient.getInstance()

    lateinit var CONFIG: ManagedConfig<PridgeConfig>
    inline val CONFIG_I: PridgeConfig
        get() = CONFIG.instance // since instance is mutable, we cant just have it as a variable

    override fun onInitializeClient() {
        initializeConfig()

        PridgeLogger.info("Initializing...")

        // Initialize object data
        FormatManager.initialize()
        ChatManager.initialize()
        DynamicSoundPlayer.initialize()
        SpecialFunctions.initialize()
        CommandManager.initialize()

        try {
            Files.createDirectories(CONFIG_DIR.resolve("sounds"))
        } catch (e: IOException) {
            PridgeLogger.error("Failed to create sounds directory: ${e.message}", e)
        }

        val imagePreviewLayer = Identifier.of("image-preview-mod", "preview-layer")
        val imagePreviewRenderer = ImagePreviewRenderer()

        @Suppress("DEPRECATION")
        HudRenderCallback.EVENT.register { context, tickCounter ->
            imagePreviewRenderer.onHudRender(context, tickCounter)
        }

        PridgeLogger.info("Initialized successfully!")
    }

    private fun initializeConfig() {
        //Load the config
        val configFile = CONFIG_DIR.resolve("settings.json").toFile()

        CONFIG = ManagedConfig.create(configFile, PridgeConfig::class.java)

        //Add shutdown Hook to save the config
        ClientLifecycleEvents.CLIENT_STOPPING.register { _ ->
            PridgeLogger.info("Shutting down, saving config...")
            CONFIG.saveToFile()
        }

        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            if (CONFIG_I.developerCategory.enabled && CONFIG_I.botCategory.ign.isEmpty()) {
                ChatUtils.sendMessage(
                    TextParser.parse(
                        """
                            &c&l[Pridge] Bridge bot IGN not configured!
                            &c&l[Pridge] config->botCategory->ign
                            &c&l[Pridge] It's required for full functionality.
                        """.trimIndent()
                    ), BOT_IGN_MISSING
                )
            }
        }
    }
}