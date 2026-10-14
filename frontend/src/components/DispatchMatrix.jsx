import React, { useState } from 'react';
import { Truck, Play, CheckCircle2, AlertCircle, Info, Sparkles, Tag as TagIcon } from 'lucide-react';

export default function DispatchMatrix({ allocations, onRunOptimization, isOptimizing }) {
  const [dynamicDiscounting, setDynamicDiscounting] = useState(true);
  const [selectedRationale, setSelectedRationale] = useState(null);

  return (
    <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6 shadow-lg">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-slate-800 gap-3">
        <div>
          <div className="flex items-center space-x-2">
            <Truck className="h-5 w-5 text-emerald-400" />
            <h3 className="text-base font-bold text-white tracking-tight">
              Smart Dispatch Allocation Matrix (FEFO + Margin Optimization)
            </h3>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Constrained multi-factor routing respecting vehicle payload, demand caps, transit time, and retail shelf-life safety buffers.
          </p>
        </div>

        {/* Action Controls */}
        <div className="flex items-center space-x-3">
          <label className="flex items-center space-x-2 text-xs text-slate-300 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={dynamicDiscounting}
              onChange={(e) => setDynamicDiscounting(e.target.checked)}
              className="rounded bg-slate-800 border-slate-700 text-emerald-500 focus:ring-emerald-500"
            />
            <span>Dynamic Discounting</span>
          </label>

          <button
            onClick={() => onRunOptimization({ enableDynamicDiscounting: dynamicDiscounting })}
            disabled={isOptimizing}
            className="flex items-center space-x-2 px-4 py-2 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-500 hover:from-emerald-500 hover:to-teal-400 text-white text-xs font-bold shadow-lg shadow-emerald-500/20 transition active:scale-95 disabled:opacity-50"
          >
            <Play className={`h-3.5 w-3.5 fill-current ${isOptimizing ? 'animate-spin' : ''}`} />
            <span>{isOptimizing ? 'Optimizing...' : 'Execute FreshRoute Optimization'}</span>
          </button>
        </div>
      </div>

      {/* Rationale Modal / Highlight */}
      {selectedRationale && (
        <div className="mt-4 p-4 rounded-xl bg-slate-800/90 border border-emerald-500/30 flex items-start justify-between text-xs animate-fadeIn">
          <div className="flex items-start space-x-2.5">
            <Sparkles className="h-4 w-4 text-emerald-400 mt-0.5 shrink-0" />
            <div>
              <span className="font-bold text-emerald-400">Decision Rationale for {selectedRationale.batchCode} → {selectedRationale.destinationName}:</span>
              <p className="text-slate-200 mt-1 leading-relaxed">{selectedRationale.decisionRationale}</p>
            </div>
          </div>
          <button
            onClick={() => setSelectedRationale(null)}
            className="text-slate-400 hover:text-white text-xs font-bold ml-3"
          >
            ✕
          </button>
        </div>
      )}

      {/* Matrix Table */}
      <div className="overflow-x-auto mt-4">
        {(!allocations || allocations.length === 0) ? (
          <div className="py-12 text-center text-slate-500">
            <Truck className="h-10 w-10 mx-auto text-slate-600 mb-2 opacity-50" />
            <p className="text-sm font-medium">No active dispatches. Click "Execute FreshRoute Optimization" above.</p>
          </div>
        ) : (
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-slate-800 text-slate-400 uppercase tracking-wider text-[11px]">
                <th className="pb-3 font-semibold">Consignment</th>
                <th className="pb-3 font-semibold">Destination Market</th>
                <th className="pb-3 font-semibold">Tonnage</th>
                <th className="pb-3 font-semibold">Transit & Buffer</th>
                <th className="pb-3 font-semibold">Dynamic Discount</th>
                <th className="pb-3 font-semibold">Net Margin</th>
                <th className="pb-3 font-semibold">Net Profit</th>
                <th className="pb-3 font-semibold text-right">Rationale</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-sans">
              {allocations.map((a, idx) => {
                const isSalvage = a.destinationTier === 'FOOD_PROCESSOR';
                const hasDiscount = a.discountPercentage > 0;

                return (
                  <tr key={idx} className="hover:bg-slate-800/40 transition group">
                    <td className="py-3">
                      <div className="font-mono font-bold text-white">{a.batchCode}</div>
                      <div className="text-[10px] text-slate-400 font-mono">{a.commodity}</div>
                    </td>
                    <td className="py-3">
                      <div className="font-semibold text-slate-200">{a.destinationName}</div>
                      <div className="text-[10px] text-slate-400 font-mono">
                        {a.destinationCity} • <span className={isSalvage ? 'text-amber-400 font-bold' : 'text-slate-400'}>{a.destinationTier}</span>
                      </div>
                    </td>
                    <td className="py-3 font-mono font-semibold text-slate-100">
                      {a.allocatedQuantityKg?.toLocaleString()} kg
                    </td>
                    <td className="py-3 font-mono">
                      <div className="text-slate-300">{a.transitTimeHours}h transit</div>
                      <div className="text-[10px] text-emerald-400 font-medium">
                        {a.remainingShelfLifeOnArrivalHours?.toFixed(1)}h on arrival (Safe)
                      </div>
                    </td>
                    <td className="py-3 font-mono">
                      {hasDiscount ? (
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-bold bg-amber-500/10 text-amber-400 border border-amber-500/20">
                          <TagIcon className="h-3 w-3 mr-1" />
                          -{a.discountPercentage?.toFixed(0)}% Markdown
                        </span>
                      ) : (
                        <span className="text-slate-500 text-[11px]">0% (Prime Price)</span>
                      )}
                    </td>
                    <td className="py-3 font-mono text-emerald-400 font-semibold">
                      ₹{a.netMarginPerKg?.toFixed(2)}/kg
                    </td>
                    <td className="py-3 font-mono text-white font-bold">
                      ₹{Math.round(a.totalNetProfit)?.toLocaleString()}
                    </td>
                    <td className="py-3 text-right">
                      <button
                        onClick={() => setSelectedRationale(a)}
                        className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white border border-slate-700 text-[11px] font-medium transition"
                        title="View explainable reason"
                      >
                        Audit Why
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
