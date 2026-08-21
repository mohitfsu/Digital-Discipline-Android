"use client";

import React, { useState, use } from "react";
import Link from "next/link";
import { useFamily } from "@/context/FamilyContext";
import DeviceStatusBadge from "@/components/DeviceStatusBadge";
import PairingModal from "@/components/PairingModal";
import {
  Smartphone,
  Shield,
  Clock,
  Zap,
  ArrowLeft,
  LayoutGrid,
  CheckCircle,
} from "lucide-react";

export default function ChildProfilePage({ params }: { params: Promise<{ childId: string }> }) {
  const resolvedParams = use(params);
  const { childId } = resolvedParams;
  const { childrenList, activeDevice, activePolicy, todaySummary } = useFamily();
  const [showPairingModal, setShowPairingModal] = useState(false);

  const child = childrenList.find((c) => c.childId === childId) || childrenList[0];

  if (!child) {
    return (
      <div className="p-8 text-center">
        <p className="text-sm font-bold text-slate-400">Child profile not found.</p>
        <Link href="/" className="inline-block mt-4 text-xs font-bold text-sky-400">
          ← Return to Dashboard
        </Link>
      </div>
    );
  }

  const rules = activePolicy?.rules || [];

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex items-center justify-between pb-4 border-b border-slate-800/80">
        <div className="flex items-center gap-3">
          <Link
            href="/"
            className="p-2 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 text-slate-400 hover:text-white transition"
          >
            <ArrowLeft className="w-4 h-4" />
          </Link>
          <div>
            <h1 className="text-xl font-black text-white">
              👶 {child.name}'s Profile ({child.age} yrs)
            </h1>
            <p className="text-xs text-slate-400">
              Child ID: <code className="text-slate-300 font-mono">{child.childId}</code>
            </p>
          </div>
        </div>

        <button
          onClick={() => setShowPairingModal(true)}
          className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-bold transition shadow-md"
        >
          <Smartphone className="w-3.5 h-3.5" />
          Pair Phone
        </button>
      </div>

      {/* Device Status */}
      <DeviceStatusBadge device={activeDevice} policyVersion={activePolicy?.version} />

      {/* Today's Usage Breakdown */}
      <div className="p-5 rounded-2xl bg-[#0F172A] border border-slate-800 space-y-4">
        <h2 className="text-sm font-bold text-white uppercase tracking-wider text-xs">
          Today's Usage & Boundaries
        </h2>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800">
            <p className="text-xs text-slate-400">Screen Time</p>
            <p className="text-xl font-black text-white mt-1">
              {todaySummary?.totalScreenTimeMinutes || 0} <span className="text-xs text-slate-400 font-normal">min</span>
            </p>
          </div>
          <div className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800">
            <p className="text-xs text-slate-400">Blocked Attempts</p>
            <p className="text-xl font-black text-rose-400 mt-1">
              {todaySummary?.totalBlocks || 0} <span className="text-xs text-slate-400 font-normal">times</span>
            </p>
          </div>
          <div className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800">
            <p className="text-xs text-slate-400">Earned Access</p>
            <p className="text-xl font-black text-amber-400 mt-1">
              {(todaySummary?.totalInterventions || 0) * 10} <span className="text-xs text-slate-400 font-normal">min</span>
            </p>
          </div>
        </div>
      </div>

      {/* Current Configured Rules */}
      <div className="p-5 rounded-2xl bg-[#0F172A] border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-bold text-white uppercase tracking-wider text-xs">
            Active App Rules ({rules.length})
          </h2>
          <Link
            href="/apps"
            className="text-xs font-bold text-sky-400 hover:text-sky-300 transition"
          >
            Edit All Rules →
          </Link>
        </div>

        <div className="space-y-2">
          {rules.map((rule) => (
            <div
              key={rule.packageName}
              className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 flex items-center justify-between"
            >
              <div>
                <p className="text-xs font-bold text-white">{rule.appDisplayName}</p>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  Daily limit: {rule.dailyLimitMinutes > 0 ? `${rule.dailyLimitMinutes}m` : "Strict"} • Earned window:{" "}
                  {rule.unlockDurationSeconds < 60 ? `${rule.unlockDurationSeconds}s` : `${rule.unlockDurationSeconds / 60}m`}
                </p>
              </div>

              <span
                className={`px-2.5 py-1 rounded-lg text-xs font-bold ${
                  rule.mode === "ALLOW"
                    ? "bg-emerald-950 text-emerald-400 border border-emerald-800"
                    : rule.mode === "BLOCK"
                    ? "bg-rose-950 text-rose-400 border border-rose-800"
                    : rule.mode === "DELAY"
                    ? "bg-amber-950 text-amber-400 border border-amber-800"
                    : "bg-sky-950 text-sky-400 border border-sky-800"
                }`}
              >
                {rule.mode}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Pairing Modal */}
      {showPairingModal && (
        <PairingModal
          childId={child.childId}
          childName={child.name}
          isOpen={showPairingModal}
          onClose={() => setShowPairingModal(false)}
        />
      )}
    </div>
  );
}
