package com.mediadock.app

import com.mediadock.app.domain.model.PlatformRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlatformRegistryTest {

    @Test
    fun testPlatformMatching() {
        val yt = PlatformRegistry.matchPlatform("https://www.youtube.com/watch?v=12345")
        assertNotNull(yt)
        assertEquals("YouTube", yt?.name)

        val insta = PlatformRegistry.matchPlatform("https://www.instagram.com/reel/abcdef/")
        assertNotNull(insta)
        assertEquals("Instagram", insta?.name)

        val tiktok = PlatformRegistry.matchPlatform("https://vm.tiktok.com/ZM8ABCDEF/")
        assertNotNull(tiktok)
        assertEquals("TikTok", tiktok?.name)

        val x = PlatformRegistry.matchPlatform("https://x.com/user/status/123456789")
        assertNotNull(x)
        assertEquals("X / Twitter", x?.name)

        val unknown = PlatformRegistry.matchPlatform("https://unknown-site-123.com")
        assertNull(unknown)
    }
}
