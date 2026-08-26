import { Injectable } from '@angular/core';
import {
  PendingLoginChallenge,
  PendingRecoveryChallenge,
} from '../models/auth.models';

const LOGIN_CHALLENGE_KEY =
  'restaurante.admin.auth.login-challenge';

const RECOVERY_CHALLENGE_KEY =
  'restaurante.admin.auth.recovery-challenge';

@Injectable({ providedIn: 'root' })
export class AuthFlowService {

  setLoginChallenge(challenge: PendingLoginChallenge): void {
    sessionStorage.setItem(
      LOGIN_CHALLENGE_KEY,
      JSON.stringify(challenge),
    );
  }

  getLoginChallenge(): PendingLoginChallenge | null {
    try {
      const raw = sessionStorage.getItem(LOGIN_CHALLENGE_KEY);

      if (!raw) {
        return null;
      }

      const challenge =
        JSON.parse(raw) as Partial<PendingLoginChallenge>;

      if (
        typeof challenge.challengeId !== 'string' ||
        typeof challenge.expiresAt !== 'string' ||
        typeof challenge.returnUrl !== 'string'
      ) {
        this.clearLoginChallenge();
        return null;
      }

      return {
        challengeId: challenge.challengeId,
        expiresAt: challenge.expiresAt,
        returnUrl: challenge.returnUrl,
      };
    } catch {
      this.clearLoginChallenge();
      return null;
    }
  }

  clearLoginChallenge(): void {
    sessionStorage.removeItem(LOGIN_CHALLENGE_KEY);
  }

  setRecoveryChallenge(
    challenge: PendingRecoveryChallenge,
  ): void {
    sessionStorage.setItem(
      RECOVERY_CHALLENGE_KEY,
      JSON.stringify(challenge),
    );
  }

  getRecoveryChallenge(): PendingRecoveryChallenge | null {
    try {
      const raw = sessionStorage.getItem(
        RECOVERY_CHALLENGE_KEY,
      );

      if (!raw) {
        return null;
      }

      const challenge =
        JSON.parse(raw) as Partial<PendingRecoveryChallenge>;

      if (
        typeof challenge.challengeId !== 'string' ||
        typeof challenge.expiresAt !== 'string' ||
        typeof challenge.email !== 'string'
      ) {
        this.clearRecoveryChallenge();
        return null;
      }

      return {
        challengeId: challenge.challengeId,
        expiresAt: challenge.expiresAt,
        email: challenge.email,
      };
    } catch {
      this.clearRecoveryChallenge();
      return null;
    }
  }

  clearRecoveryChallenge(): void {
    sessionStorage.removeItem(RECOVERY_CHALLENGE_KEY);
  }
}
