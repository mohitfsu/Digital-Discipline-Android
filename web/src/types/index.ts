export type RuleMode = "ALLOW" | "BLOCK" | "DELAY" | "EARN";

export type UserMode = "PARENT" | "SELF";

export type GoalCategory =
  | "FITNESS"
  | "STUDY"
  | "PRODUCTIVITY"
  | "SLEEP"
  | "MINDFULNESS"
  | "READING"
  | "HEALTH"
  | "FINANCE"
  | "CUSTOM";

export type TriggerCategory =
  | "SOCIAL_MEDIA"
  | "VIDEO_STREAMING"
  | "GAMING"
  | "SHOPPING"
  | "FOOD_DELIVERY"
  | "CUSTOM";

export type BehaviourCategory =
  | "PHYSICAL"
  | "MINDFUL"
  | "STUDY"
  | "HEALTH"
  | "PRODUCTIVITY"
  | "CUSTOM";

export type RewardType =
  | "EARNED_SCREEN_TIME"
  | "NO_REWARD"
  | "COMPLETION_ONLY";

export interface FamilyDto {
  familyId: string;
  familyName: string;
  ownerParentId: string;
  subscriptionTier: string;
}

export interface ParentDto {
  parentId: string;
  email: string;
  displayName: string;
  role: "OWNER" | "MEMBER";
}

export interface ChildDto {
  childId: string;
  name: string;
  age: number;
}

export interface DeviceDto {
  deviceId: string;
  childId: string;
  deviceModel: string;
  androidVersion: string;
  appVersion: string;
  isProtectionActive: boolean;
  activePolicyVersion: number;
  lastSeen?: any;
  pairedAt?: any;
}

export interface CloudAppRuleDto {
  packageName: string;
  appDisplayName: string;
  mode: RuleMode;
  isEnabled: boolean;
  dailyLimitMinutes: number;
  unlockDurationSeconds: number;
  interventionType: string;
  pauseDurationSeconds?: number;
  breathingDurationSeconds?: number;
  squatsTargetCount?: number;
}

export interface CloudScheduleDto {
  scheduleId?: string;
  name?: string;
  packageName: string;
  dayOfWeek: number; // 1=Sun..7=Sat
  startHour: number;
  startMinute: number;
  endHour: number;
  endMinute: number;
  isBlocked: boolean;
}

export interface CloudPolicyDto {
  policyId: string;
  version: number;
  updatedBy: string;
  updatedAt?: any;
  pauseDurationSeconds: number;
  breathingDurationSeconds: number;
  squatsTargetCount: number;
  rules: CloudAppRuleDto[];
  schedules: CloudScheduleDto[];
}

export interface DailySummaryDto {
  summaryId: string;
  familyId: string;
  childId: string;
  dateString: string;
  totalScreenTimeMinutes: number;
  totalBlocks: number;
  totalInterventions: number;
  totalAttempts?: number;
  totalEarnedMinutes?: number;
  habitInterruptionRate?: number;
  pauseCount?: number;
  breathingCount?: number;
  squatsCount?: number;
  appBreakdown?: Record<string, {
    appDisplayName: string;
    minutes: number;
    blocks: number;
    interventions: number;
    attempts?: number;
    earnedMinutes?: number;
    habitInterruptionRate?: number;
  }>;
}

export interface PairingCodeDto {
  code: string;
  familyId: string;
  childId: string;
  childName: string;
  createdByParentId: string;
  expiresAtTimestampMs: number;
  isUsed: boolean;
  pairedDeviceId?: string;
  usedAt?: any;
}

// Phase 4A Behaviour & Goal Domain Models
export interface GoalDto {
  goalId: string;
  ownerId: string;
  mode: UserMode;
  title: string;
  description?: string;
  category: GoalCategory;
  active: boolean;
  createdAt: number;
  updatedAt: number;
  startDate: number;
  targetDate?: number;
  dailyTarget: number;
  weeklyTarget: number;
  progress: number;
  unit: string;
  priority: number;
}

export interface TriggerDto {
  triggerId: string;
  ownerId: string;
  goalId: string;
  packageName: string;
  appDisplayName: string;
  category: TriggerCategory;
  active: boolean;
  startHour: number;
  startMinute: number;
  endHour: number;
  endMinute: number;
  daysOfWeek: string;
  priority: number;
}

export interface ReplacementBehaviourDto {
  behaviourId: string;
  category: BehaviourCategory;
  type: string;
  title: string;
  description?: string;
  targetCount: number;
  durationSeconds: number;
  unit: string;
  configJson?: string;
}

export interface BehaviourPolicyDto {
  policyId: string;
  ownerId: string;
  goalId: string;
  triggerId: string;
  replacementBehaviourId: string;
  interventionMode: RuleMode;
  rewardType: RewardType;
  earnedSeconds: number;
  maximumDailySeconds: number;
  maximumSessionSeconds: number;
  enabled: boolean;
  priority: number;
}

export interface GoalProgressDto {
  goalId: string;
  dateString: string;
  completedCount: number;
  targetCount: number;
  completedDurationSeconds: number;
  targetDurationSeconds: number;
  completionPercentage: number;
}
