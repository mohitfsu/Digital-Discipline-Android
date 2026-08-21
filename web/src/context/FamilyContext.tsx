"use client";

import React, { createContext, useContext, useEffect, useState, useCallback } from "react";
import {
  collection,
  doc,
  getDoc,
  getDocs,
  setDoc,
  updateDoc,
  deleteDoc,
  query,
  where,
  onSnapshot,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "./AuthContext";
import {
  FamilyDto,
  ChildDto,
  DeviceDto,
  CloudPolicyDto,
  CloudAppRuleDto,
  DailySummaryDto,
  PairingCodeDto,
} from "@/types";

interface FamilyContextType {
  family: FamilyDto | null;
  childrenList: ChildDto[];
  activeChild: ChildDto | null;
  activeDevice: DeviceDto | null;
  activePolicy: CloudPolicyDto | null;
  todaySummary: DailySummaryDto | null;
  loading: boolean;
  createFamily: (familyName: string) => Promise<FamilyDto>;
  createChild: (name: string, age: number) => Promise<ChildDto>;
  deleteChild: (childId: string) => Promise<void>;
  setActiveChild: (child: ChildDto) => void;
  savePolicy: (policy: CloudPolicyDto) => Promise<void>;
  generatePairingCode: (childId: string, childName: string) => Promise<string>;
  refreshData: () => Promise<void>;
}

const FamilyContext = createContext<FamilyContextType | undefined>(undefined);

// Local resilient storage fallback keys for zero-failure operation
const LOCAL_FAMILY_KEY = "digital_discipline_local_family";
const LOCAL_CHILDREN_KEY = "digital_discipline_local_children";
const LOCAL_POLICIES_KEY = "digital_discipline_local_policies";
const LOCAL_DEVICES_KEY = "digital_discipline_local_devices";

export function FamilyProvider({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const [family, setFamily] = useState<FamilyDto | null>(null);
  const [childrenList, setChildrenList] = useState<ChildDto[]>([]);
  const [activeChild, setActiveChildState] = useState<ChildDto | null>(null);
  const [activeDevice, setActiveDevice] = useState<DeviceDto | null>(null);
  const [activePolicy, setActivePolicy] = useState<CloudPolicyDto | null>(null);
  const [todaySummary, setTodaySummary] = useState<DailySummaryDto | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  const getDefaultPolicy = (childId: string): CloudPolicyDto => ({
    policyId: childId,
    version: 1,
    updatedBy: user?.email || "Parent",
    pauseDurationSeconds: 10,
    breathingDurationSeconds: 30,
    squatsTargetCount: 10,
    rules: [
      {
        packageName: "com.instagram.android",
        appDisplayName: "Instagram",
        mode: "EARN",
        isEnabled: true,
        dailyLimitMinutes: 30,
        unlockDurationSeconds: 600,
        interventionType: "PAUSE",
        pauseDurationSeconds: 10,
        breathingDurationSeconds: 30,
        squatsTargetCount: 10,
      },
      {
        packageName: "com.google.android.youtube",
        appDisplayName: "YouTube",
        mode: "EARN",
        isEnabled: true,
        dailyLimitMinutes: 45,
        unlockDurationSeconds: 900,
        interventionType: "BREATHING",
        pauseDurationSeconds: 10,
        breathingDurationSeconds: 30,
        squatsTargetCount: 10,
      },
      {
        packageName: "com.dts.freefireth",
        appDisplayName: "Gaming (Free Fire)",
        mode: "BLOCK",
        isEnabled: true,
        dailyLimitMinutes: 0,
        unlockDurationSeconds: 900,
        interventionType: "SQUATS",
        pauseDurationSeconds: 10,
        breathingDurationSeconds: 30,
        squatsTargetCount: 10,
      },
    ],
    schedules: [
      {
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
        name: "Bedtime / Sleep",
        packageName: "ALL_RESTRICTED",
        dayOfWeek: 1, // All week
        startHour: 21,
        startMinute: 30,
        endHour: 6,
        endMinute: 30,
        isBlocked: true,
      }
    ],
  });

  const loadFamilyData = useCallback(async () => {
    if (!user) {
      setFamily(null);
      setChildrenList([]);
      setActiveChildState(null);
      setActiveDevice(null);
      setActivePolicy(null);
      setTodaySummary(null);
      setLoading(false);
      return;
    }

    setLoading(true);

    try {
      // 1. Query Firestore for Family belonging to ownerParentId
      let loadedFamily: FamilyDto | null = null;
      try {
        const q = query(collection(db, "families"), where("ownerParentId", "==", user.uid));
        const snapshot = await getDocs(q);
        if (!snapshot.empty) {
          const docData = snapshot.docs[0].data() as FamilyDto;
          loadedFamily = { ...docData, familyId: snapshot.docs[0].id };
        }
      } catch (e) {
        // Fallback to local storage
        console.warn("Firestore family query fallback:", e);
      }

      // If not in Firestore, check local resilient storage
      if (!loadedFamily && typeof window !== "undefined") {
        const stored = localStorage.getItem(`${LOCAL_FAMILY_KEY}_${user.uid}`);
        if (stored) {
          loadedFamily = JSON.parse(stored);
        } else if (user.email === "parent@example.com") {
          // Pre-seed demo Smith family for quick demo user
          loadedFamily = {
            familyId: "fam_dev_sample",
            familyName: "Smith Family",
            ownerParentId: user.uid,
            subscriptionTier: "FREE",
          };
        }
      }

      setFamily(loadedFamily);

      if (loadedFamily) {
        // 2. Load Children for Family
        let loadedChildren: ChildDto[] = [];
        try {
          const childrenRef = collection(db, "families", loadedFamily.familyId, "children");
          const childSnap = await getDocs(childrenRef);
          loadedChildren = childSnap.docs.map((d) => ({
            ...(d.data() as ChildDto),
            childId: d.id,
          }));
        } catch (e) {
          console.warn("Firestore children query fallback:", e);
        }

        if (loadedChildren.length === 0 && typeof window !== "undefined") {
          const storedKids = localStorage.getItem(`${LOCAL_CHILDREN_KEY}_${loadedFamily.familyId}`);
          if (storedKids) {
            loadedChildren = JSON.parse(storedKids);
          } else if (loadedFamily.familyId === "fam_dev_sample") {
            loadedChildren = [{ childId: "child_alex_sample", name: "Alex", age: 10 }];
          }
        }

        setChildrenList(loadedChildren);

        // Select first child if none active
        const selectedChild = loadedChildren.length > 0 ? loadedChildren[0] : null;
        setActiveChildState(selectedChild);

        if (selectedChild) {
          await loadChildDetails(loadedFamily.familyId, selectedChild.childId);
        }
      }
    } catch (err) {
      console.error("Error loading family data:", err);
    } finally {
      setLoading(false);
    }
  }, [user]);

  const loadChildDetails = async (familyId: string, childId: string) => {
    // 1. Load Policy
    let pol: CloudPolicyDto | null = null;
    try {
      const polDoc = await getDoc(doc(db, "families", familyId, "children", childId, "policy", "current"));
      if (polDoc.exists()) {
        pol = polDoc.data() as CloudPolicyDto;
      }
    } catch (e) {
      // Ignore
    }

    if (!pol && typeof window !== "undefined") {
      const storedPol = localStorage.getItem(`${LOCAL_POLICIES_KEY}_${childId}`);
      if (storedPol) {
        pol = JSON.parse(storedPol);
      } else {
        pol = getDefaultPolicy(childId);
        localStorage.setItem(`${LOCAL_POLICIES_KEY}_${childId}`, JSON.stringify(pol));
      }
    }
    setActivePolicy(pol);

    // 2. Load Device with Realtime updates
    let dev: DeviceDto | null = null;
    try {
      const devDocs = await getDocs(collection(db, "families", familyId, "children", childId, "devices"));
      if (!devDocs.empty) {
        dev = devDocs.docs[0].data() as DeviceDto;
      }
    } catch (e) {
      // Ignore
    }

    if (!dev && typeof window !== "undefined") {
      const storedDev = localStorage.getItem(`${LOCAL_DEVICES_KEY}_${childId}`);
      if (storedDev) {
        dev = JSON.parse(storedDev);
      } else if (familyId === "fam_dev_sample" && childId === "child_alex_sample") {
        // Only demo sample user has a pre-populated test device
        dev = {
          deviceId: `dev_sample_alex`,
          childId: childId,
          deviceModel: "Android Phone",
          androidVersion: "Android 14 (API 34)",
          appVersion: "1.0.0-prod",
          isProtectionActive: true,
          activePolicyVersion: pol?.version || 1,
          lastSeen: new Date().toISOString(),
        };
      } else {
        dev = null; // Newly created child has NO paired device initially!
      }
    }
    setActiveDevice(dev);

    // 3. Load Today's Summary (Real or clean zero default)
    const todayStr = new Date().toISOString().split("T")[0];
    let summary: DailySummaryDto | null = null;
    try {
      const sumDoc = await getDoc(doc(db, "families", familyId, "daily_summaries", `sum_${childId}_${todayStr}`));
      if (sumDoc.exists()) {
        summary = sumDoc.data() as DailySummaryDto;
      }
    } catch (e) {
      // Ignore
    }

    if (!summary) {
      // Clean zero stats for new children
      if (familyId === "fam_dev_sample" && childId === "child_alex_sample") {
        summary = {
          summaryId: `sum_${childId}_${todayStr}`,
          familyId,
          childId,
          dateString: todayStr,
          totalScreenTimeMinutes: 42,
          totalBlocks: 14,
          totalInterventions: 6,
          appBreakdown: {
            "com.instagram.android": { appDisplayName: "Instagram", minutes: 24, blocks: 9, interventions: 4 },
            "com.google.android.youtube": { appDisplayName: "YouTube", minutes: 18, blocks: 5, interventions: 2 },
          },
        };
      } else {
        summary = {
          summaryId: `sum_${childId}_${todayStr}`,
          familyId,
          childId,
          dateString: todayStr,
          totalScreenTimeMinutes: 0,
          totalBlocks: 0,
          totalInterventions: 0,
          appBreakdown: {},
        };
      }
    }
    setTodaySummary(summary);
  };

  useEffect(() => {
    loadFamilyData();
  }, [loadFamilyData]);

  const createFamily = async (familyName: string): Promise<FamilyDto> => {
    const parentUid = user ? user.uid : "dev_parent_default";
    const famId = "fam_" + Math.random().toString(36).substring(2, 10);
    const newFam: FamilyDto = {
      familyId: famId,
      familyName: familyName.trim() || "My Family",
      ownerParentId: parentUid,
      subscriptionTier: "FREE",
    };

    // Save locally
    if (typeof window !== "undefined") {
      localStorage.setItem(`${LOCAL_FAMILY_KEY}_${parentUid}`, JSON.stringify(newFam));
    }

    // Save to Firestore
    try {
      await setDoc(doc(db, "families", famId), newFam);
      await setDoc(doc(db, "families", famId, "parents", parentUid), {
        parentId: parentUid,
        email: user?.email || "parent@example.com",
        displayName: user?.displayName || "Parent",
        role: "OWNER",
      });
    } catch (e) {
      console.warn("Firestore createFamily fallback:", e);
    }

    setFamily(newFam);
    return newFam;
  };

  const createChild = async (name: string, age: number): Promise<ChildDto> => {
    if (!family) throw new Error("No active family");
    const childId = "child_" + Math.random().toString(36).substring(2, 10);
    const newKid: ChildDto = {
      childId,
      name: name.trim() || "Child",
      age: age || 10,
    };

    const updatedKids = [...childrenList, newKid];
    setChildrenList(updatedKids);
    if (typeof window !== "undefined") {
      localStorage.setItem(`${LOCAL_CHILDREN_KEY}_${family.familyId}`, JSON.stringify(updatedKids));
    }

    const defaultPol = getDefaultPolicy(childId);
    if (typeof window !== "undefined") {
      localStorage.setItem(`${LOCAL_POLICIES_KEY}_${childId}`, JSON.stringify(defaultPol));
    }

    try {
      await setDoc(doc(db, "families", family.familyId, "children", childId), newKid);
      await setDoc(doc(db, "families", family.familyId, "children", childId, "policy", "current"), defaultPol);
    } catch (e) {
      console.warn("Firestore createChild fallback:", e);
    }

    setActiveChildState(newKid);
    setActivePolicy(defaultPol);
    return newKid;
  };

  const deleteChild = async (childId: string): Promise<void> => {
    if (!family) return;
    const updatedKids = childrenList.filter((k) => k.childId !== childId);
    setChildrenList(updatedKids);

    if (typeof window !== "undefined") {
      localStorage.setItem(`${LOCAL_CHILDREN_KEY}_${family.familyId}`, JSON.stringify(updatedKids));
      localStorage.removeItem(`${LOCAL_POLICIES_KEY}_${childId}`);
    }

    if (activeChild?.childId === childId) {
      const nextActive = updatedKids.length > 0 ? updatedKids[0] : null;
      setActiveChildState(nextActive);
      if (nextActive) {
        loadChildDetails(family.familyId, nextActive.childId);
      } else {
        setActivePolicy(null);
        setActiveDevice(null);
      }
    }

    try {
      await deleteDoc(doc(db, "families", family.familyId, "children", childId));
    } catch (e) {
      // Ignore
    }
  };

  const setActiveChild = (child: ChildDto) => {
    setActiveChildState(child);
    if (family) {
      loadChildDetails(family.familyId, child.childId);
    }
  };

  const savePolicy = async (policy: CloudPolicyDto): Promise<void> => {
    if (!family || !activeChild) throw new Error("No active family or child");
    const updatedVersion = policy.version + 1;
    const finalPolicy: CloudPolicyDto = {
      ...policy,
      version: updatedVersion,
      updatedBy: user?.email || "Parent",
      updatedAt: new Date().toISOString(),
    };

    setActivePolicy(finalPolicy);

    if (typeof window !== "undefined") {
      localStorage.setItem(`${LOCAL_POLICIES_KEY}_${activeChild.childId}`, JSON.stringify(finalPolicy));
    }

    try {
      await setDoc(
        doc(db, "families", family.familyId, "children", activeChild.childId, "policy", "current"),
        finalPolicy
      );
    } catch (e) {
      console.warn("Firestore savePolicy fallback:", e);
    }
  };

  const generatePairingCode = async (childId: string, childName: string): Promise<string> => {
    if (!family || !user) throw new Error("Must be signed in");
    const code = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = Date.now() + 15 * 60 * 1000; // 15 mins

    const pairingData: PairingCodeDto = {
      code,
      familyId: family.familyId,
      childId,
      childName,
      createdByParentId: user.uid,
      expiresAtTimestampMs: expiresAt,
      isUsed: false,
    };

    try {
      await setDoc(doc(db, "pairing_codes", code), pairingData);
    } catch (e) {
      console.warn("Firestore pairing code fallback:", e);
    }

    return code;
  };

  return (
    <FamilyContext.Provider
      value={{
        family,
        childrenList,
        activeChild,
        activeDevice,
        activePolicy,
        todaySummary,
        loading,
        createFamily,
        createChild,
        deleteChild,
        setActiveChild,
        savePolicy,
        generatePairingCode,
        refreshData: loadFamilyData,
      }}
    >
      {children}
    </FamilyContext.Provider>
  );
}

export function useFamily() {
  const context = useContext(FamilyContext);
  if (!context) {
    throw new Error("useFamily must be used within a FamilyProvider");
  }
  return context;
}
