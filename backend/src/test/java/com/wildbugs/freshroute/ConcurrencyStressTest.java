package com.wildbugs.freshroute;

import com.wildbugs.freshroute.model.ProduceBatch;
import com.wildbugs.freshroute.model.enums.BatchStatus;
import com.wildbugs.freshroute.model.enums.Commodity;
import com.wildbugs.freshroute.model.enums.ProduceCategory;
import com.wildbugs.freshroute.repository.ProduceBatchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ConcurrencyStressTest {

    @Autowired
    private ProduceBatchRepository batchRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("STRESS TEST: 10 concurrent distribution threads allocating from a single 500kg batch")
    void testConcurrentBatchAllocationZeroOversell() throws InterruptedException {
        // Create an isolated limited stock batch of 500 kg
        ProduceBatch batch = ProduceBatch.builder()
                .batchCode("STRESS-CONC-01")
                .variety("Desi Hybrid")
                .commodity(Commodity.TOMATO)
                .category(ProduceCategory.VEGETABLE)
                .initialQuantityKg(500.0)
                .remainingQuantityKg(500.0)
                .initialShelfLifeHours(48.0)
                .remainingShelfLifeHours(36.0)
                .procurementCostPerKg(20.0)
                .currentTemperatureCelsius(12.0)
                .status(BatchStatus.AVAILABLE)
                .harvestTimestamp(Instant.now())
                .originHub("Nashik Test Hub")
                .build();

        batch = batchRepository.saveAndFlush(batch);
        final Long batchId = batch.getId();

        int threadCount = 10;
        double requestedQtyPerThread = 100.0; // 10 * 100 = 1,000 kg requested, but only 500 kg available!

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successfulAllocations = new AtomicInteger(0);
        AtomicInteger rejectedAllocations = new AtomicInteger(0);

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Simultaneous burst firing

                    txTemplate.execute(status -> {
                        // Pessimistic Write Lock: SELECT ... FOR UPDATE
                        ProduceBatch locked = batchRepository.findByIdWithPessimisticLock(batchId)
                                .orElseThrow(() -> new IllegalStateException("Batch not found"));

                        if (locked.getRemainingQuantityKg() >= requestedQtyPerThread) {
                            locked.setRemainingQuantityKg(locked.getRemainingQuantityKg() - requestedQtyPerThread);
                            if (locked.getRemainingQuantityKg() <= 0.0) {
                                locked.setStatus(BatchStatus.FULLY_ALLOCATED);
                            }
                            batchRepository.saveAndFlush(locked);
                            successfulAllocations.incrementAndGet();
                        } else {
                            rejectedAllocations.incrementAndGet();
                        }
                        return null;
                    });

                } catch (Exception e) {
                    rejectedAllocations.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Fire all 10 threads concurrently
        boolean finished = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "All concurrent tasks should complete within timeout");

        ProduceBatch finalBatch = batchRepository.findById(batchId).orElseThrow();

        // Exactly 5 allocations of 100kg should succeed, 5 must be rejected
        assertEquals(5, successfulAllocations.get(), "Exactly 5 allocations must succeed for 500kg inventory");
        assertEquals(5, rejectedAllocations.get(), "5 requests must be rejected due to stock exhaustion");
        assertEquals(0.0, finalBatch.getRemainingQuantityKg(), 0.001, "Remaining quantity must be exactly 0, zero underflow allowed");
        assertEquals(BatchStatus.FULLY_ALLOCATED, finalBatch.getStatus());
    }
}
