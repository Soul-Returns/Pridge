package io.github.ricciow.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.ricciow.config.categories.*

class PridgeConfig : Config() {
    override fun getTitle(): StructuredText {
        return StructuredText.of("Pridge")
    }

    override fun isValidRunnable(runnableId: Int): Boolean {
        return false
    }

    @Expose
    @Category(name = "Guild", desc = "Guild Settings")
    @JvmField
    var guildCategory = GuildCategory()

    @Expose
    @Category(name = "Discord", desc = "Discord Settings")
    @JvmField
    var discordCategory = DiscordCategory()

    @Expose
    @Category(name = "Sounds", desc = "Special Sounds")
    @JvmField
    var soundsCategory = SoundsCategory()

    @Expose
    @Category(name = "Bot", desc = "Bot related Settings")
    @JvmField
    var botCategory = BotCategory()

    @Expose
    @Category(name = "Links", desc = "Link Settings")
    @JvmField
    var linkCategory = LinkCategory()

    @Expose
    @Category(name = "Filters", desc = "Word Filters")
    @JvmField
    var filtersCategory = FiltersCategory()

    @Expose
    @Category(name = "Developer", desc = "Developer mode configurations")
    @JvmField
    var developerCategory = DeveloperCategory()
}