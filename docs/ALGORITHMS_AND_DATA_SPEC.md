# FreshRoute: Algorithmic Specification, Data Models & API Contracts

This technical dossier details the formal algorithmic implementations, mathematical equations, data schemas, and REST API specifications governing the FreshRoute Decision Support Platform.

---

## 1. Mathematical Algorithms & Formulations

### 1.1 Perishability-Aware Allocation (FEFO + Margin Optimization)
Unlike standard FIFO (First-In, First-Out) which only tracks ingestion timestamp, FreshRoute implements **FEFO (First-Expired, First-Out) with Margin Maximization**:

1. **Perishability Urgency Index ($U_i$):**
   $$U_i = \frac{1}{\max(1.0, RSL_i)}$$
   Where $RSL_i$ is the remaining shelf life in hours. Higher $U_i$ values receive highest dispatch priority.

2. **Feasibility Validation Check:**
   For a candidate allocation of batch $i$ to destination $j$:
   $$\text{Feasible}(i, j) = \begin{cases} 
   \text{True} & \text{if } RSL_i - (T_{i, j}^{\text{transit}} + \Delta_{\text{delay}}) \ge S_j^{\text{min\_buffer}} \\
   \text{False} & \text{otherwise}
   \end{cases}$$
   Where:
   - $T_{i, j}^{\text{transit}}$ is the standard transit duration (hours).
   - $\Delta_{\text{delay}}$ is the active disruption delay (hours).
   - $S_j^{\text{min\_buffer}}$ is the destination's mandatory retail shelf-life buffer (e.g., 24h for supermarkets, 8h for juice/food processing).

3. **Net Marginal Contribution Matrix ($M_{i, j}$):**
   $$M_{i, j} = \left[ P_j \cdot (1 - \delta_{i, j}) \right] - C_{i, j}^{\text{freight}} - C_i^{\text{procurement}} - C^{\text{handling}}$$
   - $P_j$: Destination market spot price per kg.
   - $\delta_{i, j}$: Dynamic discount applied.
   - $C_{i, j}^{\text{freight}}$: Freight cost per kg.
   - $C_i^{\text{procurement}}$: Farmer acquisition cost per kg.
   - $C^{\text{handling}}$: Hub cold-storage and loading fee per kg.

4. **Iterative Allocation Algorithm:**
   - **Step 1:** Sort all active batches in descending order of Urgency $U_i$ (lowest remaining shelf life first).
   - **Step 2:** For each batch $i$, filter candidate destinations $j \in D$ where $\text{Feasible}(i, j) == \text{True}$ and remaining demand $D_j > 0$.
   - **Step 3:** Rank candidate destinations by Net Marginal Contribution $M_{i, j}$ descending.
   - **Step 4:** Allocate $x_{i, j} = \min(Q_i^{\text{rem}}, D_j^{\text{rem}}, \text{VehicleCapacity}_{i, j}^{\text{rem}})$.
   - **Step 5:** Decrement $Q_i^{\text{rem}}$, $D_j^{\text{rem}}$, and vehicle payload. Repeat until batch is exhausted or no feasible retail markets remain.
   - **Step 6 (Salvage Clearance):** If $Q_i^{\text{rem}} > 0$ and batch cannot reach retail markets before expiry, trigger dynamic markdown $\delta = 35\%$ and route to proximate `FOOD_PROCESSOR` clusters to prevent zero-revenue spoilage.

---

### 1.2 Dynamic Discounting (Decay Curve Engine)
Produce prices in conventional markets remain static until decay is visible, resulting in unsellable waste. FreshRoute applies an algorithmic progressive markdown curve based on the **Shelf-Life Consumption Ratio ($\rho_i$)**:

$$\rho_i(j) = \frac{T_{i, j}^{\text{transit}} + S_j^{\text{retail\_window}}}{RSL_i}$$

$$\delta_{i, j} = \begin{cases}
0.00 & \text{if } \rho_i(j) < 0.60 \quad \text{(Safe zone: Prime freshness)} \\
0.15 & \text{if } 0.60 \le \rho_i(j) < 0.80 \quad \text{(Velocity discount: Accelerate sales)} \\
0.35 & \text{if } \rho_i(j) \ge 0.80 \quad \text{(Salvage liquidation: Food processors/juicers)}
\end{cases}$$

---

### 1.3 Arrhenius Biochemical Temperature Decay Model
When a cold-chain disruption or reefer failure occurs, produce decay accelerates exponentially according to the empirical food science **$Q_{10}$ temperature coefficient**:

$$\text{DecayRateMultipler} = Q_{10}^{\frac{T_{\text{ambient}} - T_{\text{optimal}}}{10}}$$
For tomatoes and subtropical fruits, $Q_{10} \approx 2.2$.
- Optimal cold storage: $+12^\circ\text{C}$
- Heatwave / Reefer failure: $+22^\circ\text{C}$
- $\Delta T = 10^\circ\text{C} \implies \text{Decay Rate increases by } 2.2\times$, reducing remaining shelf life from 72h down to ~32.7h. FreshRoute immediately triggers emergency re-allocation.

---

## 2. Relational Schema & Entity Specifications

```
  ┌───────────────────────┐             ┌─────────────────────────┐
  │     ProduceBatch      │             │    DestinationMarket    │
  ├───────────────────────┤             ├─────────────────────────┤
  │ id: Long (PK)         │             │ id: Long (PK)           │
  │ batchCode: String     │             │ name: String            │
  │ commodity: Enum       │             │ tier: Enum              │
  │ initialQuantityKg: Dbl│             │ locationCity: String    │
  │ remainingQtyKg: Dbl   │             │ minShelfLifeReqHours:Dbl│
  │ harvestTimestamp: Inst│             └───────────┬─────────────┘
  │ rslHours: Double      │                         │ 1
  │ procurementCostPerKg  │                         │
  │ status: Enum          │                         │
  └──────────┬────────────┘                         │
             │ 1                                    │
             │                                      │ *
             │ *                           ┌────────┴─────────────┐
  ┌──────────▼────────────┐                │    TransportRoute    │
  │   DispatchAllocation  │                ├──────────────────────┤
  │ ───────────────────── │                │ id: Long (PK)        │
  │ id: Long (PK)         │                │ originHub: String    │
  │ batchId: Long (FK)    │                │ destinationId: Long  │
  │ destinationId:Long(FK)│◄───────────────┤ transitTimeHours: Dbl│
  │ allocatedQtyKg: Double│                │ freightCostPerKg: Dbl│
  │ basePricePerKg: Double│                │ capacityKg: Double   │
  │ discountPercent: Dbl  │                │ activeDelayHours: Dbl│
  │ netMarginPerKg: Double│                └──────────────────────┘
  │ totalNetRevenue: Dbl  │
  │ rslOnArrivalHours: Dbl│
  │ decisionRationale: Str│
  └───────────────────────┘
```

---

## 3. REST API Contracts (OpenAPI / Swagger 3.0)

### 3.1 Get Dashboard Overview & Metrics
- **Endpoint:** `GET /api/v1/dashboard/overview`
- **Response `200 OK`:**
```json
{
  "totalInventoryKg": 32000.0,
  "activeBatchesCount": 12,
  "averageRslHours": 44.5,
  "totalDestinations": 6,
  "highRiskBatchesCount": 3,
  "baseline": {
    "totalSpoilageKg": 5800.0,
    "spoilageRatePercent": 18.1,
    "demandFulfillmentPercent": 74.2,
    "totalRevenue": 845000.0,
    "netProfit": 215000.0
  },
  "freshRoute": {
    "totalSpoilageKg": 1600.0,
    "spoilageRatePercent": 5.0,
    "demandFulfillmentPercent": 96.5,
    "totalRevenue": 1120000.0,
    "netProfit": 465000.0
  },
  "improvement": {
    "spoilageReductionPercent": 42.4,
    "netProfitGainInr": 250000.0,
    "netProfitGainPercent": 116.2
  }
}
```

### 3.2 Execute Optimization Run
- **Endpoint:** `POST /api/v1/allocation/optimize`
- **Request Body (Optional overrides):**
```json
{
  "enforceBufferHours": 12.0,
  "enableDynamicDiscount": true,
  "salvageProcessingTier": true
}
```
- **Response `200 OK`:**
```json
{
  "status": "OPTIMIZATION_SUCCESS",
  "dispatchesCount": 18,
  "totalAllocatedKg": 30400.0,
  "totalSpoiledKg": 1600.0,
  "allocations": [
    {
      "allocationId": 101,
      "batchCode": "BATCH-TOM-01",
      "commodity": "TOMATO",
      "quantityKg": 3500.0,
      "destinationName": "Jaipur Modern Retail Hub",
      "transitTimeHours": 14.0,
      "rslOnArrivalHours": 28.0,
      "discountPercent": 0.0,
      "sellingPricePerKg": 48.0,
      "netMarginPerKg": 19.5,
      "totalNetRevenue": 68250.0,
      "decisionRationale": "FEFO Rank 1: High urgency batch routed to Jaipur to maximize net margin before shelf life crosses threshold."
    }
  ]
}
```

### 3.3 Trigger Disruption Simulation ("What-If" Studio)
- **Endpoint:** `POST /api/v1/disruption/simulate`
- **Request Body:**
```json
{
  "scenarioType": "TRANSIT_DELAY", // or "HEATWAVE_TEMPERATURE_SPIKE"
  "routeId": 3,
  "delayHours": 6.0,
  "temperatureSpikeCelsius": 0.0
}
```
- **Response `200 OK`:**
```json
{
  "scenario": "TRANSIT_DELAY (+6.0h on Nashik-Delhi Corridor)",
  "affectedBatchesCount": 4,
  "originalPlanSpoilageLossKg": 4200.0,
  "replannedSpoilageLossKg": 400.0,
  "spoilagePreventedKg": 3800.0,
  "reallocations": [
    {
      "batchCode": "BATCH-TOM-04",
      "previousDestination": "Delhi Azadpur Mandi",
      "newDestination": "Ahmedabad Quick-Processing Cluster",
      "actionTaken": "DIVERT_AND_DISCOUNT",
      "discountApplied": 25.0,
      "explanation": "6h delay would cause arrival with only 4h RSL, leading to 100% rejection. Diverted to Ahmedabad (8h transit) with 25% discount, saving ₹72,000 in salvage revenue."
    }
  ]
}
```

---

## 4. Concurrency & Transaction Control (Resume Highlighting)

As emphasized in your background, FreshRoute employs **Pessimistic Row-Level Locking** during multi-hub allocation checkouts:

```java
@Repository
public interface ProduceBatchRepository extends JpaRepository<ProduceBatch, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM ProduceBatch b WHERE b.id = :id")
    Optional<ProduceBatch> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT b FROM ProduceBatch b WHERE b.remainingQuantityKg > 0 ORDER BY b.remainingShelfLifeHours ASC")
    List<ProduceBatch> findAllActiveFefoOrder();
}
```
This guarantees zero race conditions or phantom inventory reservations during high-frequency dispatch execution.
