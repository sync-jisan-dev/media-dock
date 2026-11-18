package com.mediadock.app.core.downloads

import java.util.concurrent.atomic.AtomicInteger

/** Counts network transfers, excluding server-side processing and waiting jobs. */
class TransferSlots {
    private val active = AtomicInteger(0)
    fun tryAcquire(limit: Int): Boolean {
        while (true) {
            val count = active.get()
            if (count >= limit.coerceIn(1, 5)) return false
            if (active.compareAndSet(count, count + 1)) return true
        }
    }
    fun release() { active.updateAndGet { (it - 1).coerceAtLeast(0) } }
}
