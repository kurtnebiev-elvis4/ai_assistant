package com.mycelium.myapplication.data.model

data class ServerStatus(
    val isOnline: Boolean = false,
    val lastChecked: Long = 0
)

data class ServerEntry(
    val name: String,
    val runpodId: String,
    val port: Int = 8000,
) {
    val serverUrl: String
        get() = "https://$runpodId-$port.proxy.runpod.net/"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ServerEntry) return false
        return this.serverUrl == other.serverUrl
    }

    override fun hashCode(): Int = serverUrl.hashCode()
}