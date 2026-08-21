"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Home,
  LayoutGrid,
  Calendar,
  Zap,
  BarChart2,
  TrendingUp,
  Settings,
  Shield,
  Smartphone,
  PlusCircle,
} from "lucide-react";
import { useFamily } from "@/context/FamilyContext";

interface SidebarProps {
  isOpen?: boolean;
  onClose?: () => void;
}

export default function Sidebar({ isOpen, onClose }: SidebarProps) {
  const pathname = usePathname();
  const { family, activeChild, childrenList, setActiveChild } = useFamily();

  const navItems = [
    { name: "Overview", href: "/", icon: Home },
    { name: "App Rules & Limits", href: "/apps", icon: LayoutGrid },
    { name: "Schedules", href: "/schedules", icon: Calendar },
    { name: "Interventions", href: "/interventions", icon: Zap },
    { name: "Activity & Trends", href: "/activity", icon: BarChart2 },
    { name: "Behaviour Insights", href: "/insights", icon: TrendingUp },
    { name: "Device & Settings", href: "/settings", icon: Settings },
  ];

  return (
    <>
      {/* Mobile Backdrop */}
      {isOpen && (
        <div
          onClick={onClose}
          className="fixed inset-0 bg-black/60 backdrop-blur-sm z-40 lg:hidden"
        />
      )}

      <aside
        className={`fixed top-0 bottom-0 left-0 w-64 bg-[#0B0F19] border-r border-[#1E293B] z-50 flex flex-col transition-transform duration-200 lg:translate-x-0 ${
          isOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        {/* Top Header */}
        <div className="p-5 border-b border-[#1E293B] flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-sky-500/10 border border-sky-500/30 flex items-center justify-center text-sky-400">
              <Shield className="w-4 h-4" />
            </div>
            <div>
              <p className="font-black text-xs tracking-wider text-white">DIGITAL DISCIPLINE</p>
              <p className="text-[11px] text-slate-400 font-medium">{family ? family.familyName : "Parent Dashboard"}</p>
            </div>
          </div>
        </div>

        {/* Children Quick Switcher in Sidebar */}
        {family && childrenList.length > 0 && (
          <div className="p-4 border-b border-[#1E293B]/70 bg-slate-950/40">
            <p className="text-[10px] font-bold tracking-wider text-slate-400 uppercase mb-2">
              Active Child Profile
            </p>
            <div className="space-y-1">
              {childrenList.map((kid) => {
                const isSelected = activeChild?.childId === kid.childId;
                return (
                  <button
                    key={kid.childId}
                    onClick={() => {
                      setActiveChild(kid);
                      if (onClose) onClose();
                    }}
                    className={`w-full flex items-center justify-between px-3 py-2 rounded-lg text-xs font-semibold transition ${
                      isSelected
                        ? "bg-sky-500/15 border border-sky-500/40 text-sky-300"
                        : "text-slate-400 hover:text-slate-200 hover:bg-slate-900/80"
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      <span>👶</span>
                      <span>{kid.name}</span>
                      <span className="text-[10px] text-slate-500">({kid.age}y)</span>
                    </div>
                    {isSelected && <span className="w-1.5 h-1.5 rounded-full bg-sky-400" />}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* Navigation Links */}
        <nav className="flex-1 p-3 space-y-1 overflow-y-auto">
          {navItems.map((item) => {
            const isActive = pathname === item.href;
            const Icon = item.icon;
            return (
              <Link
                key={item.href}
                href={item.href}
                onClick={onClose}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-xs font-semibold transition ${
                  isActive
                    ? "bg-sky-600 text-white shadow-md shadow-sky-600/20"
                    : "text-slate-400 hover:text-white hover:bg-slate-800/60"
                }`}
              >
                <Icon className={`w-4 h-4 ${isActive ? "text-white" : "text-slate-400"}`} />
                <span>{item.name}</span>
              </Link>
            );
          })}
        </nav>

        {/* Footer Info */}
        <div className="p-4 border-t border-[#1E293B] bg-slate-950/60">
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            <p className="text-[11px] text-slate-400">Offline-First Engine</p>
          </div>
          <p className="text-[10px] text-slate-500 mt-1">Direct Cloud Policy Sync Active</p>
        </div>
      </aside>
    </>
  );
}
