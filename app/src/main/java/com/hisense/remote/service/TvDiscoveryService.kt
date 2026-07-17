package com.hisense.remote.service

import android.util.Log
import com.hisense.remote.model.DiscoveredTv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Socket

class TvDiscoveryService {
    companion object {
        private const val TAG = "TvDiscovery"
        private const val VIDAA_PORT = 36669
        private const val GOOGLE_ADB_PORT = 5555

        suspend fun discover(timeoutMs: Long = 350): List<DiscoveredTv> = withContext(Dispatchers.IO) {
            val results = mutableListOf<DiscoveredTv>()
            val subnet = getLocalSubnet()
            if (subnet == null) {
                Log.w(TAG, "Could not determine local subnet")
                return@withContext results
            }
            val localIp = getLocalIp()
            Log.d(TAG, "Scanning subnet: $subnet.0/24 (local IP: $localIp)")

            // Scan all 254 IPs in parallel for both Vidaa OS (36669) and Google TV ADB (5555)
            val deferreds = (1..254).map { i ->
                async {
                    val ip = "$subnet.$i"
                    if (ip == localIp) return@async null

                    if (checkPort(ip, VIDAA_PORT, timeoutMs)) {
                        DiscoveredTv(ip = ip, name = "Hisense Vidaa TV ($ip)", port = VIDAA_PORT)
                    } else if (checkPort(ip, GOOGLE_ADB_PORT, timeoutMs)) {
                        DiscoveredTv(ip = ip, name = "Hisense Google TV ($ip) [ADB Mode]", port = GOOGLE_ADB_PORT)
                    } else null
                }
            }

            deferreds.awaitAll().filterNotNull().forEach { results.add(it) }
            Log.d(TAG, "Found ${results.size} TV(s)")
            results
        }

        private suspend fun checkPort(ip: String, port: Int, timeout: Long): Boolean {
            return try {
                val socket = Socket()
                socket.connect(java.net.InetSocketAddress(ip, port), timeout.toInt())
                socket.close()
                true
            } catch (_: Exception) {
                false
            }
        }

        private fun getLocalSubnet(): String? {
            return try {
                val ip = getLocalIp() ?: return null
                val parts = ip.split(".")
                if (parts.size >= 3) "${parts[0]}.${parts[1]}.${parts[2]}" else null
            } catch (_: Exception) { null }
        }

        private fun getLocalIp(): String? {
            return try {
                NetworkInterface.getNetworkInterfaces()?.asSequence()
                    ?.flatMap { it.inetAddresses?.asSequence() ?: emptySequence() }
                    ?.firstOrNull {
                        !it.isLoopbackAddress &&
                        it is InetAddress &&
                        it.hostAddress?.contains(".") == true &&
                        !it.hostAddress!!.startsWith("127.")
                    }
                    ?.hostAddress
            } catch (_: Exception) { null }
        }
    }
}
