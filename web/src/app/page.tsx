"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useFamily } from "@/context/FamilyContext";
import { useAuth } from "@/context/AuthContext";
import DeviceStatusBadge from "@/components/DeviceStatusBadge";
import PairingModal from "@/components/PairingModal";
import {
  Clock,
  ShieldCheck,
  ShieldAlert,
  Zap,
  Flame,
  ArrowRight,
  Plus,
  RefreshCw,
  LayoutGrid,
  Calendar,
  Smartphone,
} from "lucide-react";

export default function HomePage() {
  const { user } = useAuth();
  const {
    family,
    childrenList,
    activeChild,
    activeDevice,
    activePolicy,
    todaySummary,
    createFamily,
    createChild,
    refreshData,
  } = useFamily();

  const [newFamilyName, setNewFamilyName] = useState("");
  const [newChildName, setNewChildName] = useState("");
  const [newChildAge, setNewChildAge] = useState(10);
  const [isCreatingFamily, setIsCreatingFamily] = useState(false);
  const [isCreatingChild, setIsCreatingChild] = useState(false);
  const [showPairingModal, setShowPairingModal] = useState(false);

  // If not signed in yet, show parent sign in welcome
  if (!user) {
    return (
      <div className="max-w-md mx-auto py-12 text-center">
        <div className="w-16 h-16 rounded-3xl bg-sky-500/10 border border-sky-500/30 text-sky-400 mx-auto flex items-center justify-center mb-4">
          <ShieldCheck className="w-8 h-8" />
        </div>
        <h2 className="text-2xl font-black text-white">Parent Control Center</h2>
        <p className="text-xs text-slate-400 mt-1 mb-6">
          Sign in or launch instant demo mode to manage your family's digital discipline policies.
        </p>

        <div className="space-y-3">
          <Link
            href="/login"
            className="w-full py-3 px-4 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-black tracking-wide uppercase transition flex items-center justify-center gap-2 shadow-lg shadow-sky-600/30"
          >
            <Zap className="w-4 h-4 fill-current" />
            Sign In / Quick Demo Login
          </Link>
        </div>
      </div>
    );
  }

  // If no family exists yet
  if (!family) {
    return (
      <div className="max-w-md mx-auto py-12 text-center">
        <div className="w-16 h-16 rounded-3xl bg-sky-500/10 border border-sky-500/30 text-sky-400 mx-auto flex items-center justify-center mb-4">
          <ShieldCheck className="w-8 h-8" />
        </div>
        <h2 className="text-xl font-black text-white">Welcome, {user.displayName || "Parent"}!</h2>
        <p className="text-xs text-slate-400 mt-1 mb-6">
          Create your Family profile to start setting up digital boundaries for your children.
        </p>

        <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 text-left">
          <label className="block text-xs font-bold text-slate-300 mb-2">
            Your Family Name
          </label>
          <input
            type="text"
            value={newFamilyName}
            onChange={(e) => setNewFamilyName(e.target.value)}
            placeholder="e.g. Miller Family"
            className="w-full px-4 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-sky-500 mb-4"
          />

          <button
            onClick={async () => {
              if (newFamilyName.trim()) {
                setIsCreatingFamily(true);
                try {
                  await createFamily(newFamilyName);
                } finally {
                  setIsCreatingFamily(false);
                }
              }
            }}
            disabled={isCreatingFamily || !newFamilyName.trim()}
            className="w-full py-2.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-black tracking-wide uppercase transition disabled:opacity-50"
          >
            {isCreatingFamily ? "Creating Family..." : "Create Family Profile"}
          </button>
        </div>
      </div>
    );
  }

  // If family exists but no children added yet
  if (childrenList.length === 0) {
    return (
      <div className="max-w-md mx-auto py-12 text-center">
        <div className="w-16 h-16 rounded-3xl bg-sky-500/10 border border-sky-500/30 text-sky-400 mx-auto flex items-center justify-center mb-4">
          <span>👶</span>
        </div>
        <h2 className="text-xl font-black text-white">Add Your First Child</h2>
        <p className="text-xs text-slate-400 mt-1 mb-6">
          Add a profile for your child to manage their apps and generate a phone pairing code.
        </p>

        <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 text-left space-y-4">
          <div>
            <label className="block text-xs font-bold text-slate-300 mb-1.5">
              Child's Name
            </label>
            <input
              type="text"
              value={newChildName}
              onChange={(e) => setNewChildName(e.target.value)}
              placeholder="e.g. Alex"
              className="w-full px-4 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-sky-500"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-300 mb-1.5">
              Age
            </label>
            <input
              type="number"
              value={newChildAge}
              onChange={(e) => setNewChildAge(parseInt(e.target.value) || 10)}
              min={3}
              max={18}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-sky-500"
            />
          </div>

          <button
            onClick={async () => {
              if (newChildName.trim()) {
                setIsCreatingChild(true);
                try {
                  const created = await createChild(newChildName, newChildAge);
                  setShowPairingModal(true);
                } finally {
                  setIsCreatingChild(false);
                }
              }
            }}
            disabled={isCreatingChild || !newChildName.trim()}
            className="w-full py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-black tracking-wide uppercase transition disabled:opacity-50"
          >
            {isCreatingChild ? "Adding Child..." : "+ Add Child & Pair Phone"}
          </button>
        </div>
      </div>
    );
  }

  const childName = activeChild?.name || "Child";
  const isDevicePaired = !!activeDevice;
  const totalScreenMinutes = isDevicePaired ? (todaySummary?.totalScreenTimeMinutes || 0) : 0;
  const totalBlocks = isDevicePaired ? (todaySummary?.totalBlocks || 0) : 0;
  const totalInterventions = isDevicePaired ? (todaySummary?.totalInterventions || 0) : 0;
  const earnedMinutes = isDevicePaired ? (totalInterventions * 10) : 0;

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800/80">
        <div>
          <h1 className="text-2xl font-black text-white tracking-tight">
            {childName}&apos;s Daily Discipline
          </h1>
          <p className="text-xs text-slate-400 mt-0.5">
            {isDevicePaired
              ? "Real-time protection overview and today's digital boundaries."
              : "Child profile active. Pair an Android device to start real-time enforcement."}
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowPairingModal(true)}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-sky-600/20 hover:bg-sky-600/30 border border-sky-500/40 text-sky-300 text-xs font-bold transition"
          >
            <Smartphone className="w-3.5 h-3.5" />
            Pair Phone (Code)
          </button>

          <button
            onClick={() => refreshData()}
            className="p-2 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 text-slate-400 hover:text-white transition"
            title="Refresh"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Onboarding Wizard for Unpaired Child */}
      {!isDevicePaired && (
        <div className="p-6 rounded-3xl bg-gradient-to-br from-amber-950/40 via-[#0F172A] to-slate-900 border-2 border-amber-500/40 shadow-2xl space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-start gap-4">
              <div className="w-12 h-12 rounded-2xl bg-amber-500/20 text-amber-400 border border-amber-500/40 flex items-center justify-center text-2xl shrink-0">
                📲
              </div>
              <div>
                <span className="text-[10px] font-extrabold uppercase tracking-widest text-amber-400 bg-amber-950/80 px-2 py-0.5 rounded-full border border-amber-800">
                  Step 1 • Phone Setup
                </span>
                <h2 className="text-base font-black text-white mt-1">
                  Pair {childName}&apos;s Phone to Activate Protection
                </h2>
                <p className="text-xs text-slate-300 mt-1 max-w-xl">
                  {childName}&apos;s profile is created, but no phone is linked yet. Generate a 6-digit single-use code to link the phone so your rules and limits can take effect.
                </p>
              </div>
            </div>

            <button
              onClick={() => setShowPairingModal(true)}
              className="px-5 py-3 rounded-2xl bg-amber-500 hover:bg-amber-400 text-slate-950 text-xs font-black uppercase tracking-wider transition shadow-xl shadow-amber-500/20 shrink-0 flex items-center justify-center gap-2"
            >
              <Smartphone className="w-4 h-4" />
              Pair Phone (Get 6-Digit Code)
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-3 border-t border-slate-800/80 text-xs text-slate-400">
            <div className="flex items-center gap-2">
              <span className="w-5 h-5 rounded-full bg-slate-800 text-slate-200 font-bold flex items-center justify-center text-[10px]">1</span>
              <span>Install app on {childName}&apos;s phone</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-5 h-5 rounded-full bg-slate-800 text-slate-200 font-bold flex items-center justify-center text-[10px]">2</span>
              <span>Tap &quot;Pair Device with Code&quot;</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-5 h-5 rounded-full bg-slate-800 text-slate-200 font-bold flex items-center justify-center text-[10px]">3</span>
              <span>Enter 6-digit code to connect</span>
            </div>
          </div>
        </div>
      )}

      {/* Device Status & Health */}
      <DeviceStatusBadge
        device={activeDevice}
        policyVersion={activePolicy?.version}
        onRefresh={() => refreshData()}
        onPair={() => setShowPairingModal(true)}
      />

      {/* 4 Summary Stat Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3.5">
        <div className="p-4 rounded-2xl bg-[#0F172A] border border-slate-800 flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-400">Screen Time</span>
            <div className="w-7 h-7 rounded-lg bg-sky-500/10 text-sky-400 flex items-center justify-center">
              <Clock className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-black text-white">{totalScreenMinutes}</span>
            <span className="text-xs font-bold text-slate-400 ml-1">min</span>
            <p className="text-[11px] text-emerald-400 font-semibold mt-0.5">
              {isDevicePaired ? "Within daily goal" : "No device paired"}
            </p>
          </div>
        </div>

        <div className="p-4 rounded-2xl bg-[#0F172A] border border-slate-800 flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-400">Blocked Attempts</span>
            <div className="w-7 h-7 rounded-lg bg-rose-500/10 text-rose-400 flex items-center justify-center">
              <ShieldAlert className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-black text-rose-400">{totalBlocks}</span>
            <span className="text-xs font-bold text-slate-400 ml-1">times</span>
            <p className="text-[11px] text-slate-400 mt-0.5">
              {isDevicePaired ? "Urges prevented" : "Protection waiting"}
            </p>
          </div>
        </div>

        <div className="p-4 rounded-2xl bg-[#0F172A] border border-slate-800 flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-400">Interventions Done</span>
            <div className="w-7 h-7 rounded-lg bg-emerald-500/10 text-emerald-400 flex items-center justify-center">
              <ShieldCheck className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-black text-emerald-400">{totalInterventions}</span>
            <span className="text-xs font-bold text-slate-400 ml-1">completed</span>
            <p className="text-[11px] text-slate-400 mt-0.5">
              {isDevicePaired ? "Pause & squats challenges" : "No challenge events"}
            </p>
          </div>
        </div>

        <div className="p-4 rounded-2xl bg-[#0F172A] border border-slate-800 flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-400">Earned Access</span>
            <div className="w-7 h-7 rounded-lg bg-amber-500/10 text-amber-400 flex items-center justify-center">
              <Flame className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-black text-amber-400">{earnedMinutes}</span>
            <span className="text-xs font-bold text-slate-400 ml-1">min</span>
            <p className="text-[11px] text-slate-400 mt-0.5">
              {isDevicePaired ? "Earned via discipline" : "Connect device"}
            </p>
          </div>
        </div>
      </div>

      {/* Quick Navigation Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-2">
        <Link
          href="/apps"
          className="p-5 rounded-2xl bg-[#0F172A] border border-slate-800 hover:border-sky-500/50 transition group flex flex-col justify-between"
        >
          <div>
            <div className="w-10 h-10 rounded-xl bg-sky-500/10 text-sky-400 flex items-center justify-center mb-3">
              <LayoutGrid className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-black text-white group-hover:text-sky-400 transition">
              App Rules & Limits
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              Configure Instagram, YouTube, and games to Allow, Block, Delay, or Earn.
            </p>
          </div>
          <div className="flex items-center gap-1 text-xs font-bold text-sky-400 mt-4">
            <span>Manage Apps</span>
            <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition" />
          </div>
        </Link>

        <Link
          href="/schedules"
          className="p-5 rounded-2xl bg-[#0F172A] border border-slate-800 hover:border-emerald-500/50 transition group flex flex-col justify-between"
        >
          <div>
            <div className="w-10 h-10 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center mb-3">
              <Calendar className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-black text-white group-hover:text-emerald-400 transition">
              Weekly Schedules
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              Set automated quiet hours during School, Homework, and Bedtime.
            </p>
          </div>
          <div className="flex items-center gap-1 text-xs font-bold text-emerald-400 mt-4">
            <span>Configure Schedules</span>
            <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition" />
          </div>
        </Link>

        <Link
          href="/interventions"
          className="p-5 rounded-2xl bg-[#0F172A] border border-slate-800 hover:border-amber-500/50 transition group flex flex-col justify-between"
        >
          <div>
            <div className="w-10 h-10 rounded-xl bg-amber-500/10 text-amber-400 flex items-center justify-center mb-3">
              <Zap className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-black text-white group-hover:text-amber-400 transition">
              Intervention Timers
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              Customize duration for Mindful Pause (10-30s), Breathing, and Squat counts.
            </p>
          </div>
          <div className="flex items-center gap-1 text-xs font-bold text-amber-400 mt-4">
            <span>Set Challenge Timers</span>
            <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition" />
          </div>
        </Link>
      </div>

      {/* Pairing Modal */}
      {showPairingModal && activeChild && (
        <PairingModal
          childId={activeChild.childId}
          childName={activeChild.name}
          isOpen={showPairingModal}
          onClose={() => setShowPairingModal(false)}
        />
      )}
    </div>
  );
}
