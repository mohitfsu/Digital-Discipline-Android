"use client";

import React from "react";

interface ActivityDataPoint {
  label: string;
  minutes: number;
  blocks: number;
  interventions: number;
}

interface ActivityBarChartProps {
  data: ActivityDataPoint[];
}

export default function ActivityBarChart({ data }: ActivityBarChartProps) {
  const maxMinutes = Math.max(...data.map((d) => d.minutes), 60);

  return (
    <div className="space-y-4">
      {/* Visual Bars */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
        {data.map((item) => {
          const heightPercent = Math.min(Math.round((item.minutes / maxMinutes) * 100), 100);
          return (
            <div
              key={item.label}
              className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-300">{item.label}</span>
                  <span className="text-sm font-black text-sky-400">{item.minutes} min</span>
                </div>
                <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden mt-2">
                  <div
                    className="bg-gradient-to-r from-sky-500 to-indigo-500 h-full rounded-full transition-all duration-500"
                    style={{ width: `${heightPercent}%` }}
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2 mt-4 pt-3 border-t border-slate-800/80 text-center">
                <div className="p-1.5 rounded-lg bg-rose-950/30 border border-rose-900/40">
                  <p className="text-[10px] text-rose-300">Blocked</p>
                  <p className="text-xs font-bold text-rose-400">{item.blocks}</p>
                </div>
                <div className="p-1.5 rounded-lg bg-emerald-950/30 border border-emerald-900/40">
                  <p className="text-[10px] text-emerald-300">Interventions</p>
                  <p className="text-xs font-bold text-emerald-400">{item.interventions}</p>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
