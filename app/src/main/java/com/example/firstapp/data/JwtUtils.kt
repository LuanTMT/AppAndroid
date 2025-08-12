package com.example.firstapp.data

import android.util.Base64
import android.util.Log
import org.json.JSONObject

object JwtUtils {
    fun decodePayload(token: String): JSONObject? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payloadSegment = parts[1]
            val decodedBytes = Base64.decode(payloadSegment, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            val decoded = String(decodedBytes)
            JSONObject(decoded)
        } catch (e: Exception) {
            Log.e("JwtUtils", "decodePayload error: ${e.message}", e)
            null
        }
    }

    fun extractUserId(token: String): String? {
        val payload = decodePayload(token) ?: return null
        // Try common claim keys
        val candidates = sequence {
            yield(payload.optString("sub", null))
            yield(payload.optString("userId", null))
            yield(payload.optString("id", null))
            // Nested user object
            yield(payload.optJSONObject("user")?.optString("id", null))
        }
        return candidates.firstOrNull { !it.isNullOrEmpty() }
    }
}


