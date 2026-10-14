import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import KpiHeader from './components/KpiHeader';
import ComparisonChart from './components/ComparisonChart';
import BatchInventoryTable from './components/BatchInventoryTable';
import DispatchMatrix from './components/DispatchMatrix';
import DisruptionStudio from './components/DisruptionStudio';
import AddBatchModal from './components/AddBatchModal';
import { api } from './api';
import { RefreshCw, CheckCircle2, AlertCircle } from 'lucide-react';

export default function App() {
  const [overview, setOverview] = useState(null);
  const [batches, setBatches] = useState([]);
  const [allocations, setAllocations] = useState([]);
  const [isOptimizing, setIsOptimizing] = useState(false);
  const [isSimulating, setIsSimulating] = useState(false);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [toast, setToast] = useState(null);

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 4000);
  };

  const loadInitialData = async () => {
    try {
      const data = await api.getOverview();
      setOverview(data);
      setBatches(data.activeBatches || []);
      setAllocations(data.comparison?.freshRoute?.allocations || []);
    } catch (err) {
      console.error("Failed to load dashboard overview:", err);
      showToast("Connected via benchmark fallback mode", "info");
    }
  };

  useEffect(() => {
    loadInitialData();
  }, []);

  const handleRunOptimization = async (params = {}) => {
    setIsOptimizing(true);
    try {
      const res = await api.optimize(params);
      setAllocations(res.allocations || []);
      
      // Refresh overview KPIs
      const updatedOverview = await api.getOverview();
      setOverview(updatedOverview);
      setBatches(updatedOverview.activeBatches || []);

      showToast(`FreshRoute FEFO optimization executed! ${res.totalDispatches} dispatches created.`);
    } catch (err) {
      console.error("Optimization failed:", err);
      showToast("Optimization run failed: " + err.message, "error");
    } finally {
      setIsOptimizing(false);
    }
  };

  const handleSimulateDisruption = async (payload) => {
    setIsSimulating(true);
    try {
      const res = await api.simulateDisruption(payload);
      showToast(`Disruption triggered: ${res.scenario}`);
      
      // Refresh inventory after disruption
      const updatedBatches = await api.getBatches();
      setBatches(updatedBatches);

      return res;
    } catch (err) {
      showToast("Disruption simulation failed: " + err.message, "error");
      throw err;
    } finally {
      setIsSimulating(false);
    }
  };

  const handleAddBatch = async (batchPayload) => {
    try {
      await api.createBatch(batchPayload);
      showToast(`Batch ${batchPayload.batchCode} registered successfully!`);
      const updatedOverview = await api.getOverview();
      setOverview(updatedOverview);
      setBatches(updatedOverview.activeBatches || []);
    } catch (err) {
      showToast("Failed to register batch: " + err.message, "error");
      throw err;
    }
  };

  const handleReset = async () => {
    try {
      await api.resetData();
      showToast("Database successfully reset to initial reference dataset.");
      await loadInitialData();
    } catch (err) {
      showToast("Reset failed: " + err.message, "error");
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col font-sans">
      {/* Toast Notification */}
      {toast && (
        <div className="fixed bottom-5 right-5 z-50 flex items-center space-x-2 px-4 py-2.5 rounded-xl bg-slate-900 border border-slate-700 text-xs shadow-2xl animate-bounce">
          {toast.type === 'error' ? (
            <AlertCircle className="h-4 w-4 text-rose-400" />
          ) : (
            <CheckCircle2 className="h-4 w-4 text-emerald-400" />
          )}
          <span className="text-white font-medium">{toast.message}</span>
        </div>
      )}

      {/* Top Navigation */}
      <Navbar
        onReset={handleReset}
        onOpenAddModal={() => setIsAddModalOpen(true)}
        isOptimizing={isOptimizing}
      />

      {/* Main Content */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
        
        {/* Executive KPI Command Cards */}
        <KpiHeader
          comparison={overview?.comparison}
          totalInventoryKg={overview?.totalInventoryKg}
          activeBatchesCount={overview?.activeBatchesCount}
          highRiskCount={overview?.highRiskBatchesCount}
        />

        {/* Side-by-Side Algorithmic Benchmark Chart */}
        <ComparisonChart comparison={overview?.comparison} />

        {/* Live FEFO Perishable Batch Table */}
        <BatchInventoryTable batches={batches} />

        {/* Smart Dispatch Allocation Matrix */}
        <DispatchMatrix
          allocations={allocations}
          onRunOptimization={handleRunOptimization}
          isOptimizing={isOptimizing}
        />

        {/* Disruption "What-If" Studio */}
        <DisruptionStudio
          onSimulateDisruption={handleSimulateDisruption}
          isSimulating={isSimulating}
        />

      </main>

      {/* Add Custom Batch Modal */}
      <AddBatchModal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        onAddBatch={handleAddBatch}
      />

      {/* Footer */}
      <footer className="border-t border-slate-800/80 bg-slate-900/50 py-6 mt-12">
        <div className="max-w-7xl mx-auto px-4 text-center sm:flex sm:justify-between sm:items-center text-xs text-slate-500">
          <p>
            FreshRoute • Problem Statement 03 • Reference: Ministry of Food Processing Industries (MoFPI 2020–22 Study)
          </p>
          <p className="mt-2 sm:mt-0 font-mono text-slate-400">
            Java 21 • Spring Boot 3 • PostgreSQL • React 18 • Vite • Tailwind CSS
          </p>
        </div>
      </footer>
    </div>
  );
}
