"use client";

import React, { useState } from "react";
import { useFamily } from "@/context/FamilyContext";
import { CloudScheduleDto } from "@/types";
import {
  Calendar,
  Clock,
  Plus,
  Trash2,
  Save,
  CheckCircle2,
  BookOpen,
  Moon,
  School,
} from "lucide-react";

export default function SchedulesPage() {
  const { activeChild, activePolicy, savePolicy } = useFamily();

  const [schedules, setSchedules] = useState<CloudScheduleDto[]>(
    activePolicy?.schedules || [
      {
        scheduleId: "sched_school",
        name: "School Hours",
        packageName: "ALL_RESTRICTED",
        dayOfWeek: 2, // Mon..Fri
        startHour: 8,
        startMinute: 30,
        endHour: 15,
        endMinute: 30,
        isBlocked: true,
      },
      {
        scheduleId: "sched_sleep",
        name: "Bedtime / Sleep",
        packageName: "ALL_RESTRICTED",
        dayOfWeek: 1, // Sun..Sat
        startHour: 21,
        startMinute: 30,
        endHour: 6,
        endMinute: 30,
        isBlocked: true,
      },
    ]
  );

  const [isSaving, setIsSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);

  const handleUpdateSchedule = (index: number, updates: Partial<CloudScheduleDto>) => {
    const updated = [...schedules];
    updated[index] = { ...updated[index], ...updates };
    setSchedules(updated);
    setSaveSuccess(false);
  };

  const handleRemoveSchedule = (index: number) => {
    setSchedules(schedules.filter((_, i) => i !== index));
    setSaveSuccess(false);
  };

  const handleAddSchedule = (presetName: string, startH: number, startM: number, endH: number, endM: number) => {
    const newSched: CloudScheduleDto = {
      scheduleId: `sched_${Math.random().toString(36).substring(2, 8)}`,
      name: presetName,
      packageName: "ALL_RESTRICTED",
      dayOfWeek: 2,
      startHour: startH,
      startMinute: startM,
      endHour: endH,
      endMinute: endM,
      isBlocked: true,
    };
    setSchedules([...schedules, newSched]);
    setSaveSuccess(false);
  };

  const handleSavePolicy = async () => {
    if (!activePolicy) return;
    setIsSaving(true);
    try {
      const nextPolicy = {
        ...activePolicy,
        schedules,
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
      {/* Top Action Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800/80">
        <div>
          <h1 className="text-2xl font-black text-white tracking-tight">
            Recurring Schedules
          </h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Automated downtime during School, Study, and Bedtime for 👶 <strong>{childName}</strong>
          </p>
        </div>

        <button
          onClick={handleSavePolicy}
          disabled={isSaving}
          className="flex items-center gap-2 px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-black uppercase tracking-wider transition shadow-lg shadow-emerald-600/20 disabled:opacity-50"
        >
          <Save className="w-4 h-4" />
          {isSaving ? "Saving..." : "Save Schedules"}
        </button>
      </div>

      {saveSuccess && (
        <div className="p-4 rounded-xl bg-emerald-950/60 border border-emerald-800 text-emerald-300 text-xs font-semibold flex items-center gap-2 animate-in fade-in">
          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          <span>Schedules saved and pushed to cloud policy!</span>
        </div>
      )}

      {/* Quick Add Presets */}
      <div className="p-4 rounded-2xl bg-slate-900/60 border border-slate-800 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Calendar className="w-4 h-4 text-sky-400" />
          <span className="text-xs font-bold text-white">Add Schedule Preset:</span>
        </div>

        <div className="flex flex-wrap gap-2">
          <button
            onClick={() => handleAddSchedule("School Hours", 8, 30, 15, 30)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-bold text-slate-200 transition"
          >
            <School className="w-3.5 h-3.5 text-sky-400" />
            + School (8:30 AM - 3:30 PM)
          </button>
          <button
            onClick={() => handleAddSchedule("Study & Homework", 16, 30, 18, 30)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-bold text-slate-200 transition"
          >
            <BookOpen className="w-3.5 h-3.5 text-emerald-400" />
            + Study (4:30 PM - 6:30 PM)
          </button>
          <button
            onClick={() => handleAddSchedule("Night Bedtime", 21, 30, 6, 30)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-bold text-slate-200 transition"
          >
            <Moon className="w-3.5 h-3.5 text-indigo-400" />
            + Bedtime (9:30 PM - 6:30 AM)
          </button>
        </div>
      </div>

      {/* Schedules List */}
      <div className="space-y-4">
        {schedules.map((schedule, index) => {
          const startFormatted = `${schedule.startHour.toString().padStart(2, "0")}:${schedule.startMinute.toString().padStart(2, "0")}`;
          const endFormatted = `${schedule.endHour.toString().padStart(2, "0")}:${schedule.endMinute.toString().padStart(2, "0")}`;

          return (
            <div
              key={schedule.scheduleId || index}
              className="p-5 rounded-2xl bg-[#0F172A] border border-slate-800 space-y-4"
            >
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-sky-500/10 text-sky-400 flex items-center justify-center">
                    <Clock className="w-5 h-5" />
                  </div>
                  <div>
                    <input
                      type="text"
                      value={schedule.name || "Schedule"}
                      onChange={(e) => handleUpdateSchedule(index, { name: e.target.value })}
                      className="text-sm font-bold text-white bg-transparent border-b border-transparent hover:border-slate-700 focus:border-sky-500 focus:outline-none px-1"
                    />
                    <p className="text-[11px] text-slate-400">
                      Active: {startFormatted} to {endFormatted}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <span className="px-2.5 py-1 rounded-lg bg-rose-950/60 border border-rose-800/80 text-rose-400 text-xs font-bold">
                    ⛔ BLOCK ALL RESTRICTED
                  </span>

                  <button
                    onClick={() => handleRemoveSchedule(index)}
                    className="p-2 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-rose-950/30 transition"
                    title="Delete schedule"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* Time Pickers & Scope */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-3 border-t border-slate-800/60 text-xs">
                <div>
                  <label className="block text-[11px] font-bold text-slate-400 mb-1">
                    Start Time
                  </label>
                  <input
                    type="time"
                    value={startFormatted}
                    onChange={(e) => {
                      const [h, m] = e.target.value.split(":").map(Number);
                      handleUpdateSchedule(index, { startHour: h || 0, startMinute: m || 0 });
                    }}
                    className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs text-white focus:outline-none focus:border-sky-500"
                  />
                </div>

                <div>
                  <label className="block text-[11px] font-bold text-slate-400 mb-1">
                    End Time
                  </label>
                  <input
                    type="time"
                    value={endFormatted}
                    onChange={(e) => {
                      const [h, m] = e.target.value.split(":").map(Number);
                      handleUpdateSchedule(index, { endHour: h || 0, endMinute: m || 0 });
                    }}
                    className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs text-white focus:outline-none focus:border-sky-500"
                  />
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
