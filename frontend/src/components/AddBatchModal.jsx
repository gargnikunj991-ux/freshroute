import React, { useState } from 'react';
import { X, Plus, AlertCircle } from 'lucide-react';

export default function AddBatchModal({ isOpen, onClose, onAddBatch }) {
  const [formData, setFormData] = useState({
    batchCode: `BATCH-CUSTOM-${Math.floor(100 + Math.random() * 900)}`,
    variety: 'Nashik Hybrid Selection',
    commodity: 'TOMATO',
    category: 'VEGETABLE',
    quantityKg: 3000,
    remainingShelfLifeHours: 28,
    procurementCostPerKg: 22,
    currentTemperatureCelsius: 12
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      await onAddBatch({
        ...formData,
        quantityKg: Number(formData.quantityKg),
        remainingShelfLifeHours: Number(formData.remainingShelfLifeHours),
        procurementCostPerKg: Number(formData.procurementCostPerKg),
        currentTemperatureCelsius: Number(formData.currentTemperatureCelsius)
      });
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to register batch');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fadeIn">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-md p-6 shadow-2xl relative">
        
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div>
            <h3 className="text-base font-bold text-white tracking-tight">Register Custom Perishable Batch</h3>
            <p className="text-xs text-slate-400 mt-0.5">Live demonstration inventory injection</p>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800 transition"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {error && (
          <div className="mt-4 p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-xs text-rose-400 flex items-center space-x-2">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="mt-4 space-y-3.5 text-xs">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-400 mb-1 font-medium">Batch Code</label>
              <input
                type="text"
                required
                value={formData.batchCode}
                onChange={(e) => setFormData({ ...formData, batchCode: e.target.value })}
                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-white font-mono focus:outline-none focus:border-emerald-500"
              />
            </div>
            <div>
              <label className="block text-slate-400 mb-1 font-medium">Commodity</label>
              <select
                value={formData.commodity}
                onChange={(e) => {
                  const comm = e.target.value;
                  let cat = 'VEGETABLE';
                  if (comm === 'MANGO' || comm === 'BANANA') cat = 'FRUIT';
                  if (comm === 'SPINACH') cat = 'LEAFY_GREEN';
                  if (comm === 'POTATO') cat = 'TUBER';
                  setFormData({ ...formData, commodity: comm, category: cat });
                }}
                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-emerald-500"
              >
                <option value="TOMATO">Tomato</option>
                <option value="MANGO">Mango</option>
                <option value="BANANA">Banana</option>
                <option value="SPINACH">Spinach</option>
                <option value="POTATO">Potato</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-slate-400 mb-1 font-medium">Produce Variety Name</label>
            <input
              type="text"
              required
              value={formData.variety}
              onChange={(e) => setFormData({ ...formData, variety: e.target.value })}
              className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-emerald-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-400 mb-1 font-medium">Quantity (kg)</label>
              <input
                type="number"
                min="100"
                step="100"
                required
                value={formData.quantityKg}
                onChange={(e) => setFormData({ ...formData, quantityKg: e.target.value })}
                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-white font-mono focus:outline-none focus:border-emerald-500"
              />
            </div>
            <div>
              <label className="block text-slate-400 mb-1 font-medium">Remaining Shelf Life (hrs)</label>
              <input
                type="number"
                min="2"
                max="360"
                required
                value={formData.remainingShelfLifeHours}
                onChange={(e) => setFormData({ ...formData, remainingShelfLifeHours: e.target.value })}
                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-white font-mono focus:outline-none focus:border-emerald-500"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-400 mb-1 font-medium">Procurement Cost (₹/kg)</label>
              <input
                type="number"
                min="5"
                step="0.5"
                required
                value={formData.procurementCostPerKg}
                onChange={(e) => setFormData({ ...formData, procurementCostPerKg: e.target.value })}
                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-white font-mono focus:outline-none focus:border-emerald-500"
              />
            </div>
            <div>
              <label className="block text-slate-400 mb-1 font-medium">Storage Temp (°C)</label>
              <input
                type="number"
                min="2"
                max="30"
                value={formData.currentTemperatureCelsius}
                onChange={(e) => setFormData({ ...formData, currentTemperatureCelsius: e.target.value })}
                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-white font-mono focus:outline-none focus:border-emerald-500"
              />
            </div>
          </div>

          <div className="pt-3 flex items-center justify-end space-x-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 font-medium transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold transition shadow-lg shadow-emerald-600/20 active:scale-95 disabled:opacity-50"
            >
              {isSubmitting ? 'Registering...' : 'Register Batch'}
            </button>
          </div>
        </form>

      </div>
    </div>
  );
}
