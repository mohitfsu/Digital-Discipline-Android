"use client";

import React, { useState, useEffect } from "react";
import { useFamily } from "@/context/FamilyContext";
import { X, Smartphone, Clock, CheckCircle2, Copy, Check } from "lucide-react";

interface PairingModalProps {
  childId: string;
  childName: string;
  isOpen: boolean;
  onClose: () => void;
}

export default function PairingModal({ childId, childName, isOpen, onClose }: PairingModalProps) {
  const { generatePairingCode, activeDevice } = useFamily();
  const [code, setCode] = useState<string | null>(null);
  const [secondsLeft, setSecondsLeft] = useState<number>(900); // 15 mins
  const [copied, setCopied] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);

  useEffect(() => {
    if (isOpen && !code) {
      setIsGenerating(true);
      generatePairingCode(childId, childName)
        .then((newCode) => {
          setCode(newCode);
          setSecondsLeft(900);
        })
        .finally(() => setIsGenerating(false));
    }
  }, [isOpen, childId, childName, code, generatePairingCode]);

  useEffect(() => {
    if (!isOpen || !code) return;
    const timer = setInterval(() => {
      setSecondsLeft((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, [isOpen, code]);

  if (!isOpen) return null;

  const minutes = Math.floor(secondsLeft / 60);
  const seconds = secondsLeft % 60;
  const timeFormatted = `${minutes}:${seconds < 10 ? "0" : ""}${seconds}`;

  const copyToClipboard = () => {
    if (code) {
      navigator.clipboard.writeText(code);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in">
      <div className="w-full max-w-md bg-[#0F172A] border border-slate-800 rounded-2xl p-6 shadow-2xl relative">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="text-center">
          <div className="w-12 h-12 rounded-2xl bg-sky-500/10 border border-sky-500/30 text-sky-400 mx-auto flex items-center justify-center mb-3">
            <Smartphone className="w-6 h-6" />
          </div>
          <h3 className="text-lg font-black text-white">Pair {childName}'s Android Phone</h3>
          <p className="text-xs text-slate-400 mt-1">
            Open Digital Discipline on your child's phone and enter this single-use code:
          </p>

          {/* 6-Digit Code Box */}
          <div className="my-6 p-4 rounded-xl bg-slate-950 border-2 border-sky-500/40 flex items-center justify-center gap-3">
            {isGenerating ? (
              <p className="text-sm font-bold text-slate-400 animate-pulse">Generating code...</p>
            ) : (
              <>
                <span className="font-mono text-3xl font-black tracking-[0.3em] text-sky-400">
                  {code || "------"}
                </span>
                <button
                  onClick={copyToClipboard}
                  className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition ml-2"
                  title="Copy code"
                >
                  {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                </button>
              </>
            )}
          </div>

          <div className="flex items-center justify-center gap-1.5 text-xs text-slate-400 font-medium">
            <Clock className="w-3.5 h-3.5 text-amber-400" />
            <span>Code expires in <strong className="text-slate-200">{timeFormatted}</strong> (15 min TTL)</span>
          </div>

          {/* Step by step guide */}
          <div className="text-left mt-6 p-4 rounded-xl bg-slate-900/60 border border-slate-800/80 space-y-2.5 text-xs text-slate-300">
            <p className="font-bold text-white uppercase tracking-wider text-[10px]">How to Pair:</p>
            <div className="flex items-start gap-2">
              <span className="w-4 h-4 rounded-full bg-sky-500/20 text-sky-400 font-bold text-[10px] flex items-center justify-center shrink-0 mt-0.5">1</span>
              <span>Install & open Digital Discipline on the child's phone.</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="w-4 h-4 rounded-full bg-sky-500/20 text-sky-400 font-bold text-[10px] flex items-center justify-center shrink-0 mt-0.5">2</span>
              <span>Tap <strong>"🔗 PAIR DEVICE WITH CODE"</strong> on the main screen.</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="w-4 h-4 rounded-full bg-sky-500/20 text-sky-400 font-bold text-[10px] flex items-center justify-center shrink-0 mt-0.5">3</span>
              <span>Enter the 6-digit code above. Device will instantly bind and enforce your rules!</span>
            </div>
          </div>

          <button
            onClick={onClose}
            className="w-full mt-6 py-2.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white font-bold text-xs transition"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
}
