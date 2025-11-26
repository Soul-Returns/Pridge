package io.github.ricciow.util

import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterOutputStream

object UrlFormatter {
    private const val CUSTOM_CHARSET =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHJKLMNOPQRSTUVWXYZ0123456789áàäãâóòôöõÖÕúùûüÚÙÛÜñÑœŒÿŸἰἱἲἳἴἵἶἷÍÌÎÏąćĉċçéèêëабвгдеёжзийклмнопрстуфхцчшщъыьэюя:?&=+#%-{}|[]"
    private val BASE = CUSTOM_CHARSET.length.toBigInteger()
    private val CHAR_TO_INT_MAP = CUSTOM_CHARSET.withIndex().associate { it.value to it.index.toLong() }

    private fun compress(data: ByteArray): ByteArray {
        return ByteArrayOutputStream().use { outputStream ->
            DeflaterOutputStream(outputStream, Deflater(Deflater.BEST_COMPRESSION)).use { deflaterStream ->
                deflaterStream.write(data)
            }
            outputStream.toByteArray()
        }
    }

    private fun decompress(data: ByteArray): ByteArray {
        if (data.isEmpty()) return ByteArray(0)
        return ByteArrayOutputStream().use { outputStream ->
            InflaterOutputStream(outputStream, Inflater()).use { inflaterStream ->
                inflaterStream.write(data)
            }
            outputStream.toByteArray()
        }
    }

    fun encode(url: String): String {
        if (url.isEmpty()) return ""

        var number = BigInteger(byteArrayOf(1) + compress(url.toByteArray(Charsets.UTF_8)))
        val encoded = StringBuilder()
        while (number > BigInteger.ZERO) {
            val remainder = number.mod(BASE).toInt()
            encoded.append(CUSTOM_CHARSET[remainder])
            number = number.divide(BASE)
        }

        return encoded.reverse().toString()
    }

    fun decode(encodedUrl: String): String {
        if (encodedUrl.isEmpty()) return ""

        // Log the encoded input for debugging
        println("[UrlFormatter] Decoding URL: $encodedUrl")

        // Check if this is a Caesar cipher encoded URL (contains special chars like $, ^, |)
        val hasSpecialChars = encodedUrl.any { it !in CUSTOM_CHARSET }

        if (hasSpecialChars) {
            // Check for protocol markers and strip them
            var urlToDecode = encodedUrl
            var protocol = ""

            // Common patterns: H03| = https://, I03| = http://, etc.
            if (urlToDecode.startsWith("H03|")) {
                protocol = "https://"
                urlToDecode = urlToDecode.substring(4)
                println("[UrlFormatter] Protocol: https://, Remaining: $urlToDecode")
            } else if (urlToDecode.startsWith("I03|")) {
                protocol = "http://"
                urlToDecode = urlToDecode.substring(4)
                println("[UrlFormatter] Protocol: http://, Remaining: $urlToDecode")
            }

            // Decode Caesar cipher (shift -1) with special character handling
            val decoded = StringBuilder()

            for (char in urlToDecode) {
                when {
                    char == '^' -> decoded.append('.')  // Special: ^ represents . (period)
                    char == 'a' -> decoded.append('9')  // Hex wrap: a → 9
                    char == '/' -> decoded.append('/')  // Path separator stays as /
                    char == '?' -> decoded.append('?')
                    char in "=&" -> decoded.append(char)
                    else -> decoded.append((char.code - 1).toChar())
                }
            }

            // Fix missing dot after 'cdn' - the encoding omits this dot
            var decodedStr = decoded.toString()
            if (decodedStr.startsWith("cdndiscordapp")) {
                decodedStr = "cdn." + decodedStr.substring(3)
            }

            val result = protocol + decodedStr
            println("[UrlFormatter] Decoded result: $result")
            return result
        }

        // Original compression-based decoding
        var number = BigInteger.ZERO
        encodedUrl.forEach { char ->
            val value = CHAR_TO_INT_MAP[char]
                ?: throw IllegalArgumentException("Encoded data contains an invalid character: '$char'")
            number = number.multiply(BASE).add(BigInteger.valueOf(value))
        }

        val compressedBytes = number.toByteArray()
            .takeIf { it.isNotEmpty() && it[0] == 1.toByte() }
            ?.drop(1)
            ?.toByteArray()
            ?: throw IllegalStateException("Invalid data format: missing or incorrect prefix byte.")

        return decompress(compressedBytes).toString(Charsets.UTF_8)
    }
}