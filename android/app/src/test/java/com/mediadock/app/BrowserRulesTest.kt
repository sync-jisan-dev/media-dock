package com.mediadock.app

import com.mediadock.app.feature.browser.BrowserRules
import org.junit.Assert.*
import org.junit.Test

class BrowserRulesTest {
    @Test fun preservesSignedWebUrls() { assertEquals("https://example.com/v?token=a+b%2Fc", BrowserRules.destination("https://example.com/v?token=a+b%2Fc")) }
    @Test fun completesDomainsAndEncodesSearchTerms() {
        assertEquals("https://example.com/path", BrowserRules.destination("example.com/path"))
        assertEquals("https://duckduckgo.com/?q=music+%26+travel", BrowserRules.destination("music & travel"))
        assertEquals("", BrowserRules.destination("   "))
    }
    @Test fun unsupportedSchemesAreSearchesInsteadOfNavigation() { assertTrue(BrowserRules.destination("javascript:alert(1)").startsWith("https://duckduckgo.com/")) }
    @Test fun displaysHostWithoutQueryOrCredentials() { assertEquals("example.com", BrowserRules.displayHost("https://www.example.com/watch?token=hidden")) }
}
