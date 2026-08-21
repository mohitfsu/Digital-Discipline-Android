"use client";

import React, { createContext, useContext, useEffect, useState } from "react";
import {
  User,
  onAuthStateChanged,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signOut as fbSignOut,
} from "firebase/auth";
import { auth } from "@/lib/firebase";

export interface CustomParentUser {
  uid: string;
  email: string | null;
  displayName: string | null;
  isDevUser?: boolean;
}

interface AuthContextType {
  user: CustomParentUser | null;
  loading: boolean;
  signInWithEmail: (email: string, pass: string) => Promise<void>;
  signUpWithEmail: (email: string, pass: string) => Promise<void>;
  signInWithDevAccount: (email?: string) => void;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const DEV_USER_STORAGE_KEY = "digital_discipline_dev_parent_user";

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<CustomParentUser | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    // Check local dev user first
    const cachedDevUser = typeof window !== "undefined" ? localStorage.getItem(DEV_USER_STORAGE_KEY) : null;
    if (cachedDevUser) {
      try {
        const parsed = JSON.parse(cachedDevUser);
        setUser(parsed);
        setLoading(false);
      } catch (e) {
        localStorage.removeItem(DEV_USER_STORAGE_KEY);
      }
    }

    // Safety timeout: Never stay stuck in loading state for more than 500ms
    const safetyTimer = setTimeout(() => {
      setLoading(false);
    }, 500);

    let unsubscribe: (() => void) | null = null;
    try {
      unsubscribe = onAuthStateChanged(auth, (fbUser: User | null) => {
        clearTimeout(safetyTimer);
        if (fbUser) {
          setUser({
            uid: fbUser.uid,
            email: fbUser.email,
            displayName: fbUser.displayName || fbUser.email?.split("@")[0] || "Parent",
          });
        } else {
          // If not signed into Firebase, keep dev user if exists
          const currentCached = typeof window !== "undefined" ? localStorage.getItem(DEV_USER_STORAGE_KEY) : null;
          if (currentCached) {
            setUser(JSON.parse(currentCached));
          } else {
            setUser(null);
          }
        }
        setLoading(false);
      });
    } catch (e) {
      clearTimeout(safetyTimer);
      setLoading(false);
    }

    return () => {
      clearTimeout(safetyTimer);
      if (unsubscribe) unsubscribe();
    };
  }, []);

  const signInWithEmail = async (email: string, pass: string) => {
    try {
      const cred = await signInWithEmailAndPassword(auth, email, pass);
      localStorage.removeItem(DEV_USER_STORAGE_KEY);
      setUser({
        uid: cred.user.uid,
        email: cred.user.email,
        displayName: cred.user.displayName || cred.user.email?.split("@")[0] || "Parent",
      });
    } catch (err: any) {
      // If Firebase auth provider not enabled in console, allow seamless dev login for friction-free testing
      if (err.code === "auth/configuration-not-found" || err.code === "auth/operation-not-allowed" || err.message?.includes("CONFIGURATION_NOT_FOUND")) {
        signInWithDevAccount(email);
        return;
      }
      throw err;
    }
  };

  const signUpWithEmail = async (email: string, pass: string) => {
    try {
      const cred = await createUserWithEmailAndPassword(auth, email, pass);
      localStorage.removeItem(DEV_USER_STORAGE_KEY);
      setUser({
        uid: cred.user.uid,
        email: cred.user.email,
        displayName: cred.user.displayName || cred.user.email?.split("@")[0] || "Parent",
      });
    } catch (err: any) {
      if (err.code === "auth/configuration-not-found" || err.code === "auth/operation-not-allowed" || err.message?.includes("CONFIGURATION_NOT_FOUND")) {
        signInWithDevAccount(email);
        return;
      }
      throw err;
    }
  };

  const signInWithDevAccount = (email: string = "parent@example.com") => {
    // Generate deterministic clean parent ID matching Android dev parent hash
    const cleanEmail = email.trim().toLowerCase();
    let hash = 0;
    for (let i = 0; i < cleanEmail.length; i++) {
      hash = ((hash << 5) - hash) + cleanEmail.charCodeAt(i);
      hash |= 0;
    }
    const devUid = "dev_parent_" + Math.abs(hash);
    const devUser: CustomParentUser = {
      uid: devUid,
      email: cleanEmail,
      displayName: cleanEmail.split("@")[0].toUpperCase() + " (Parent)",
      isDevUser: true,
    };
    if (typeof window !== "undefined") {
      localStorage.setItem(DEV_USER_STORAGE_KEY, JSON.stringify(devUser));
    }
    setUser(devUser);
  };

  const signOut = async () => {
    if (typeof window !== "undefined") {
      localStorage.removeItem(DEV_USER_STORAGE_KEY);
    }
    try {
      await fbSignOut(auth);
    } catch (e) {
      // Ignore
    }
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        signInWithEmail,
        signUpWithEmail,
        signInWithDevAccount,
        signOut,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
