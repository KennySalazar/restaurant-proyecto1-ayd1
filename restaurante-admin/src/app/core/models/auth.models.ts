export const ROLES = ['ADMIN', 'WAITER', 'KITCHEN', 'CASHIER'] as const;
export type Role = (typeof ROLES)[number];

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string | null;
  tokenType: string | null;
  expiresIn: number;
  requiresTwoFactor: boolean;
  challengeId: string | null;
  challengeExpiresAt: string | null;
  message: string;
}

export interface VerifyChallengeRequest {
  challengeId: string;
  otp: string;
}

export interface AccessTokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface PasswordChangeRequest {
  currentPassword: string;
  newPassword: string;
}

export interface UserResponse {
  id: number;
  email: string;
  role: Role;
  enabled: boolean;
  twoFactorEnabled: boolean;
}

export interface StoredSession {
  accessToken: string;
  tokenType: string;
  expiresAt: number;
  user: UserResponse | null;
}

export interface PendingLoginChallenge {
  challengeId: string;
  expiresAt: string;
  returnUrl: string;
}

export interface ChallengeResponse {
  challengeId: string;
  expiresAt: string;
  message: string;
}

export interface RecoveryRequest {
  email: string;
}

export interface RecoveryVerificationRequest {
  challengeId: string;
  otp: string;
  newPassword: string;
}

export interface PendingRecoveryChallenge {
  challengeId: string;
  expiresAt: string;
  email: string;
}

export interface TwoFactorRequest {
  currentPassword: string;
}
