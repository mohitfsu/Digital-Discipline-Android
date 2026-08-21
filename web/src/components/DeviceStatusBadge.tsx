"use client";

import React from "react";
import { DeviceDto } from "@/types";
import { CheckCircle2, AlertTriangle, Smartphone, RefreshCw } from "lucide-react";

interface DeviceStatusBadgeProps {
  device: DeviceDto | null;
  policyVersion?: number;
  onRefresh?: () => void;
  onPair?: () => void;
}

export default function DeviceStatusBadge({ device, policyVersion, onRefresh, onPair }: DeviceStatusBadgeProps) {
  if (!device) {
    return (
      <div className="p-5 rounded-2xl bg-gradient-to-r from-amber-950/40 via-slate-900 to-amber-950/20 border-2 border-amber-500/50 shadow-lg flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3.5">
          <div className="w-12 h-12 rounded-2xl bg-amber-500/15 border border-amber-500/30 flex items-center justify-center text-amber-400 shrink-0 animate-pulse">
            <Smartphone className="w-6 h-6" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <p className="text-sm font-black text-white">Action Required: No Phone Paired</p>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold uppercase bg-amber-950 text-amber-400 border border-amber-700">
                Setup Needed
              </span>
            </div>
            <p className="text-xs text-slate-300 mt-1">
              Pair your child's Android phone using a 6-digit code so rules & discipline take effect.
            </p>
          </div>
        </div>

        {onPair && (
          <button
            onClick={onPair}
            className="flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 text-xs font-black uppercase tracking-wider transition shadow-lg shrink-0"
          >
            <Smartphone className="w-4 h-4" />
            Pair Phone Now
          </button>
        )}
      </div>
    );
  }

  const isProtected = device.isProtectionActive;

  return (
    <div className="space-y-3">
      {/* Main Status Card */}
      <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div
            className={`w-10 h-10 rounded-xl flex items-center justify-center ${
              isProtected
                ? "bg-emerald-500/10 border border-emerald-500/30 text-emerald-400"
                : "bg-rose-500/10 border border-rose-500/30 text-rose-400"
            }`}
          >
            {isProtected ? <CheckCircle2 className="w-5 h-5" /> : <AlertTriangle className="w-5 h-5" />}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-sm font-bold text-white">{device.deviceModel || "Android Device"}</span>
              <span
                className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase ${
                  isProtected
                    ? "bg-emerald-950/60 text-emerald-400 border border-emerald-800/60"
                    : "bg-rose-950/60 text-rose-400 border border-rose-800/60"
                }`}
              >
                {isProtected ? "Protected" : "Needs Attention"}
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-0.5">
              Active Policy: <span className="font-semibold text-slate-300">v{device.activePolicyVersion || policyVersion || 1}</span> •{" "}
              {device.androidVersion || "Android"}
            </p>
          </div>
        </div>

        {onRefresh && (
          <button
            onClick={onRefresh}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-300 transition"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            Sync Status
          </button>
        )}
      </div>

      {/* Prominent Warning Banner if Protection Disabled */}
      {!isProtected && (
        <div className="p-3.5 rounded-xl bg-rose-950/50 border border-rose-800/80 flex items-start gap-3 animate-pulse">
          <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
          <div>
            <p className="text-xs font-bold text-rose-200 uppercase tracking-wide">
              Child Protection Inactive
            </p>
            <p className="text-xs text-rose-300/90 mt-0.5">
              Accessibility service or overlay permission was disabled on the child's phone. Open Digital Discipline on the phone to restore enforcement.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
