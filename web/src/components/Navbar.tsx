"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { useFamily } from "@/context/FamilyContext";
import { Shield, User, Menu, X, LogOut, Smartphone, CheckCircle, AlertTriangle } from "lucide-react";
import ChildSelector from "./ChildSelector";

interface NavbarProps {
  onToggleSidebar?: () => void;
}

export default function Navbar({ onToggleSidebar }: NavbarProps) {
  const { user, signOut } = useAuth();
  const { family, activeChild, activeDevice } = useFamily();
  const [profileOpen, setProfileOpen] = useState(false);

  return (
    <header className="sticky top-0 z-40 bg-[#090D16]/90 backdrop-blur-md border-b border-[#1E293B] px-4 lg:px-8 py-3">
      <div className="flex items-center justify-between">
        {/* Left: Brand & Mobile Menu Toggle */}
        <div className="flex items-center gap-3">
          {onToggleSidebar && (
            <button
              onClick={onToggleSidebar}
              className="lg:hidden p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
              aria-label="Toggle navigation"
            >
              <Menu className="w-5 h-5" />
            </button>
          )}

          <Link href="/" className="flex items-center gap-2.5 group">
            <div className="w-8 h-8 rounded-lg bg-sky-500/10 border border-sky-500/30 flex items-center justify-center text-sky-400 group-hover:border-sky-400 transition">
              <Shield className="w-4 h-4" />
            </div>
            <div>
              <span className="font-extrabold text-sm tracking-wider text-white">DIGITAL DISCIPLINE</span>
              <span className="hidden sm:inline-block ml-2 text-[10px] uppercase font-bold text-sky-400 bg-sky-950/60 border border-sky-800/60 px-1.5 py-0.5 rounded">
                Parent Hub
              </span>
            </div>
          </Link>
        </div>

        {/* Center: Quick Child Switcher */}
        {user && family && (
          <div className="hidden md:flex items-center">
            <ChildSelector />
          </div>
        )}

        {/* Right: Protection Badge & Profile */}
        <div className="flex items-center gap-3">
          {activeDevice ? (
            <div className="hidden sm:flex items-center gap-2 px-2.5 py-1 rounded-full bg-slate-900 border border-slate-800 text-xs font-medium">
              {activeDevice.isProtectionActive ? (
                <>
                  <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                  <span className="text-emerald-400 font-semibold">Protected</span>
                </>
              ) : (
                <>
                  <span className="w-2 h-2 rounded-full bg-rose-400" />
                  <span className="text-rose-400 font-semibold">Needs Attention</span>
                </>
              )}
            </div>
          ) : (
            <Link
              href="/settings"
              className="hidden sm:flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-amber-950/60 border border-amber-800/80 text-xs font-bold text-amber-400 hover:bg-amber-900/40 transition"
            >
              <span className="w-2 h-2 rounded-full bg-amber-400 animate-pulse" />
              <span>Unpaired Device</span>
            </Link>
          )}

          {user ? (
            <div className="relative">
              <button
                onClick={() => setProfileOpen(!profileOpen)}
                className="flex items-center gap-2 p-1.5 rounded-lg bg-slate-900 border border-slate-800 hover:border-slate-700 transition"
              >
                <div className="w-7 h-7 rounded-full bg-sky-600/20 text-sky-400 font-bold flex items-center justify-center text-xs">
                  {user.email ? user.email[0].toUpperCase() : "P"}
                </div>
                <span className="hidden lg:inline text-xs font-medium text-slate-300 max-w-[120px] truncate">
                  {user.email || "Parent"}
                </span>
              </button>

              {profileOpen && (
                <div className="absolute right-0 mt-2 w-56 rounded-xl bg-[#0F172A] border border-slate-800 shadow-2xl p-2 z-50 animate-in fade-in slide-in-from-top-2">
                  <div className="px-3 py-2 border-b border-slate-800/80 mb-1">
                    <p className="text-xs text-slate-400">Signed in as</p>
                    <p className="text-xs font-bold text-white truncate">{user.email}</p>
                    {family && (
                      <p className="text-[11px] text-sky-400 font-medium mt-1">🏠 {family.familyName}</p>
                    )}
                  </div>

                  <Link
                    href="/settings"
                    onClick={() => setProfileOpen(false)}
                    className="flex items-center gap-2 px-3 py-2 rounded-lg text-xs text-slate-300 hover:text-white hover:bg-slate-800/60 transition"
                  >
                    <Smartphone className="w-3.5 h-3.5" />
                    Family & Pairing Settings
                  </Link>

                  <button
                    onClick={() => {
                      setProfileOpen(false);
                      signOut();
                    }}
                    className="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-xs text-rose-400 hover:bg-rose-950/30 transition mt-1"
                  >
                    <LogOut className="w-3.5 h-3.5" />
                    Sign Out
                  </button>
                </div>
              )}
            </div>
          ) : (
            <Link
              href="/login"
              className="text-xs font-bold px-3.5 py-1.5 rounded-lg bg-sky-600 hover:bg-sky-500 text-white transition shadow-sm"
            >
              Parent Sign In
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
