package io.github.ricciow.rendering

import io.github.ricciow.Pridge.mc
import io.github.ricciow.util.PridgeLogger
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderTickCounter
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.util.InputUtil
import net.minecraft.text.ClickEvent.OpenUrl
import net.minecraft.util.Identifier
import org.apache.commons.io.IOUtils
import org.lwjgl.glfw.GLFW
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern

class ImagePreviewRenderer {

    private var currentUrl: String? = null
    private var loadingUrl: String? = null

    private var imageWidth = 100
    private var imageHeight = 100
    private var hasTexture = false

    fun onHudRender(drawContext: DrawContext, tickCounter: RenderTickCounter) {
        if (!mc.inGameHud.chatHud.isChatFocused) {
            if (this.hasTexture) {
                clearTexture()
            }
            return
        }

        val mouseX = mc.mouse.x * mc.window.scaledWidth.toDouble() / mc.window.width
        val mouseY = mc.mouse.y * mc.window.scaledHeight.toDouble() / mc.window.height
        val style = mc.inGameHud.chatHud.getTextStyleAt(mouseX, mouseY)

        var url: String? = null
        if (style != null) {
            val clickEvent = style.getClickEvent()
            if (clickEvent is OpenUrl) {
                url = clickEvent.uri().toString()
            }
        }

        handleUrl(url)

        if (this.hasTexture) {
            renderPreview(drawContext)
        }
    }

    private fun handleUrl(url: String?) {
        var url = url
        if (url == null) {
            if (this.currentUrl != null) {
                this.currentUrl = null
                clearTexture()
            }
            return
        }
        if (!url.startsWith("http")) {
            return
        }
        if (url.contains("imgur.com/") && !url.contains("i.imgur")) {
            url = "https://i.imgur.com/${url.split("/").last()}.png"
        }
        if (url == this.currentUrl || url == this.loadingUrl) {
            return
        }
        this.loadingUrl = url
        this.currentUrl = url
        clearTexture()
        CompletableFuture.runAsync { loadImage(url) }
    }

    private fun loadImage(url: String) {
        var connection: HttpURLConnection? = null

        try {
            val uri = URI(url)
            val urlConnection = uri.toURL().openConnection() as HttpURLConnection
            connection = urlConnection.apply {
                setRequestProperty("User-Agent", "Pridge Image Previewer/1.0")
                instanceFollowRedirects = true
                connectTimeout = 5000
                readTimeout = 5000
            }

            connection.inputStream.use { stream ->
                val contentType = connection.getHeaderField("Content-Type").orEmpty()

                if ("text/html" in contentType) {
                    val body = IOUtils.toString(stream, StandardCharsets.UTF_8)
                    val imageURL = extractImageUrl(body, url)
                    if (imageURL != null) {
                        loadImage(imageURL)
                        return
                    }
                }

                val image = NativeImage.read(stream)
                mc.execute {
                    if (url == this.loadingUrl) {
                        registerTexture(image)
                        this.loadingUrl = null
                    } else {
                        image.close()
                    }
                }
            }
        } catch (e: Exception) {
            PridgeLogger.error("Failed to load image preview from $url: ${e.message}", e)
            if (url == this.loadingUrl) {
                this.loadingUrl = null
            }
        } finally {
            connection?.disconnect()
        }
    }

    private fun extractImageUrl(html: String, baseUrl: String): String? {
        val matcher = OGP_IMAGE_REGEX.matcher(html)
        val imageUrl = when {
            matcher.find() -> matcher.group("url")
            IMG_TAG_REGEX.matcher(html).find() -> IMG_TAG_REGEX.matcher(html).run {
                find()
                group("url")
            }

            else -> null
        } ?: return null

        return if (imageUrl.startsWith("/")) {
            val baseUri = URI.create(baseUrl)
            "${baseUri.scheme}://${baseUri.host}$imageUrl"
        } else imageUrl
    }


    private fun registerTexture(image: NativeImage) {
        this.imageWidth = image.width
        this.imageHeight = image.height
        val texture = NativeImageBackedTexture({ PREVIEW_TEXTURE_ID.toString() }, image)
        mc.textureManager.registerTexture(PREVIEW_TEXTURE_ID, texture)
        this.hasTexture = true
    }

    private fun clearTexture() {
        if (this.hasTexture) {
            this.hasTexture = false
            mc.execute {
                if (mc.textureManager.getTexture(PREVIEW_TEXTURE_ID) != null) {
                    mc.textureManager.destroyTexture(PREVIEW_TEXTURE_ID)
                }
            }
        }
    }

    private fun renderPreview(drawContext: DrawContext) {
        val screenWidth = mc.window.scaledWidth
        val screenHeight = mc.window.scaledHeight
        val aspectRatio = imageWidth.toFloat() / imageHeight
        var desiredWidth = screenWidth * 0.5f

        val handle = mc.window.handle
        if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SHIFT) &&
            InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_CONTROL)
        ) {
            desiredWidth = screenWidth * 0.75f
        } else {
            if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SHIFT)) {
                desiredWidth = screenWidth * 0.25f
            }

            if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_CONTROL)) {
                desiredWidth = this.imageWidth.toFloat()
            }
        }

        var finalWidth = desiredWidth
        var finalHeight = finalWidth / aspectRatio
        if (finalHeight > screenHeight) {
            finalHeight = screenHeight.toFloat()
            finalWidth = finalHeight * aspectRatio
        }
        val intWidth = finalWidth.toInt()
        val intHeight = finalHeight.toInt()
        drawContext.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            PREVIEW_TEXTURE_ID,
            0, 0,
            0.0f, 0.0f,
            intWidth, intHeight,
            intWidth, intHeight
        )
    }

    companion object {
        private val OGP_IMAGE_REGEX =
            Pattern.compile("<meta property=\"(?:og:image|twitter:image)\" content=\"(?<url>.+?)\".*?/?>")
        private val IMG_TAG_REGEX = Pattern.compile("<img.*?src=\"(?<url>.+?)\".*?>")
        private val PREVIEW_TEXTURE_ID = Identifier.of("image_preview", "preview_texture")
    }
}