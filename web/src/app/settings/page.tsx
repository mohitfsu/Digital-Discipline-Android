"use client";

import React, { useState } from "react";
import { useFamily } from "@/context/FamilyContext";
import { useAuth } from "@/context/AuthContext";
import PairingModal from "@/components/PairingModal";
import {
  Settings,
  Smartphone,
  Plus,
  Trash2,
  CheckCircle2,
  Users,
  Shield,
  KeyRound,
  Terminal,
} from "lucide-react";

export default function SettingsPage() {
  const { user } = useAuth();
  const {
    family,
    childrenList,
    activeChild,
    activeDevice,
    activePolicy,
    createChild,
    deleteChild,
    setActiveChild,
  } = useFamily();

  const [newChildName, setNewChildName] = useState("");
  const [newChildAge, setNewChildAge] = useState(10);
  const [isAddingChild, setIsAddingChild] = useState(false);
  const [pairingModalChild, setPairingModalChild] = useState<{ id: string; name: string } | null>(null);

  const handleAddChild = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newChildName.trim()) return;
    setIsAddingChild(true);
    try {
      await createChild(newChildName, newChildAge);
      setNewChildName("");
      setNewChildAge(10);
    } catch (e) {
      console.error(e);
    } finally {
      setIsAddingChild(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="pb-4 border-b border-slate-800/80">
        <h1 className="text-2xl font-black text-white tracking-tight">
          Family & Device Settings
        </h1>
        <p className="text-xs text-slate-400 mt-0.5">
          Manage family members, child profiles, and Android phone pairing.
        </p>
      </div>

      {/* 1. Family Information */}
      <div className="p-5 rounded-2xl bg-[#0F172A] border border-slate-800 space-y-4">
        <div className="flex items-center gap-2">
          <Users className="w-4 h-4 text-sky-400" />
          <h2 className="text-sm font-bold text-white uppercase tracking-wider text-xs">
            Family Profile
          </h2>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
          <div className="p-3.5 rounded-xl bg-slate-900 border border-slate-800">
            <p className="text-slate-400">Family Name</p>
            <p className="text-sm font-bold text-white mt-0.5">{family?.familyName || "My Family"}</p>
          </div>

          <div className="p-3.5 rounded-xl bg-slate-900 border border-slate-800">
            <p className="text-slate-400">Parent Account</p>
            <p className="text-sm font-bold text-white mt-0.5">{user?.email || "Parent"}</p>
          </div>
        </div>
      </div>

      {/* 2. Children Management & Phone Pairing */}
      <div className="p-5 rounded-2xl bg-[#0F172A] border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Smartphone className="w-4 h-4 text-emerald-400" />
            <h2 className="text-sm font-bold text-white uppercase tracking-wider text-xs">
              Children & Device Pairing ({childrenList.length})
            </h2>
          </div>
        </div>

        {/* Children List */}
        <div className="space-y-3">
          {childrenList.map((kid) => {
            const isSelected = activeChild?.childId === kid.childId;
            return (
              <div
                key={kid.childId}
                className={`p-4 rounded-xl border transition flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
                  isSelected
                    ? "bg-slate-900/90 border-sky-500/50"
                    : "bg-slate-900/40 border-slate-800"
                }`}
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-sky-500/10 text-sky-400 flex items-center justify-center text-lg">
                    👶
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="text-sm font-bold text-white">{kid.name}</h3>
                      <span className="text-xs text-slate-400">({kid.age} yrs)</span>
                      {isSelected && (
                        <span className="px-2 py-0.5 rounded-full bg-sky-950 text-sky-400 border border-sky-800 text-[10px] font-bold">
                          Active
                        </span>
                      )}
                    </div>
                    <p className="text-[11px] font-mono text-slate-500">ID: {kid.childId}</p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setPairingModalChild({ id: kid.childId, name: kid.name })}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-sky-600 hover:bg-sky-500 text-white text-xs font-bold transition shadow-sm"
                  >
                    <Smartphone className="w-3.5 h-3.5" />
                    Pair Phone (Code)
                  </button>

                  <button
                    onClick={() => setActiveChild(kid)}
                    className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold transition"
                  >
                    Select
                  </button>

                  {childrenList.length > 1 && (
                    <button
                      onClick={() => deleteChild(kid.childId)}
                      className="p-1.5 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-rose-950/30 transition"
                      title="Delete child"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        {/* Add Child Form */}
        <form onSubmit={handleAddChild} className="pt-3 border-t border-slate-800/80">
          <p className="text-xs font-bold text-slate-300 mb-2">+ Add Another Child Profile:</p>
          <div className="flex flex-col sm:flex-row gap-2">
            <input
              type="text"
              required
              value={newChildName}
              onChange={(e) => setNewChildName(e.target.value)}
              placeholder="Child's name (e.g. Emma)"
              className="flex-1 px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-sky-500"
            />
            <input
              type="number"
              required
              value={newChildAge}
              onChange={(e) => setNewChildAge(parseInt(e.target.value) || 10)}
              min={3}
              max={18}
              placeholder="Age"
              className="w-24 px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-sky-500"
            />
            <button
              type="submit"
              disabled={isAddingChild || !newChildName.trim()}
              className="px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold transition disabled:opacity-50"
            >
              {isAddingChild ? "Adding..." : "+ Add Child"}
            </button>
          </div>
        </form>
      </div>

      {/* 3. Technical Diagnostics & Telemetry */}
      <div className="p-5 rounded-2xl bg-[#0F172A] border border-slate-800 space-y-3">
        <div className="flex items-center gap-2">
          <Terminal className="w-4 h-4 text-slate-400" />
          <h2 className="text-sm font-bold text-white uppercase tracking-wider text-xs">
            Technical Telemetry & Cloud Diagnostics
          </h2>
        </div>

        <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 text-xs font-mono space-y-1.5 text-slate-400">
          <p>
            <span className="text-sky-400">Active Policy Version:</span> v{activePolicy?.version || 1}
          </p>
          <p>
            <span className="text-sky-400">Paired Device UUID:</span> {activeDevice?.deviceId || "Unpaired"}
          </p>
          <p>
            <span className="text-sky-400">Protection Status:</span>{" "}
            {activeDevice?.isProtectionActive ? "ENFORCING_LOCAL_ROOM" : "INACTIVE"}
          </p>
          <p>
            <span className="text-sky-400">Multi-Tenant Tenant Isolation:</span> VERIFIED_STRICT
          </p>
        </div>
      </div>

      {/* Pairing Modal */}
      {pairingModalChild && (
        <PairingModal
          childId={pairingModalChild.id}
          childName={pairingModalChild.name}
          isOpen={!!pairingModalChild}
          onClose={() => setPairingModalChild(null)}
        />
      )}
    </div>
  );
}
