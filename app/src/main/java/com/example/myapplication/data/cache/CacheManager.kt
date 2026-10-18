package com.example.myapplication.data.cache

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CacheSnapshot<T>(
    val data: List<T>,
    val timestamp: Long
)

class CacheManager(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun <T> saveCache(key: String, data: List<T>, serializer: (List<T>) -> String) {
        try {
            val file = File(context.cacheDir, "cache_$key.json")
            val metaFile = File(context.cacheDir, "meta_$key.txt")
            val jsonStr = serializer(data)
            file.writeText(jsonStr)
            metaFile.writeText(System.currentTimeMillis().toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun <T> loadCache(key: String, deserializer: (String) -> List<T>): CacheSnapshot<T>? {
        return try {
            val file = File(context.cacheDir, "cache_$key.json")
            val metaFile = File(context.cacheDir, "meta_$key.txt")
            if (!file.exists()) return null

            val jsonStr = file.readText()
            val timestamp = if (metaFile.exists()) metaFile.readText().toLongOrNull() ?: 0L else 0L
            val list = deserializer(jsonStr)
            CacheSnapshot(list, timestamp)
        } catch (e: Exception) {
            null
        }
    }

    fun getFormattedLastSync(timestamp: Long): String {
        if (timestamp == 0L) return "Never synced"
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
