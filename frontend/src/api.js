let rawBase = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1').trim();
rawBase = rawBase.replace(/\/+$/, '');
if (!rawBase.endsWith('/api/v1')) {
  rawBase = `${rawBase}/api/v1`;
}
export const API_BASE = rawBase;
export const SWAGGER_URL = `${rawBase.replace(/\/api\/v1$/, '')}/swagger-ui/index.html`;

async function handleResponse(response) {
  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`API Error (${response.status}): ${errorText || response.statusText}`);
  }
  return response.json();
}

export const api = {
  async getOverview() {
    try {
      const res = await fetch(`${API_BASE}/dashboard/overview`);
      return await handleResponse(res);
    } catch (err) {
      console.warn("Backend not reachable, loading fallback benchmark overview...", err.message);
      return getFallbackOverview();
    }
  },

  async getBatches() {
    try {
      const res = await fetch(`${API_BASE}/dashboard/batches`);
      return await handleResponse(res);
    } catch (err) {
      console.warn("Backend not reachable, loading fallback batches...", err.message);
      return getFallbackBatches();
    }
  },

  async getMarkets() {
    try {
      const res = await fetch(`${API_BASE}/dashboard/markets`);
      return await handleResponse(res);
    } catch (err) {
      console.warn("Backend not reachable, loading fallback markets...", err.message);
      return [];
    }
  },

  async getRoutes() {
    try {
      const res = await fetch(`${API_BASE}/dashboard/routes`);
      return await handleResponse(res);
    } catch (err) {
      console.warn("Backend not reachable, loading fallback routes...", err.message);
      return [];
    }
  },

  async optimize(params = {}) {
    const res = await fetch(`${API_BASE}/allocation/optimize`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params)
    });
    return await handleResponse(res);
  },

  async compareBaseline() {
    const res = await fetch(`${API_BASE}/allocation/compare-baseline`);
    return await handleResponse(res);
  },

  async resetData() {
    const res = await fetch(`${API_BASE}/allocation/reset`, { method: 'POST' });
    return res.ok;
  },

  async simulateDisruption(payload) {
    const res = await fetch(`${API_BASE}/disruption/simulate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    return await handleResponse(res);
  },

  async createBatch(payload) {
    const res = await fetch(`${API_BASE}/dashboard/batches`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    return await handleResponse(res);
  }
};

// Fallback data matching the MoFPI 32,000 kg Benchmark Dataset
function getFallbackOverview() {
  return {
    totalInventoryKg: 32000.0,
    activeBatchesCount: 10,
    averageRslHours: 49.3,
    totalDestinationsCount: 6,
    highRiskBatchesCount: 3,
    comparison: {
      baseline: {
        status: "BASELINE_SIMULATION_COMPLETED",
        planType: "BASELINE_FIFO",
        totalDispatches: 14,
        totalAllocatedKg: 26200.0,
        totalSpoiledKg: 5800.0,
        spoilagePercentage: 18.1,
        grossSalesRevenue: 845000.0,
        totalFreightCost: 92000.0,
        totalDiscountsGranted: 0.0,
        totalSpoilageLoss: 148000.0,
        netEconomicProfit: 215000.0,
        overallDemandFulfillmentPercent: 74.2,
        zeroSpoilageCorridors: 0,
        allocations: []
      },
      freshRoute: {
        status: "OPTIMIZATION_SUCCESS",
        planType: "FRESHROUTE_FEFO",
        totalDispatches: 18,
        totalAllocatedKg: 30400.0,
        totalSpoiledKg: 1600.0,
        spoilagePercentage: 5.0,
        grossSalesRevenue: 1120000.0,
        totalFreightCost: 108000.0,
        totalDiscountsGranted: 42000.0,
        totalSpoilageLoss: 40000.0,
        netEconomicProfit: 465000.0,
        overallDemandFulfillmentPercent: 96.5,
        zeroSpoilageCorridors: 5,
        allocations: []
      },
      spoilageReductionKg: 4200.0,
      spoilageReductionPercent: 72.4,
      netProfitGainInr: 250000.0,
      netProfitGainPercent: 116.3,
      demandFulfillmentGainPercent: 22.3,
      feasibilityViolationsPrevented: 4,
      executiveSummary: "FreshRoute reduces perishable spoilage by 72.4% (-4,200 kg) and boosts net economic profit by +₹2,50,000 (+116.3%) while achieving 96.5% demand fulfillment across destination markets."
    },
    activeBatches: getFallbackBatches()
  };
}

function getFallbackBatches() {
  return [
    {
      id: 1,
      batchCode: "BATCH-SPN-01",
      variety: "Organic Green Palak",
      commodity: "SPINACH",
      category: "LEAFY_GREEN",
      initialQuantityKg: 1500.0,
      remainingQuantityKg: 1500.0,
      initialShelfLifeHours: 24.0,
      remainingShelfLifeHours: 14.0,
      procurementCostPerKg: 15.0,
      currentTemperatureCelsius: 8.0,
      status: "AVAILABLE",
      urgencyIndex: 0.071,
      riskLevel: "CRITICAL"
    },
    {
      id: 2,
      batchCode: "BATCH-TOM-01",
      variety: "Hybrid Red",
      commodity: "TOMATO",
      category: "VEGETABLE",
      initialQuantityKg: 4000.0,
      remainingQuantityKg: 4000.0,
      initialShelfLifeHours: 72.0,
      remainingShelfLifeHours: 22.0,
      procurementCostPerKg: 22.0,
      currentTemperatureCelsius: 12.0,
      status: "AVAILABLE",
      urgencyIndex: 0.045,
      riskLevel: "MEDIUM"
    },
    {
      id: 3,
      batchCode: "BATCH-BAN-01",
      variety: "Jalgaon Robusta",
      commodity: "BANANA",
      category: "FRUIT",
      initialQuantityKg: 3500.0,
      remainingQuantityKg: 3500.0,
      initialShelfLifeHours: 48.0,
      remainingShelfLifeHours: 26.0,
      procurementCostPerKg: 18.0,
      currentTemperatureCelsius: 14.0,
      status: "AVAILABLE",
      urgencyIndex: 0.038,
      riskLevel: "MEDIUM"
    },
    {
      id: 4,
      batchCode: "BATCH-SPN-02",
      variety: "Organic Green Palak",
      commodity: "SPINACH",
      category: "LEAFY_GREEN",
      initialQuantityKg: 1500.0,
      remainingQuantityKg: 1500.0,
      initialShelfLifeHours: 36.0,
      remainingShelfLifeHours: 28.0,
      procurementCostPerKg: 15.0,
      currentTemperatureCelsius: 8.0,
      status: "AVAILABLE",
      urgencyIndex: 0.036,
      riskLevel: "MEDIUM"
    },
    {
      id: 5,
      batchCode: "BATCH-MNG-01",
      variety: "Ratnagiri Alphonso",
      commodity: "MANGO",
      category: "FRUIT",
      initialQuantityKg: 2500.0,
      remainingQuantityKg: 2500.0,
      initialShelfLifeHours: 60.0,
      remainingShelfLifeHours: 36.0,
      procurementCostPerKg: 65.0,
      currentTemperatureCelsius: 13.0,
      status: "AVAILABLE",
      urgencyIndex: 0.028,
      riskLevel: "LOW"
    },
    {
      id: 6,
      batchCode: "BATCH-TOM-02",
      variety: "Desi Country",
      commodity: "TOMATO",
      category: "VEGETABLE",
      initialQuantityKg: 3500.0,
      remainingQuantityKg: 3500.0,
      initialShelfLifeHours: 72.0,
      remainingShelfLifeHours: 48.0,
      procurementCostPerKg: 20.0,
      currentTemperatureCelsius: 12.0,
      status: "AVAILABLE",
      urgencyIndex: 0.021,
      riskLevel: "LOW"
    },
    {
      id: 7,
      batchCode: "BATCH-BAN-02",
      variety: "Grand Naine G9",
      commodity: "BANANA",
      category: "FRUIT",
      initialQuantityKg: 4000.0,
      remainingQuantityKg: 4000.0,
      initialShelfLifeHours: 72.0,
      remainingShelfLifeHours: 55.0,
      procurementCostPerKg: 19.0,
      currentTemperatureCelsius: 14.0,
      status: "AVAILABLE",
      urgencyIndex: 0.018,
      riskLevel: "LOW"
    },
    {
      id: 8,
      batchCode: "BATCH-MNG-02",
      variety: "Gir Kesar",
      commodity: "MANGO",
      category: "FRUIT",
      initialQuantityKg: 3000.0,
      remainingQuantityKg: 3000.0,
      initialShelfLifeHours: 84.0,
      remainingShelfLifeHours: 60.0,
      procurementCostPerKg: 55.0,
      currentTemperatureCelsius: 13.0,
      status: "AVAILABLE",
      urgencyIndex: 0.017,
      riskLevel: "LOW"
    },
    {
      id: 9,
      batchCode: "BATCH-TOM-03",
      variety: "Roma Plum",
      commodity: "TOMATO",
      category: "VEGETABLE",
      initialQuantityKg: 4500.0,
      remainingQuantityKg: 4500.0,
      initialShelfLifeHours: 96.0,
      remainingShelfLifeHours: 72.0,
      procurementCostPerKg: 24.0,
      currentTemperatureCelsius: 11.0,
      status: "AVAILABLE",
      urgencyIndex: 0.014,
      riskLevel: "LOW"
    },
    {
      id: 10,
      batchCode: "BATCH-POT-01",
      variety: "Kufri Chipsona",
      commodity: "POTATO",
      category: "TUBER",
      initialQuantityKg: 4000.0,
      remainingQuantityKg: 4000.0,
      initialShelfLifeHours: 360.0,
      remainingShelfLifeHours: 180.0,
      procurementCostPerKg: 12.0,
      currentTemperatureCelsius: 15.0,
      status: "AVAILABLE",
      urgencyIndex: 0.006,
      riskLevel: "LOW"
    }
  ];
}
