"use client";

import React, { useState } from "react";
import { useFamily } from "@/context/FamilyContext";
import { ChevronDown, Plus, Check } from "lucide-react";
import Link from "next/link";

export default function ChildSelector() {
  const { family, childrenList, activeChild, setActiveChild } = useFamily();
  const [isOpen, setIsOpen] = useState(false);

  if (!family || childrenList.length === 0) return null;

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-800 hover:border-slate-700 transition text-xs font-semibold text-white"
      >
        <span className="text-sm">👶</span>
        <span>{activeChild ? `${activeChild.name} (${activeChild.age} yrs)` : "Select Child"}</span>
        <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
      </button>

      {isOpen && (
        <div className="absolute left-0 mt-2 w-56 rounded-xl bg-[#0F172A] border border-slate-800 shadow-2xl p-2 z-50 animate-in fade-in slide-in-from-top-2">
          <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400 px-2 py-1">
            Family Children
          </p>
          <div className="space-y-1 my-1">
            {childrenList.map((kid) => {
              const isSelected = activeChild?.childId === kid.childId;
              return (
                <button
                  key={kid.childId}
                  onClick={() => {
                    setActiveChild(kid);
                    setIsOpen(false);
                  }}
                  className={`w-full flex items-center justify-between px-2.5 py-1.5 rounded-lg text-xs font-medium transition ${
                    isSelected
                      ? "bg-sky-500/20 text-sky-300 font-bold"
                      : "text-slate-300 hover:bg-slate-800/80"
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <span>👶</span>
                    <span>{kid.name}</span>
                    <span className="text-[10px] text-slate-500">({kid.age}y)</span>
                  </div>
                  {isSelected && <Check className="w-3.5 h-3.5 text-sky-400" />}
                </button>
              );
            })}
          </div>

          <div className="border-t border-slate-800 pt-1 mt-1">
            <Link
              href="/settings"
              onClick={() => setIsOpen(false)}
              className="flex items-center gap-2 px-2.5 py-1.5 rounded-lg text-xs font-medium text-sky-400 hover:bg-sky-950/40 transition"
            >
              <Plus className="w-3.5 h-3.5" />
              Manage or Add Child
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
