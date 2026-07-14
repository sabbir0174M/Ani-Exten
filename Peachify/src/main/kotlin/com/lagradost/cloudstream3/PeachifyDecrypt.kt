package com.lagradost.cloudstream3

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.nio.charset.StandardCharsets

object PeachifyDecrypt {
    
    /**
     * Decrypt Peachify encrypted data
     * Handles both base64 and AES encryption
     */
    fun decryptData(encryptedData: String, key: String = ""): String? {
        return try {
            if (encryptedData.contains("=") || encryptedData.length % 4 == 0) {
                try {
                    val decoded = Base64.decode(encryptedData, Base64.DEFAULT)
                    String(decoded, StandardCharsets.UTF_8)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * AES Decrypt using CBC mode
     */
    fun aesDecrypt(
        encryptedData: String,
        key: String,
        iv: String
    ): String? {
        return try {
            val decodedKey = Base64.decode(key, Base64.DEFAULT)
            val decodedIv = Base64.decode(iv, Base64.DEFAULT)
            val decodedData = Base64.decode(encryptedData, Base64.DEFAULT)

            val keySpec = SecretKeySpec(decodedKey, 0, decodedKey.size, "AES")
            val cipher = Cipher.getInstance("AES")
            
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decrypted = cipher.doFinal(decodedData)
            
            String(decrypted, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse and extract streams from encrypted JSON response
     */
    fun extractStreamsFromEncrypted(encryptedResponse: String): List<StreamSource> {
        val sources = mutableListOf<StreamSource>()
        
        try {
            val decrypted = decryptData(encryptedResponse) ?: return sources
            
            val sourceRegex = "(?:\\"url\\":|\\"src\\":|src=)\\"?([^\\"\\s,}]+)".toRegex()
            val qualityRegex = "(?:\\"quality\\":|\\"resolution\\":|quality=)\\"?(\\d+)".toRegex()
            val typeRegex = "(?:\\"type\\":|\\"format\\":|type=)\\"?([^\\"\\s,}]+)".toRegex()
            val dubRegex = "(?:\\"dub\\":|\\"audio\\":|dub=)\\"?([^\\"\\s,}]+)".toRegex()

            sourceRegex.findAll(decrypted).forEach { match ->
                val url = match.groupValues[1]
                val quality = qualityRegex.find(decrypted)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val type = typeRegex.find(decrypted)?.groupValues?.get(1) ?: "mp4"
                val dub = dubRegex.find(decrypted)?.groupValues?.get(1) ?: "Original"

                if (url.isNotEmpty()) {
                    sources.add(
                        StreamSource(
                            url = url,
                            quality = quality,
                            type = type,
                            dub = dub
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Log error silently
        }
        
        return sources
    }
}

data class StreamSource(
    val url: String,
    val quality: Int,
    val type: String,
    val dub: String
)
