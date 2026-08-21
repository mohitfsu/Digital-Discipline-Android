"use client";

import React, { useState } from "react";
import { useFamily } from "@/context/FamilyContext";
import { CloudAppRuleDto, RuleMode } from "@/types";
import {
  Save,
  Plus,
  Trash2,
  CheckCircle2,
  Sparkles,
  Smartphone,
  Info,
} from "lucide-react";

export default function AppsPage() {
  const { activeChild, activePolicy, savePolicy } = useFamily();

  const [rules, setRules] = useState<CloudAppRuleDto[]>(
    activePolicy?.rules || [
      {
        packageName: "com.instagram.android",
        appDisplayName: "Instagram",
        mode: "EARN",
        isEnabled: true,
        dailyLimitMinutes: 30,
        unlockDurationSeconds: 600,
        interventionType: "PAUSE",
      },
      {
        packageName: "com.google.android.youtube",
        appDisplayName: "YouTube",
        mode: "EARN",
        isEnabled: true,
        dailyLimitMinutes: 45,
        unlockDurationSeconds: 900,
        interventionType: "BREATHING",
      },
      {
        packageName: "com.dts.freefireth",
        appDisplayName: "Gaming (Free Fire)",
        mode: "BLOCK",
        isEnabled: true,
        dailyLimitMinutes: 0,
        unlockDurationSeconds: 900,
        interventionType: "SQUATS",
      },
    ]
  );

  const [isSaving, setIsSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [showAddModal, setShowAddModal] = useState(false);
  const [customName, setCustomName] = useState("");
  const [customPkg, setCustomPkg] = useState("");
  const [customMode, setCustomMode] = useState<RuleMode>("EARN");

  const durationOptions = [
    { label: "10s (Test)", value: 10 },
    { label: "1 min", value: 60 },
    { label: "5 min", value: 300 },
    { label: "10 min", value: 600 },
    { label: "15 min", value: 900 },
    { label: "30 min", value: 1800 },
    { label: "60 min", value: 3600 },
  ];

  const dailyLimitOptions = [
    { label: "No Limit (0m)", value: 0 },
    { label: "15 min", value: 15 },
    { label: "30 min", value: 30 },
    { label: "45 min", value: 45 },
    { label: "60 min", value: 60 },
    { label: "90 min", value: 90 },
    { label: "120 min", value: 120 },
  ];

  const popularAppPresets = [
    { name: "TikTok", pkg: "com.zhiliaoapp.musically" },
    { name: "Snapchat", pkg: "com.snapchat.android" },
    { name: "Reddit", pkg: "com.reddit.frontpage" },
    { name: "Roblox", pkg: "com.roblox.client" },
    { name: "Twitter / X", pkg: "com.twitter.android" },
    { name: "Netflix", pkg: "com.netflix.mediaclient" },
    { name: "Discord", pkg: "com.discord" },
    { name: "Chrome Browser", pkg: "com.android.chrome" },
  ];

  const handleUpdateRule = (index: number, updates: Partial<CloudAppRuleDto>) => {
    const updated = [...rules];
    updated[index] = { ...updated[index], ...updates };
    setRules(updated);
    setSaveSuccess(false);
  };

  const handleRemoveRule = (index: number) => {
    setRules(rules.filter((_, i) => i !== index));
    setSaveSuccess(false);
  };

  const handleSavePolicy = async () => {
    if (!activePolicy) return;
    setIsSaving(true);
    try {
      const nextPolicy = {
        ...activePolicy,
        rules,
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

  const handleAddCustomApp = () => {
    if (!customPkg.trim()) return;
    const newRule: CloudAppRuleDto = {
      packageName: customPkg.trim(),
      appDisplayName: customName.trim() || customPkg.trim(),
      mode: customMode,
      isEnabled: true,
      dailyLimitMinutes: 30,
      unlockDurationSeconds: 600,
      interventionType: "PAUSE",
    };
    setRules([...rules, newRule]);
    setShowAddModal(false);
    setCustomName("");
    setCustomPkg("");
    setSaveSuccess(false);
  };

  const childName = activeChild?.name || "Child";
  const currentVersion = activePolicy?.version || 1;

  return (
    <div className="space-y-6">
      {/* Top Action Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800/80">
        <div>
          <h1 className="text-2xl font-black text-white tracking-tight">
            App Rules & Limits
          </h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Configuring rules for 👶 <strong>{childName}</strong> • Policy Version:{" "}
            <span className="text-sky-400 font-bold">v{currentVersion}</span>
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => setShowAddModal(true)}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-xs font-bold text-slate-200 transition"
          >
            <Plus className="w-3.5 h-3.5" />
            + Add Target App
          </button>

          <button
            onClick={handleSavePolicy}
            disabled={isSaving}
            className="flex items-center gap-2 px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-black uppercase tracking-wider transition shadow-lg shadow-emerald-600/20 disabled:opacity-50"
          >
            <Save className="w-4 h-4" />
            {isSaving ? "Pushing Policy..." : `Push Policy (v${currentVersion + 1})`}
          </button>
        </div>
      </div>

      {saveSuccess && (
        <div className="p-4 rounded-xl bg-emerald-950/60 border border-emerald-800 text-emerald-300 text-xs font-semibold flex items-center gap-2 animate-in fade-in">
          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          <span>
            Policy updated to <strong>v{currentVersion}</strong> and pushed to cloud! Child's phone will automatically synchronize.
          </span>
        </div>
      )}

      {/* Rules List */}
      <div className="space-y-3.5">
        {rules.map((rule, index) => (
          <div
            key={rule.packageName}
            className="p-5 rounded-2xl bg-[#0F172A] border border-slate-800 space-y-4 transition hover:border-slate-700"
          >
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center text-lg">
                  📱
                </div>
                <div>
                  <h3 className="text-sm font-bold text-white">{rule.appDisplayName}</h3>
                  <p className="text-[11px] font-mono text-slate-400">{rule.packageName}</p>
                </div>
              </div>

              {/* Mode Selector */}
              <div className="flex items-center gap-2">
                {(["EARN", "BLOCK", "DELAY", "ALLOW"] as RuleMode[]).map((mode) => {
                  const isSelected = rule.mode === mode;
                  return (
                    <button
                      key={mode}
                      onClick={() => handleUpdateRule(index, { mode })}
                      className={`px-3 py-1.5 rounded-lg text-xs font-black uppercase transition ${
                        isSelected
                          ? mode === "ALLOW"
                            ? "bg-emerald-600 text-white shadow-md"
                            : mode === "BLOCK"
                            ? "bg-rose-600 text-white shadow-md"
                            : mode === "DELAY"
                            ? "bg-amber-600 text-white shadow-md"
                            : "bg-sky-600 text-white shadow-md"
                          : "bg-slate-900 text-slate-400 hover:text-slate-200 border border-slate-800"
                      }`}
                    >
                      {mode}
                    </button>
                  );
                })}

                <button
                  onClick={() => handleRemoveRule(index)}
                  className="p-2 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-rose-950/30 transition ml-2"
                  title="Delete rule"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>

            {/* Config Sliders & Dropdowns */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-3 border-t border-slate-800/60 text-xs">
              {/* Daily Limit */}
              <div>
                <label className="block text-[11px] font-bold text-slate-400 mb-1">
                  Daily Usage Limit
                </label>
                <select
                  value={rule.dailyLimitMinutes}
                  onChange={(e) => handleUpdateRule(index, { dailyLimitMinutes: parseInt(e.target.value) })}
                  className="w-full px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-800 text-xs text-white focus:outline-none focus:border-sky-500"
                >
                  {dailyLimitOptions.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              </div>

              {/* Earned Duration */}
              <div>
                <label className="block text-[11px] font-bold text-slate-400 mb-1">
                  Earned Access Window
                </label>
                <select
                  value={rule.unlockDurationSeconds}
                  onChange={(e) => handleUpdateRule(index, { unlockDurationSeconds: parseInt(e.target.value) })}
                  className="w-full px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-800 text-xs text-white focus:outline-none focus:border-sky-500"
                >
                  {durationOptions.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              </div>

              {/* Intervention Type */}
              <div>
                <label className="block text-[11px] font-bold text-slate-400 mb-1">
                  Intervention Challenge
                </label>
                <select
                  value={rule.interventionType}
                  onChange={(e) => handleUpdateRule(index, { interventionType: e.target.value })}
                  className="w-full px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-800 text-xs text-white focus:outline-none focus:border-sky-500"
                >
                  <option value="PAUSE">Mindful Pause</option>
                  <option value="BREATHING">Box Breathing</option>
                  <option value="SQUATS">Squats Challenge</option>
                </select>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Add Custom App Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-md bg-[#0F172A] border border-slate-800 rounded-2xl p-6 shadow-2xl space-y-4">
            <h3 className="text-base font-black text-white">Add Target App to Restrict</h3>

            <div>
              <p className="text-[11px] font-bold text-sky-400 mb-2 uppercase tracking-wide">
                1-Tap Popular App Presets:
              </p>
              <div className="flex flex-wrap gap-1.5">
                {popularAppPresets.map((preset) => (
                  <button
                    key={preset.pkg}
                    type="button"
                    onClick={() => {
                      setCustomName(preset.name);
                      setCustomPkg(preset.pkg);
                    }}
                    className="px-2.5 py-1 rounded-lg bg-slate-900 border border-slate-800 hover:border-sky-500/50 text-[11px] font-medium text-slate-300 transition"
                  >
                    {preset.name}
                  </button>
                ))}
              </div>
            </div>

            <div className="border-t border-slate-800 pt-3 space-y-3">
              <div>
                <label className="block text-xs font-bold text-slate-300 mb-1">
                  App Display Name
                </label>
                <input
                  type="text"
                  value={customName}
                  onChange={(e) => setCustomName(e.target.value)}
                  placeholder="e.g. TikTok"
                  className="w-full px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-sky-500"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-300 mb-1">
                  Android Package Name
                </label>
                <input
                  type="text"
                  value={customPkg}
                  onChange={(e) => setCustomPkg(e.target.value)}
                  placeholder="e.g. com.zhiliaoapp.musically"
                  className="w-full px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-sky-500 font-mono"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-300 mb-1">
                  Initial Mode
                </label>
                <select
                  value={customMode}
                  onChange={(e) => setCustomMode(e.target.value as RuleMode)}
                  className="w-full px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs text-white focus:outline-none focus:border-sky-500"
                >
                  <option value="EARN">EARN (Physical / Mental Challenge)</option>
                  <option value="BLOCK">BLOCK (Strict Lock Screen)</option>
                  <option value="DELAY">DELAY (Mindful Pause Only)</option>
                  <option value="ALLOW">ALLOW (Unrestricted)</option>
                </select>
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowAddModal(false)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-slate-400 hover:text-white transition"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleAddCustomApp}
                disabled={!customPkg.trim()}
                className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold transition disabled:opacity-50"
              >
                Save App
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
