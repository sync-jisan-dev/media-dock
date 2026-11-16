package com.mediadock.app.core.network

import com.mediadock.app.BuildConfig

object ApiConstants {
    val DEFAULT_BASE_URL = if (BuildConfig.DEBUG) "http://10.0.2.2:3000/" else "https://media-dock.onrender.com/"
    const val LEGACY_TUNNEL_URL = "https://decorative-needed-toolbox-tyler.trycloudflare.com/"
    const val TIMEOUT_CONNECT_SECONDS = 30L
    const val TIMEOUT_READ_SECONDS = 120L
    const val TIMEOUT_WRITE_SECONDS = 60L
}
