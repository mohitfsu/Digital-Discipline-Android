"use client";

import React, { useState } from "react";
import { useFamily } from "@/context/FamilyContext";
import {
  Zap,
  Clock,
  Activity,
  KeyRound,
  Save,
  CheckCircle2,
  Sparkles,
  Shield,
} from "lucide-react";

export default function InterventionsPage() {
  const { activeChild, activePolicy, savePolicy } = useFamily();

  const [pauseDuration, setPauseDuration] = useState<number>(activePolicy?.pauseDurationSeconds || 10);
  const [breathingDuration, setBreathingDuration] = useState<number>(activePolicy?.breathingDurationSeconds || 30);
  const [squatsCount, setSquatsCount] = useState<number>(activePolicy?.squatsTargetCount || 10);

  const [isSaving, setIsSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);

  const handleSave = async () => {
    if (!activePolicy) return;
    setIsSaving(true);
    try {
      const nextPolicy = {
        ...activePolicy,
        pauseDurationSeconds: pauseDuration,
        breathingDurationSeconds: breathingDuration,
        squatsTargetCount: squatsCount,
      };
      await savePolicy(nextPolicy);
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 4000);
    } catch (e) {
      console.error("Save error:", e);
    } finally {
      setIsSaving(false);
    }
  };

  const childName = activeChild?.name || "Child";

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800/80">
        <div>
          <h1 className="text-2xl font-black text-white tracking-tight">
            Intervention Challenge Settings
          </h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Dopamine reset exercises and mindful barriers for 👶 <strong>{childName}</strong>
          </p>
        </div>

        <button
          onClick={handleSave}
          disabled={isSaving}
          className="flex items-center gap-2 px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-black uppercase tracking-wider transition shadow-lg shadow-emerald-600/20 disabled:opacity-50"
        >
          <Save className="w-4 h-4" />
          {isSaving ? "Saving..." : "Save Interventions"}
        </button>
      </div>

      {saveSuccess && (
        <div className="p-4 rounded-xl bg-emerald-950/60 border border-emerald-800 text-emerald-300 text-xs font-semibold flex items-center gap-2 animate-in fade-in">
          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          <span>Intervention parameters saved and pushed to cloud policy!</span>
        </div>
      )}

      {/* Escalation Sequence Visual */}
      <div className="p-5 rounded-2xl bg-gradient-to-br from-sky-950/40 via-slate-900 to-indigo-950/30 border border-sky-900/40 space-y-3">
        <div className="flex items-center gap-2">
          <Sparkles className="w-4 h-4 text-sky-400" />
          <h2 className="text-sm font-bold text-white uppercase tracking-wider text-xs">
            How Progressive Interventions Work
          </h2>
        </div>
        <p className="text-xs text-slate-300">
          When the child opens a restricted app, Digital Discipline delivers progressive challenges to reset compulsive urges before granting temporary screen time.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-3 pt-2">
          <div className="p-3 rounded-xl bg-slate-900/80 border border-slate-800">
            <span className="text-[10px] font-bold uppercase tracking-wider text-sky-400">
              Attempt #1
            </span>
            <p className="text-xs font-bold text-white mt-1">⏳ Mindful Pause</p>
            <p className="text-[11px] text-slate-400 mt-0.5">Short stillness countdown to break impulsive habit loops.</p>
          </div>

          <div className="p-3 rounded-xl bg-slate-900/80 border border-slate-800">
            <span className="text-[10px] font-bold uppercase tracking-wider text-teal-400">
              Attempt #2
            </span>
            <p className="text-xs font-bold text-white mt-1">🫁 Box Breathing</p>
            <p className="text-[11px] text-slate-400 mt-0.5">4-4-4-4 breathing cycle to down-regulate nervous system.</p>
          </div>

          <div className="p-3 rounded-xl bg-slate-900/80 border border-slate-800">
            <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-400">
              Attempt #3+
            </span>
            <p className="text-xs font-bold text-white mt-1">🏋️ Physical Squats</p>
            <p className="text-[11px] text-slate-400 mt-0.5">Movement challenge to earn digital access with effort.</p>
          </div>
        </div>
      </div>

      {/* Intervention Customization Cards */}
      <div className="space-y-4">
        {/* 1. Mindful Pause */}
        <div className="p-5 rounded-2xl bg-[#0F172A] border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-sky-500/10 text-sky-400 flex items-center justify-center text-lg">
                ⏳
              </div>
              <div>
                <h3 className="text-sm font-bold text-white">Mindful Pause Timer</h3>
                <p className="text-xs text-slate-400">Countdown duration required before app unlocks.</p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              {[10, 15, 30].map((sec) => (
                <button
                  key={sec}
                  onClick={() => setPauseDuration(sec)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-bold transition ${
                    pauseDuration === sec
                      ? "bg-sky-600 text-white shadow-md"
                      : "bg-slate-900 text-slate-400 hover:text-white border border-slate-800"
                  }`}
                >
                  {sec} seconds
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* 2. Box Breathing */}
        <div className="p-5 rounded-2xl bg-[#0F172A] border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-teal-500/10 text-teal-400 flex items-center justify-center text-lg">
                🫁
              </div>
              <div>
                <h3 className="text-sm font-bold text-white">Box Breathing Timer</h3>
                <p className="text-xs text-slate-400">Full guided breathing animation duration.</p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              {[15, 30, 60].map((sec) => (
                <button
                  key={sec}
                  onClick={() => setBreathingDuration(sec)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-bold transition ${
                    breathingDuration === sec
                      ? "bg-teal-600 text-white shadow-md"
                      : "bg-slate-900 text-slate-400 hover:text-white border border-slate-800"
                  }`}
                >
                  {sec} seconds
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* 3. Squats Target */}
        <div className="p-5 rounded-2xl bg-[#0F172A] border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center text-lg">
                🏋️
              </div>
              <div>
                <h3 className="text-sm font-bold text-white">Squats Challenge Target</h3>
                <p className="text-xs text-slate-400">Number of repetitions required to earn screen access.</p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              {[5, 10, 15, 20].map((reps) => (
                <button
                  key={reps}
                  onClick={() => setSquatsCount(reps)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-bold transition ${
                    squatsCount === reps
                      ? "bg-emerald-600 text-white shadow-md"
                      : "bg-slate-900 text-slate-400 hover:text-white border border-slate-800"
                  }`}
                >
                  {reps} reps
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* 4. Parent PIN Override Info */}
        <div className="p-5 rounded-2xl bg-[#0F172A] border border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-amber-500/10 text-amber-400 flex items-center justify-center text-lg">
              <KeyRound className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-white">Parent PIN Override</h3>
              <p className="text-xs text-slate-400">
                Default PIN is <strong className="text-amber-400 font-mono">1234</strong>. Unlocks phone for 15 minutes in emergencies.
              </p>
            </div>
          </div>

          <span className="px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-800 text-xs font-bold text-emerald-400">
            ✓ Enabled
          </span>
        </div>
      </div>
    </div>
  );
}
