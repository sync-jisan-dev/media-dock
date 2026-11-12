package com.mediadock.app.core.network

object ApiConstants {
    // 10.0.2.2 points to host machine loopback inside standard Android emulators
    const val DEFAULT_BASE_URL = "http://10.0.2.2:3001/"
    const val TIMEOUT_CONNECT_SECONDS = 30L
    const val TIMEOUT_READ_SECONDS = 60L
    const val TIMEOUT_WRITE_SECONDS = 60L
}
