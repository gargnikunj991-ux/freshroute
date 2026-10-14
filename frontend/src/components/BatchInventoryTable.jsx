import React, { useState } from 'react';
import { Clock, Thermometer, AlertTriangle, ArrowUpDown, Filter } from 'lucide-react';

export default function BatchInventoryTable({ batches }) {
  const [filterCommodity, setFilterCommodity] = useState('ALL');
  const [sortBy, setSortBy] = useState('FEFO'); // 'FEFO', 'QTY_DESC', 'COST'

  const filtered = batches.filter(b => {
    if (filterCommodity === 'ALL') return true;
    return b.commodity === filterCommodity;
  });

  const sorted = [...filtered].sort((a, b) => {
    if (sortBy === 'FEFO') return a.remainingShelfLifeHours - b.remainingShelfLifeHours;
    if (sortBy === 'QTY_DESC') return b.remainingQuantityKg - a.remainingQuantityKg;
    if (sortBy === 'COST') return b.procurementCostPerKg - a.procurementCostPerKg;
    return 0;
  });

  const getRslBar = (rslHours, initialHours) => {
    const pct = Math.min(100, Math.max(5, (rslHours / (initialHours || 72)) * 100));
    let color = 'bg-emerald-500';
    let textColor = 'text-emerald-400';

    if (rslHours <= 16.0) {
      color = 'bg-rose-500';
      textColor = 'text-rose-400 font-bold';
    } else if (rslHours <= 30.0) {
      color = 'bg-amber-400';
      textColor = 'text-amber-400 font-medium';
    }

    return (
      <div className="w-full">
        <div className="flex items-center justify-between text-xs mb-1">
          <span className={`font-mono ${textColor}`}>{rslHours.toFixed(1)} hrs left</span>
          <span className="text-[10px] text-slate-500">Base: {initialHours || 72}h</span>
        </div>
        <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden">
          <div className={`${color} h-full rounded-full transition-all duration-500`} style={{ width: `${pct}%` }}></div>
        </div>
      </div>
    );
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'FULLY_ALLOCATED':
        return <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">DISPATCHED</span>;
      case 'SPOILED':
        return <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20">SPOILED</span>;
      default:
        return <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">AVAILABLE</span>;
    }
  };

  return (
    <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6 shadow-lg">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-slate-800 gap-3">
        <div>
          <div className="flex items-center space-x-2">
            <Clock className="h-5 w-5 text-amber-400" />
            <h3 className="text-base font-bold text-white tracking-tight">
              Live Perishable Inventory Queue (FEFO Priority Ranking)
            </h3>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Produce ordered by shelf-life decay rate. High urgency batches (<span className="text-rose-400 font-mono">&lt;16h</span>) are prioritized for immediate dispatch.
          </p>
        </div>

        {/* Filter Controls */}
        <div className="flex items-center space-x-2">
          <select
            value={filterCommodity}
            onChange={(e) => setFilterCommodity(e.target.value)}
            className="bg-slate-800 border border-slate-700 text-slate-200 text-xs rounded-lg px-2.5 py-1.5 focus:outline-none focus:border-emerald-500"
          >
            <option value="ALL">All Commodities</option>
            <option value="TOMATO">Tomatoes</option>
            <option value="MANGO">Mangoes</option>
            <option value="BANANA">Bananas</option>
            <option value="SPINACH">Spinach</option>
            <option value="POTATO">Potatoes</option>
          </select>

          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="bg-slate-800 border border-slate-700 text-slate-200 text-xs rounded-lg px-2.5 py-1.5 focus:outline-none focus:border-emerald-500"
          >
            <option value="FEFO">Sort: Lowest RSL (FEFO)</option>
            <option value="QTY_DESC">Sort: Highest Qty</option>
            <option value="COST">Sort: Highest Cost</option>
          </select>
        </div>
      </div>

      {/* Table */}
      <div className="overflow-x-auto mt-4">
        <table className="w-full text-left text-xs">
          <thead>
            <tr className="border-b border-slate-800 text-slate-400 uppercase tracking-wider text-[11px]">
              <th className="pb-3 font-semibold">Batch Code</th>
              <th className="pb-3 font-semibold">Commodity & Variety</th>
              <th className="pb-3 font-semibold">Quantity</th>
              <th className="pb-3 font-semibold w-52">Remaining Shelf Life (RSL)</th>
              <th className="pb-3 font-semibold">Urgency Index (Ui)</th>
              <th className="pb-3 font-semibold">Cost Basis</th>
              <th className="pb-3 font-semibold">Temp (°C)</th>
              <th className="pb-3 font-semibold text-right">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 font-sans">
            {sorted.map((b) => (
              <tr key={b.id || b.batchCode} className="hover:bg-slate-800/40 transition">
                <td className="py-3 font-mono font-medium text-slate-200">
                  {b.batchCode}
                </td>
                <td className="py-3">
                  <div className="font-semibold text-white">{b.variety}</div>
                  <div className="text-[11px] text-slate-400 font-mono">{b.commodity} • {b.category}</div>
                </td>
                <td className="py-3 font-mono text-slate-200 font-medium">
                  {b.remainingQuantityKg?.toLocaleString()} kg
                  {b.remainingQuantityKg < b.initialQuantityKg && (
                    <span className="text-[10px] text-slate-500 block">Initial: {b.initialQuantityKg} kg</span>
                  )}
                </td>
                <td className="py-3">
                  {getRslBar(b.remainingShelfLifeHours, b.initialShelfLifeHours)}
                </td>
                <td className="py-3 font-mono">
                  <span className={`px-2 py-0.5 rounded text-[11px] ${
                    b.remainingShelfLifeHours <= 16
                      ? 'bg-rose-500/10 text-rose-400 font-bold border border-rose-500/20'
                      : 'text-slate-300'
                  }`}>
                    {b.urgencyIndex?.toFixed(3) || (1 / Math.max(1, b.remainingShelfLifeHours)).toFixed(3)}
                  </span>
                </td>
                <td className="py-3 font-mono text-slate-300">
                  ₹{b.procurementCostPerKg?.toFixed(2)}/kg
                </td>
                <td className="py-3 font-mono text-cyan-400">
                  <span className="inline-flex items-center">
                    <Thermometer className="h-3 w-3 mr-0.5 text-cyan-400" />
                    {b.currentTemperatureCelsius?.toFixed(1)}°C
                  </span>
                </td>
                <td className="py-3 text-right">
                  {getStatusBadge(b.status)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
