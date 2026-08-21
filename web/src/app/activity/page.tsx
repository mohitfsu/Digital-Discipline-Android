"use client";

import React, { useState } from "react";
import { useFamily } from "@/context/FamilyContext";
import ActivityBarChart from "@/components/ActivityBarChart";
import {
  BarChart2,
  Shield,
  Clock,
  ShieldCheck,
  ShieldAlert,
  Flame,
  Lock,
} from "lucide-react";

export default function ActivityPage() {
  const { activeChild, activeDevice, todaySummary } = useFamily();
  const [selectedRange, setSelectedRange] = useState<"today" | "yesterday" | "week">("today");

  const childName = activeChild?.name || "Child";
  const isDevicePaired = !!activeDevice;

  const chartData = [
    {
      label: "Today",
      minutes: isDevicePaired ? (todaySummary?.totalScreenTimeMinutes || 0) : 0,
      blocks: isDevicePaired ? (todaySummary?.totalBlocks || 0) : 0,
      interventions: isDevicePaired ? (todaySummary?.totalInterventions || 0) : 0,
    },
    {
      label: "Yesterday",
      minutes: isDevicePaired ? 0 : 0,
      blocks: isDevicePaired ? 0 : 0,
      interventions: isDevicePaired ? 0 : 0,
    },
    {
      label: "Weekly Avg / Day",
      minutes: isDevicePaired ? 0 : 0,
      blocks: isDevicePaired ? 0 : 0,
      interventions: isDevicePaired ? 0 : 0,
    },
  ];

  const appBreakdown = [
    {
      name: "Instagram",
      pkg: "com.instagram.android",
      minutes: isDevicePaired ? (todaySummary?.appBreakdown?.["com.instagram.android"]?.minutes || 0) : 0,
      blocks: isDevicePaired ? (todaySummary?.appBreakdown?.["com.instagram.android"]?.blocks || 0) : 0,
      interventions: isDevicePaired ? (todaySummary?.appBreakdown?.["com.instagram.android"]?.interventions || 0) : 0,
      limit: "30m limit",
    },
    {
      name: "YouTube",
      pkg: "com.google.android.youtube",
      minutes: isDevicePaired ? (todaySummary?.appBreakdown?.["com.google.android.youtube"]?.minutes || 0) : 0,
      blocks: isDevicePaired ? (todaySummary?.appBreakdown?.["com.google.android.youtube"]?.blocks || 0) : 0,
      interventions: isDevicePaired ? (todaySummary?.appBreakdown?.["com.google.android.youtube"]?.interventions || 0) : 0,
      limit: "45m limit",
    },
    {
      name: "Gaming (Free Fire)",
      pkg: "com.dts.freefireth",
      minutes: 0,
      blocks: 0,
      interventions: 0,
      limit: "Strict Block",
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800/80">
        <div>
          <h1 className="text-2xl font-black text-white tracking-tight">
            Discipline Activity & Trends
          </h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Aggregated digital wellbeing trends for 👶 <strong>{childName}</strong>
          </p>
        </div>

        <div className="flex items-center gap-1.5 p-1 rounded-xl bg-slate-900 border border-slate-800 text-xs">
          <button
            onClick={() => setSelectedRange("today")}
            className={`px-3 py-1.5 rounded-lg font-bold transition ${
              selectedRange === "today" ? "bg-sky-600 text-white" : "text-slate-400 hover:text-white"
            }`}
          >
            Today
          </button>
          <button
            onClick={() => setSelectedRange("yesterday")}
            className={`px-3 py-1.5 rounded-lg font-bold transition ${
              selectedRange === "yesterday" ? "bg-sky-600 text-white" : "text-slate-400 hover:text-white"
            }`}
          >
            Yesterday
          </button>
          <button
            onClick={() => setSelectedRange("week")}
            className={`px-3 py-1.5 rounded-lg font-bold transition ${
              selectedRange === "week" ? "bg-sky-600 text-white" : "text-slate-400 hover:text-white"
            }`}
          >
            This Week
          </button>
        </div>
      </div>

      {/* Privacy Notice Banner */}
      <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center gap-3 text-xs text-slate-300">
        <Lock className="w-4 h-4 text-emerald-400 shrink-0" />
        <span>
          <strong className="text-white">Privacy-First Architecture:</strong> Only aggregated screen duration and block counts are recorded. Digital Discipline never reads private messages, chats, recordings, or screenshots.
        </span>
      </div>

      {/* Weekly Visual Comparison Bar Chart */}
      <div className="p-5 rounded-2xl bg-[#0F172A] border border-slate-800 space-y-4">
        <h2 className="text-sm font-bold text-white uppercase tracking-wider text-xs">
          Screen Time & Habit Reset Trends
        </h2>
        <ActivityBarChart data={chartData} />
      </div>

      {/* App Breakdown Table */}
      <div className="p-5 rounded-2xl bg-[#0F172A] border border-slate-800 space-y-4">
        <h2 className="text-sm font-bold text-white uppercase tracking-wider text-xs">
          Target Applications Breakdown
        </h2>

        <div className="space-y-2">
          {appBreakdown.map((app) => (
            <div
              key={app.pkg}
              className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-slate-800 flex items-center justify-center text-lg">
                  📱
                </div>
                <div>
                  <h3 className="text-sm font-bold text-white">{app.name}</h3>
                  <p className="text-[11px] text-slate-400">{app.limit}</p>
                </div>
              </div>

              <div className="flex items-center gap-6 text-xs">
                <div>
                  <span className="text-slate-400 block text-[10px]">Time Used</span>
                  <span className="font-black text-white">{app.minutes} min</span>
                </div>
                <div>
                  <span className="text-slate-400 block text-[10px]">Blocks</span>
                  <span className="font-black text-rose-400">{app.blocks}</span>
                </div>
                <div>
                  <span className="text-slate-400 block text-[10px]">Interventions</span>
                  <span className="font-black text-emerald-400">{app.interventions}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
