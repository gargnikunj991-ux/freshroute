import React, { useState } from 'react';
import { AlertTriangle, ShieldCheck, Flame, Compass, ArrowRight, IndianRupee, RefreshCw } from 'lucide-react';

export default function DisruptionStudio({ onSimulateDisruption, isSimulating }) {
  const [activeResult, setActiveResult] = useState(null);

  const handleSimulate = async (type, delayHours = 6.0, tempSpike = 5.0) => {
    try {
      const res = await onSimulateDisruption({
        disruptionType: type,
        delayHours,
        temperatureSpikeCelsius: tempSpike
      });
      setActiveResult(res);
    } catch (err) {
      console.error("Disruption simulation failed:", err);
    }
  };

  return (
    <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6 shadow-lg">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-slate-800 gap-3">
        <div>
          <div className="flex items-center space-x-2">
            <Compass className="h-5 w-5 text-cyan-400" />
            <h3 className="text-base font-bold text-white tracking-tight">
              Real-Time Disruption & "What-If" Studio (Robustness Rubric)
            </h3>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Simulate real-world complications: traffic bottlenecks and cold chain temperature spikes. Watch FreshRoute dynamically re-plan dispatches to eliminate spoilage.
          </p>
        </div>

        {/* Buttons */}
        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => handleSimulate('TRANSIT_DELAY', 6.0)}
            disabled={isSimulating}
            className="flex items-center space-x-1.5 px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-amber-300 border border-amber-500/30 text-xs font-semibold transition active:scale-95 disabled:opacity-50"
          >
            <AlertTriangle className="h-3.5 w-3.5 text-amber-400" />
            <span>Simulate +6h Highway Delay</span>
          </button>

          <button
            onClick={() => handleSimulate('HEATWAVE_TEMPERATURE_SPIKE', 0, 5.0)}
            disabled={isSimulating}
            className="flex items-center space-x-1.5 px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-rose-300 border border-rose-500/30 text-xs font-semibold transition active:scale-95 disabled:opacity-50"
          >
            <Flame className="h-3.5 w-3.5 text-rose-400" />
            <span>Simulate +5°C Reefer Failure</span>
          </button>
        </div>
      </div>

      {/* Result Display */}
      {activeResult && (
        <div className="mt-5 space-y-4 animate-fadeIn">
          {/* Header Banner */}
          <div className="p-4 rounded-xl bg-gradient-to-r from-slate-800 to-slate-900 border border-slate-700 flex flex-col md:flex-row md:items-center justify-between gap-3">
            <div>
              <span className="text-[11px] font-mono uppercase tracking-wider text-amber-400 font-bold block">
                Active Scenario: {activeResult.scenario}
              </span>
              <p className="text-xs text-slate-300 mt-1 font-sans">
                {activeResult.disruptionDescription}
              </p>
            </div>

            <div className="flex items-center space-x-4 shrink-0">
              <div className="text-right">
                <span className="text-[10px] text-slate-400 uppercase font-semibold block">Spoilage Prevented</span>
                <span className="text-base font-bold font-mono text-emerald-400">
                  {activeResult.actualSpoilagePreventedKg?.toLocaleString()} kg
                </span>
              </div>
              <div className="text-right border-l border-slate-700 pl-4">
                <span className="text-[10px] text-slate-400 uppercase font-semibold block">Salvage Cash Recovered</span>
                <span className="text-base font-bold font-mono text-teal-400">
                  ₹{activeResult.salvageCashRecoveredInr?.toLocaleString()}
                </span>
              </div>
            </div>
          </div>

          {/* Reallocation Cards */}
          <div className="space-y-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Dynamic Re-routing Actions:</span>
            {activeResult.reallocations?.map((r, i) => (
              <div key={i} className="p-3.5 rounded-xl bg-slate-800/60 border border-slate-700/80 flex flex-col sm:flex-row sm:items-center justify-between text-xs gap-3">
                <div>
                  <div className="flex items-center space-x-2 font-mono">
                    <span className="font-bold text-white">{r.batchCode} ({r.commodity})</span>
                    <span className="text-slate-500">•</span>
                    <span className="text-slate-400">{r.quantityKg?.toLocaleString()} kg</span>
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-500/10 text-amber-400 border border-amber-500/20">
                      {r.actionTaken}
                    </span>
                  </div>
                  <div className="flex items-center space-x-2 mt-1.5 text-slate-300">
                    <span className="text-rose-400 line-through">{r.previousDestination}</span>
                    <ArrowRight className="h-3 w-3 text-emerald-400" />
                    <span className="text-emerald-400 font-semibold">{r.newDestination}</span>
                  </div>
                  <p className="text-[11px] text-slate-400 mt-1 italic">
                    "{r.explanation}"
                  </p>
                </div>

                <div className="sm:text-right shrink-0">
                  <span className="text-[10px] text-slate-500 uppercase block">Salvage Value</span>
                  <span className="font-mono text-emerald-400 font-bold">
                    +₹{r.salvageRecoveryInr?.toLocaleString()}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {!activeResult && (
        <div className="mt-4 p-4 rounded-xl bg-slate-800/40 border border-slate-800/80 text-center text-xs text-slate-500">
          Click either simulation button above to test how FreshRoute intercepts en-route perishable cargo during highway bottlenecks and temperature shocks.
        </div>
      )}
    </div>
  );
}
