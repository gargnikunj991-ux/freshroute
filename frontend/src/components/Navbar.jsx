import React from 'react';
import { Leaf, Database, ExternalLink, RotateCcw, Plus, Activity } from 'lucide-react';
import { SWAGGER_URL } from '../api';

export default function Navbar({ onReset, onOpenAddModal, isOptimizing }) {
  return (
    <header className="border-b border-slate-800 bg-slate-900/90 backdrop-blur sticky top-0 z-40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        
        {/* Brand */}
        <div className="flex items-center space-x-3">
          <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-emerald-600 to-teal-400 flex items-center justify-center shadow-lg shadow-emerald-500/20">
            <Leaf className="h-6 w-6 text-slate-950" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="text-xl font-bold tracking-tight text-white">FreshRoute</span>
              <span className="px-2 py-0.5 text-xs font-semibold uppercase tracking-wider bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 rounded-md">
                PS 03 • MoFPI
              </span>
            </div>
            <p className="text-xs text-slate-400 hidden sm:block">
              Decision Support for Perishable Produce Distribution
            </p>
          </div>
        </div>

        {/* Status & Actions */}
        <div className="flex items-center space-x-3">
          {/* PostgreSQL Status Indicator */}
          <div className="hidden md:flex items-center space-x-2 px-3 py-1.5 rounded-lg bg-slate-800/80 border border-slate-700/60 text-xs">
            <Database className="h-3.5 w-3.5 text-cyan-400" />
            <span className="text-slate-300 font-mono">PostgreSQL</span>
            <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse"></span>
          </div>

          {/* Swagger Link */}
          <a
            href={SWAGGER_URL}
            target="_blank"
            rel="noreferrer"
            className="hidden sm:flex items-center space-x-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium border border-slate-700 transition"
            title="Open Spring Boot Swagger OpenAPI Docs"
          >
            <Activity className="h-3.5 w-3.5 text-emerald-400" />
            <span>OpenAPI / Swagger</span>
            <ExternalLink className="h-3 w-3 text-slate-400" />
          </a>

          {/* Reset Database */}
          <button
            onClick={onReset}
            disabled={isOptimizing}
            className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-rose-950/40 hover:text-rose-300 text-slate-300 text-xs font-medium border border-slate-700 hover:border-rose-800/60 transition disabled:opacity-50"
            title="Reset Benchmark Corridors & Batches"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            <span className="hidden sm:inline">Reset Seed</span>
          </button>

          {/* Add Batch Button */}
          <button
            onClick={onOpenAddModal}
            className="flex items-center space-x-1.5 px-3.5 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold shadow-md shadow-emerald-600/20 transition active:scale-95"
          >
            <Plus className="h-3.5 w-3.5 stroke-[3]" />
            <span>+ Add Batch</span>
          </button>
        </div>

      </div>
    </header>
  );
}
