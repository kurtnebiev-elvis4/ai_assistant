package com.mycelium.myapplication.data.model

data class ServerEntry(
    val id: String = "",
    val name: String,
    val runpodId: String,
    val port: Int = 8000,
    val isCustom: Boolean = false,
    val isOnline: Boolean = false,
    val lastChecked: Long = 0
) {
    val serverUrl: String
        get() = "https://$runpodId-$port.proxy.runpod.net/"
}