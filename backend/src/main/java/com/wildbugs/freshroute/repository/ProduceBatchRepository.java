package com.wildbugs.freshroute.repository;

import com.wildbugs.freshroute.model.ProduceBatch;
import com.wildbugs.freshroute.model.enums.BatchStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProduceBatchRepository extends JpaRepository<ProduceBatch, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM ProduceBatch b WHERE b.id = :id")
    Optional<ProduceBatch> findByIdWithPessimisticLock(@Param("id") Long id);

    @Query("SELECT b FROM ProduceBatch b WHERE b.remainingQuantityKg > 0 ORDER BY b.remainingShelfLifeHours ASC")
    List<ProduceBatch> findAllActiveFefoOrder();

    @Query("SELECT b FROM ProduceBatch b WHERE b.remainingQuantityKg > 0 ORDER BY b.harvestTimestamp ASC")
    List<ProduceBatch> findAllActiveFifoOrder();

    List<ProduceBatch> findByStatus(BatchStatus status);

    Optional<ProduceBatch> findByBatchCode(String batchCode);
}
