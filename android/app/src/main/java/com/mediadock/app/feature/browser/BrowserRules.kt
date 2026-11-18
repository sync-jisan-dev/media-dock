package com.mediadock.app.feature.browser

import com.mediadock.app.core.datastore.WorkspaceRules
import java.net.URLEncoder

object BrowserRules {
    const val MAX_TABS = 12
    fun destination(input: String): String {
        val value = input.trim()
        if (value.isBlank()) return ""
        if (WorkspaceRules.validUrl(value)) return value
        if (!value.contains(' ') && value.contains('.') && WorkspaceRules.validUrl("https://$value")) return "https://$value"
        return "https://duckduckgo.com/?q=${URLEncoder.encode(value, "UTF-8") }"
    }
    fun displayHost(url: String): String = runCatching { java.net.URI(url).host.orEmpty().removePrefix("www.") }.getOrDefault("")
}
