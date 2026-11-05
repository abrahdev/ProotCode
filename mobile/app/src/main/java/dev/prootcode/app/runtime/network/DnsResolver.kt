package dev.prootcode.app.runtime.network

import android.content.Context
import android.net.ConnectivityManager

object DnsResolver {
    private val FALLBACK = listOf("8.8.8.8", "1.1.1.1")

    fun servers(context: Context): List<String> {
        val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
        val network = manager?.activeNetwork ?: return FALLBACK
        val properties = manager.getLinkProperties(network) ?: return FALLBACK
        return properties.dnsServers.mapNotNull { it.hostAddress }.ifEmpty { FALLBACK }
    }
}
