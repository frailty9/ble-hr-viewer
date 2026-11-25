package io.github.frailty9.blehr.data.heartrate

/** Controls whether a manual scan may reconnect the most recently used device. */
internal class ManualScanReconnectPolicy {
    private var suppressedAfterExplicitDisconnect = false
    private var pendingAddress: String? = null

    fun beginScan(recentAddress: String?, connected: Boolean) {
        pendingAddress = recentAddress?.takeUnless { connected || suppressedAfterExplicitDisconnect }
    }

    fun findMatch(scannedAddresses: Iterable<String>): String? {
        val target = pendingAddress ?: return null
        val match = scannedAddresses.firstOrNull { it.equals(target, ignoreCase = true) } ?: return null
        pendingAddress = null
        return match
    }

    fun endScan() {
        pendingAddress = null
    }

    fun onExplicitDisconnect() {
        suppressedAfterExplicitDisconnect = true
        endScan()
    }

    fun onExplicitConnect() {
        suppressedAfterExplicitDisconnect = false
        endScan()
    }
}
