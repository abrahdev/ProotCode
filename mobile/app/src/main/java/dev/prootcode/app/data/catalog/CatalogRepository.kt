package dev.prootcode.app.data.catalog

import android.content.res.AssetManager
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

interface CatalogSource {
    fun load(): String
}

class AssetCatalogSource(
    private val assets: AssetManager,
    private val assetName: String = CatalogDefaults.ASSET_NAME,
) : CatalogSource {
    override fun load(): String = assets.open(assetName).bufferedReader().use { it.readText() }
}

class RemoteCatalogSource(
    private val url: String,
    private val connectTimeoutMillis: Int = 10_000,
    private val readTimeoutMillis: Int = 20_000,
    private val maxChars: Int = 2 * 1024 * 1024,
) : CatalogSource {
    override fun load(): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = connectTimeoutMillis
        connection.readTimeout = readTimeoutMillis
        connection.requestMethod = "GET"
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("catalog request failed with ${connection.responseCode}")
            }
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            if (text.length > maxChars) throw IOException("catalog response is too large")
            return text
        } finally {
            connection.disconnect()
        }
    }
}

class CatalogRepository(
    private val cacheFile: File,
    private val remote: CatalogSource?,
    private val bundled: CatalogSource,
) {
    fun load(refresh: Boolean = false): Catalog {
        if (refresh) {
            val json = remote?.let { source -> runCatching { source.load() }.getOrNull() }
            json
                ?.takeIf { it.isNotBlank() }
                ?.let { text ->
                    val catalog = runCatching { CatalogParser.parse(text) }.getOrNull()
                    if (catalog != null) {
                        cacheFile.parentFile?.mkdirs()
                        cacheFile.writeText(text)
                        return catalog
                    }
                }
        }
        if (cacheFile.isFile) {
            runCatching { CatalogParser.parse(cacheFile.readText()) }.getOrNull()?.let { return it }
        }
        return CatalogParser.parse(bundled.load())
    }
}

object CatalogDefaults {
    const val REMOTE_URL = "https://raw.githubusercontent.com/abrahdev/ProotCode/main/catalog/catalog.json"
    const val ASSET_NAME = "catalog.json"
    private const val CACHE_NAME = "catalog.json"

    fun cacheFileFor(cacheDir: File): File = File(cacheDir, CACHE_NAME)

    fun create(
        assets: AssetManager,
        cacheDir: File,
    ): CatalogRepository =
        CatalogRepository(
            cacheFile = cacheFileFor(cacheDir),
            remote = RemoteCatalogSource(REMOTE_URL),
            bundled = AssetCatalogSource(assets),
        )
}
