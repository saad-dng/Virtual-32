package com.antigravity.virtual32.util

import android.util.Log
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object IpDiscovery {
    fun getBestIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            val ips = mutableListOf<Pair<String, String>>() // Name to IP

            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val name = intf.name.lowercase()
                for (addr in Collections.list(intf.inetAddresses)) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        ips.add(name to addr.hostAddress)
                    }
                }
            }

            // Prefer hotspot or wifi
            val preferred = ips.firstOrNull { (name, _) ->
                name.startsWith("ap") || name.startsWith("softap") || name.startsWith("swlan") || name.startsWith("wlan")
            }
            if (preferred != null) return preferred.second

            // Fallback to non-cellular
            val nonCellular = ips.firstOrNull { (name, _) ->
                !name.startsWith("rmnet") && !name.startsWith("ccmni")
            }
            if (nonCellular != null) return nonCellular.second

            // Last resort
            return ips.firstOrNull()?.second ?: "127.0.0.1"
        } catch (e: Exception) {
            Log.e("IpDiscovery", "Failed to discover IP", e)
            return "127.0.0.1"
        }
    }
}
