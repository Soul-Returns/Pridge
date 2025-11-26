package io.github.ricciow.format

import io.github.ricciow.Pridge.CONFIG_I
import io.github.ricciow.util.ColorCode
import io.github.ricciow.util.PridgeLogger
import io.github.ricciow.util.TextParser.parse
import io.github.ricciow.util.UrlFormatter
import io.github.ricciow.util.toText
import net.minecraft.text.ClickEvent.OpenUrl
import net.minecraft.text.HoverEvent.ShowText
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import net.minecraft.util.Formatting
import java.net.URI
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

object SpecialFunctions {
    private val registry = mutableMapOf<String, SpecialFunction>()

    private val LINK_PATTERN = Pattern.compile("\\[LINK]\\((l\\$[^)]+)\\)")

    fun initialize() {
        registry.put("discord", SpecialFunctions::discordHandler)
        registry.put("contest1", SpecialFunctions::contest1Handler)
        registry.put("contest2", SpecialFunctions::contest2Handler)
        registry.put("contest3", SpecialFunctions::contest3Handler)
        registry.put("contest4", SpecialFunctions::contest4Handler)
        registry.put("bestiary", SpecialFunctions::bestiaryHandler)
        registry.put("bestiary2", SpecialFunctions::bestiary2Handler)
        registry.put("collection", SpecialFunctions::collectionHandler)
    }

    /**
     * Gets a function from the corresponding name
     * @param functionName Name of the function
     * @return An optional containing a single function to be run with .run()
     */
    fun get(functionName: String): SpecialFunction? {
        return registry[functionName]
    }

    /**
     * Runs a function with the corresponding name
     * @param functionName Name of the function to be run
     * @param matcher RegExp matcher to be passed through the function
     * @return String containing the result of the function or null if there isn't a function
     */
    fun run(functionName: String, originalText: String, matcher: Matcher): FormatResult? {
        val function = get(functionName) ?: return null
        return function.run(originalText, matcher)
    }

    //<editor-fold desc="Helper Methods">
    /**
     * Formats a time value, returning an empty string if the value is 0.
     * @param timeValue The numeric value of the time unit.
     * @param suffix The suffix to append (e.g., "h", "m", "s").
     * @return A formatted string like " 10h" or an empty string.
     */
    private fun timeFunc(timeValue: Int, suffix: String): String {
        if (timeValue == 0) return ""
        return " $timeValue$suffix"
    }

    /**
     * Capitalizes the first letter of a string.
     * @param str The string to capitalize.
     * @return The capitalized string.
     */
    private fun capitalizeFirstLetter(str: String?): String? {
        if (str == null || str.isEmpty()) return str
        return str.substring(0, 1).uppercase(Locale.getDefault()) + str.substring(1)
    }

    /**
     * Placeholder for a function that formats hyperlinks.
     * In a real mod, this would involve creating chat components with click events.
     * @return A formatted string or null if no links are found.
     */
    private fun formatLink(originalText: String, matcher: Matcher): Text {
        if (CONFIG_I.linkCategory.enabled) {
            var representation = CONFIG_I.linkCategory.representation
            val group = matcher.group(1)
            // Remove the "l$" prefix before decoding
            val encodedUrl = group.removePrefix("l$")
            val url = try {
                UrlFormatter.decode(encodedUrl)
            } catch (e: Exception) {
                PridgeLogger.error("Failed to decode URL: $group", e)
                representation = "&a&l[Failed to decode URL]"
                "https://github.com/Ricciow/Pridge-1.21.5/issues"
            }
            return parse(representation).apply {
                style = Style.EMPTY
                    .withClickEvent(OpenUrl(URI(url)))
                    .withHoverEvent(ShowText(url.toText()))
            }
        }

        return parse(originalText)
    }

    private fun contest1Handler(originalText: String, matcher: Matcher): FormatResult {
        val crop = matcher.group(1)
        val hours = matcher.group(2).toInt()
        val minutes = matcher.group(3).toInt()
        val seconds = matcher.group(4).toInt()

        val hoursStr = timeFunc(hours, "h")
        val minutesStr = timeFunc(minutes, "m")
        val secondsStr = timeFunc(seconds, "s")

        return FormatResult("&eNext $crop contest in&f$hoursStr$minutesStr$secondsStr", botText = true)
    }

    private fun contest2Handler(originalText: String, matcher: Matcher): FormatResult {
        val crop1 = matcher.group(1)
        val crop2 = matcher.group(2)
        val crop3 = matcher.group(3)
        // Groups 4 (minutesActive) and 5 (secondsActive) are unused in the final format string.
        val nextCrop = matcher.group(6)
        val hours = matcher.group(7).toInt()
        val minutes = matcher.group(8).toInt()
        val seconds = matcher.group(9).toInt()

        val hoursStr = timeFunc(hours, "h")
        val minutesStr = timeFunc(minutes, "m")
        val secondsStr = timeFunc(seconds, "s")

        return FormatResult(
            "\n &a&lActive Contest\n &6$crop1, $crop2, $crop3\n&eNext $nextCrop contest in&f$hoursStr$minutesStr$secondsStr",
            botText = true
        )
    }

    private fun contest3Handler(originalText: String, matcher: Matcher): FormatResult {
        val crop1 = matcher.group(1)
        val crop2 = matcher.group(2)
        val crop3 = matcher.group(3)
        // Groups 4 (minutesActive) and 5 (secondsActive) are unused.
        val crop4 = matcher.group(6)
        val crop5 = matcher.group(7)
        val crop6 = matcher.group(8)
        val hours = matcher.group(9).toInt()
        val minutes = matcher.group(10).toInt()
        val seconds = matcher.group(11).toInt()

        val hoursStr = timeFunc(hours, "h")
        val minutesStr = timeFunc(minutes, "m")
        val secondsStr = timeFunc(seconds, "s")

        return FormatResult(
            " &a&lActive Contest\n &6$crop1, $crop2, $crop3\n&eNext: \n &6$crop4, $crop5, $crop6\n &eIn&f$hoursStr$minutesStr$secondsStr",
            botText = true
        )
    }

    private fun contest4Handler(originalText: String, matcher: Matcher): FormatResult {
        val crop1 = matcher.group(1)
        val crop2 = matcher.group(2)
        val crop3 = matcher.group(3)
        val hours = matcher.group(4).toInt()
        val minutes = matcher.group(5).toInt()
        val seconds = matcher.group(6).toInt()

        val hoursStr = timeFunc(hours, "h")
        val minutesStr = timeFunc(minutes, "m")
        val secondsStr = timeFunc(seconds, "s")

        return FormatResult(
            " &e&lNext:\n &6$crop1, $crop2, $crop3\n &eIn&f$hoursStr$minutesStr$secondsStr",
            botText = true
        )
    }

    private fun bestiaryHandler(originalText: String, matcher: Matcher): FormatResult {
        val maxPerPage = CONFIG_I.botCategory.getLineCount()

        val bestiary = matcher.group(1)
        val user = matcher.group(2)
        val profile = matcher.group(3)
        val message = matcher.group(4)

        // A single, more robust pattern to capture all data points at once
        val bestiaryDataPattern = Pattern.compile("(\\w[\\w\\s]*?) (\\d+)/(\\d+)(?: \\(([\\d.]+)\\))?")

        val prefix = parse("\n &6&l$bestiary bestiary - &f&l$user (&f&l$profile)&6&l\n ")
        val pages = mutableListOf<Text>()
        val currentPageContent = StringBuilder()
        var entriesOnPage = 0

        // Use the single pattern on the whole message
        val entryMatcher = bestiaryDataPattern.matcher(message)
        while (entryMatcher.find()) {
            if (entriesOnPage == maxPerPage) {
                pages.add(parse(currentPageContent.toString()))
                currentPageContent.setLength(0) // More efficient than new StringBuilder()
                entriesOnPage = 0
            }

            val name = entryMatcher.group(1).trim()
            val current = entryMatcher.group(2)
            val max = entryMatcher.group(3)
            val kdrString = entryMatcher.group(4) // Can be null

            if (entriesOnPage != 0) {
                currentPageContent.append("\n")
            }

            currentPageContent.append(" &e&l$name &f&l$current&e&l/&f&l$max")

            if (kdrString != null) {
                val kdr = kdrString.toDouble()
                val color = when (kdr) {
                    in 1.00..Double.MAX_VALUE -> ColorCode.GREEN.getMcCode()
                    in 0.66..1.00 -> ColorCode.YELLOW.getMcCode()
                    in 0.33..0.66 -> ColorCode.GOLD.getMcCode()
                    else -> ColorCode.RED.getMcCode()
                }
                currentPageContent.append(" &e&l($color&l$kdrString&e&l)")
            } else {
                currentPageContent.append(" &e&l(&a&lPro&e&l)")
            }
            entriesOnPage++
        }

        // Add the final page if it has any content
        if (currentPageContent.isNotEmpty()) {
            if (!pages.isEmpty()) {
                while (entriesOnPage < maxPerPage) {
                    currentPageContent.append("\n")
                    entriesOnPage += 1
                }
            }
            pages.add(parse(currentPageContent.toString()))
        }

        val titles = mutableListOf<Text>()
        for (i in 1..pages.size) {
            titles.add(parse("&6Page ($i/${pages.size})"))
        }

        return FormatResult(
            pages, titles, TextColor.fromFormatting(Formatting.DARK_AQUA)!!,
            TextColor.fromFormatting(Formatting.GRAY)!!, prefix, botText = true
        )
    }

    private fun bestiary2Handler(originalText: String, matcher: Matcher): FormatResult {
        val mob = matcher.group(1)
        val user = matcher.group(2)
        val profile = matcher.group(3)
        val num = matcher.group(4)

        val str = if (num.toInt() > 0) "&a&lPro" else "&4&l0"

        return FormatResult(
            "&f&l$user (&f&l$profile)&6&l:\n &6&l$mob - &f&l$num&e&l/&f&l0 &e&l($str&e&l)",
            botText = true
        )
    }

    private fun collectionHandler(originalText: String, matcher: Matcher): FormatResult {
        val maxPerPage = CONFIG_I.botCategory.getLineCount()

        val skill = matcher.group(1)
        val user = matcher.group(2)
        val profile = matcher.group(3)
        val message = matcher.group(4)

        // 1. Replaced the two patterns with a single, more direct one.
        // This pattern captures all necessary groups in one pass.
        val dataPattern = Pattern.compile("([\\w\\s]*) (\\d+)/(\\d+) \\(([^)]+)\\)")

        val prefix = parse("\n &6&l${capitalizeFirstLetter(skill)} collections - &f&l$user (&f&l$profile)&6&l\n ")

        val pages = mutableListOf<Text>()
        val currentPageContent = StringBuilder()
        var entriesOnPage = 0

        // 2. The loop is now simpler, with no nested matching.
        val entryMatcher = dataPattern.matcher(message)
        while (entryMatcher.find()) {
            if (entriesOnPage == maxPerPage) {
                pages.add(parse(currentPageContent.toString()))
                currentPageContent.setLength(0)
                entriesOnPage = 0
            }

            // 3. All data is retrieved directly from the single 'entryMatcher'.
            val name = entryMatcher.group(1).trim()
            val current = entryMatcher.group(2).toInt()
            val max = entryMatcher.group(3).toInt()
            val progress = entryMatcher.group(4) // e.g., "1,234" or "1,234/5,678"

            if (entriesOnPage != 0) {
                currentPageContent.append("\n")
            }

            val median = Math.floorDiv(max, 4)
            val numberColor = if (current == max) {
                ColorCode.GREEN
            } else if (current > median) {
                ColorCode.GOLD
            } else {
                ColorCode.RED
            }

            val formattedProgress = progress.replace("/", "&e&l/&f&l")

            currentPageContent.append(
                " &e&l$name ${numberColor.getMcCode()}&l$current&e&l/${numberColor.getMcCode()}&l$max &e&l(&f&l$formattedProgress&e&l)"
            )

            entriesOnPage++
        }

        // This logic for handling the final page remains the same.
        if (currentPageContent.isNotEmpty()) {
            if (!pages.isEmpty()) {
                while (entriesOnPage < maxPerPage) {
                    currentPageContent.append("\n")
                    entriesOnPage++
                }
            }
            pages.add(parse(currentPageContent.toString()))
        }

        // This logic for generating titles remains the same.
        val titles = mutableListOf<Text>()
        for (i in 1..pages.size) {
            titles.add(parse("&6Page ($i/${pages.size})"))
        }

        // The return statement is the same.
        return FormatResult(
            pages, titles, TextColor.fromFormatting(Formatting.DARK_AQUA)!!,
            TextColor.fromFormatting(Formatting.GRAY)!!, prefix, botText = true
        )
    }

    private fun discordHandler(originalText: String, matcher: Matcher): FormatResult {
        val user = matcher.group(1)
        var message = matcher.group(2)
        val userName: String?

        // This logic handles cases where the message contains ": " and the regex captures it as part of the user.
        // It correctly reassembles the user and message parts.
        if (user.contains(": ")) {
            val parts = user.split(": ")
            userName = parts[0]
            val messageBuilder = StringBuilder()
            for (i in 1..<parts.size) {
                messageBuilder.append(parts[i]).append(": ")
            }
            messageBuilder.append(message)
            message = messageBuilder.toString()
        } else {
            userName = user
        }

        val discord = CONFIG_I.discordCategory
        val finalMessage = parse(
            "${discord.nameColor.get().getMcCode()}$userName${discord.messageColor.get().getMcCode()}: "
        )

        val parts = message.split(" ")
        var starting = true
        for (part in parts) {
            if (part.isEmpty()) {
                continue
            }

            val linkMatcher = LINK_PATTERN.matcher(part)
            //Treat links
            val formattedPart = if (linkMatcher.matches()) {
                formatLink(part, linkMatcher)
            } else {
                parse("${CONFIG_I.discordCategory.messageColor.get().getMcCode()}$part")
            }

            //Add to the end of the message;
            if (!starting) {
                finalMessage.append(" ")
            } else {
                starting = false
            }
            finalMessage.append(formattedPart)
        }

        return FormatResult(finalMessage, discordText = true)
    }

    fun interface SpecialFunction {
        fun run(originalText: String, matcher: Matcher): FormatResult
    }
}