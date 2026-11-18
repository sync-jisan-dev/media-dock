package com.mediadock.app

import com.mediadock.app.core.downloads.TransferSlots
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class TransferSlotsTest {
    @Test fun honorsLimitsAndReleasesCapacity() {
        val slots = TransferSlots(); assertTrue(slots.tryAcquire(2)); assertTrue(slots.tryAcquire(2)); assertFalse(slots.tryAcquire(2))
        slots.release(); assertTrue(slots.tryAcquire(2))
    }
    @Test fun reducingTheLimitWaitsForExistingTransfers() {
        val slots = TransferSlots(); repeat(3) { assertTrue(slots.tryAcquire(3)) }
        assertFalse(slots.tryAcquire(1)); repeat(3) { slots.release() }; assertTrue(slots.tryAcquire(1)); assertFalse(slots.tryAcquire(1))
    }
    @Test fun concurrentStartsCannotExceedTheLimit() {
        val slots = TransferSlots(); val executor = Executors.newFixedThreadPool(12); val start = CountDownLatch(1); val done = CountDownLatch(24); val acquired = AtomicInteger()
        repeat(24) { executor.submit { start.await(); if (slots.tryAcquire(3)) acquired.incrementAndGet(); done.countDown() } }
        start.countDown(); assertTrue(done.await(3, java.util.concurrent.TimeUnit.SECONDS)); executor.shutdownNow(); assertEquals(3, acquired.get())
    }
}
