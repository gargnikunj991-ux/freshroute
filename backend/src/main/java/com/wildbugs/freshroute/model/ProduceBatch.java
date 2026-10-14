package com.wildbugs.freshroute.model;

import com.wildbugs.freshroute.model.enums.BatchStatus;
import com.wildbugs.freshroute.model.enums.Commodity;
import com.wildbugs.freshroute.model.enums.ProduceCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "produce_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProduceBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(unique = true, nullable = false)
    private String batchCode;

    private String variety;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Commodity commodity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProduceCategory category;

    @NotNull
    @Column(nullable = false)
    private Double initialQuantityKg;

    @NotNull
    @Column(nullable = false)
    private Double remainingQuantityKg;

    @NotNull
    @Column(nullable = false)
    private Double initialShelfLifeHours;

    @NotNull
    @Column(nullable = false)
    private Double remainingShelfLifeHours;

    @NotNull
    @Column(nullable = false)
    private Double procurementCostPerKg;

    @NotNull
    @Column(nullable = false)
    private Double currentTemperatureCelsius;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status;

    private Instant harvestTimestamp;

    private String originHub;

    public double getUrgencyIndex() {
        return 1.0 / Math.max(1.0, this.remainingShelfLifeHours);
    }
}
