import { computed, Injectable, signal } from '@angular/core';
import { AccessTokenResponse, Role, StoredSession, UserResponse } from '../models/auth.models';

const SESSION_KEY = 'restaurante.pos.auth.session';

@Injectable({ providedIn: 'root' })
export class AuthSessionService {
  private readonly sessionSignal = signal<StoredSession | null>(this.readSession());

  readonly user = computed(() => this.sessionSignal()?.user ?? null);

  get currentUser(): UserResponse | null {
    return this.sessionSignal()?.user ?? null;
  }

  getToken(): string | null {
    const session = this.sessionSignal();
    if (!session || session.expiresAt <= Date.now()) {
      if (session) {
        this.clear();
      }
      return null;
    }
    return session.accessToken;
  }

  isAuthenticated(): boolean {
    return this.getToken() !== null;
  }

  hasRole(roles?: readonly Role[]): boolean {
    if (!roles || roles.length === 0) {
      return this.isAuthenticated();
    }

    const role = this.currentUser?.role;
    return this.isAuthenticated() && role !== null && role !== undefined && roles.includes(role);
  }

  establish(response: AccessTokenResponse): void {
    const currentUser = this.currentUser;
    const session: StoredSession = {
      accessToken: response.accessToken,
      tokenType: response.tokenType || 'Bearer',
      expiresAt: Date.now() + response.expiresIn,
      user: currentUser,
    };
    this.persist(session);
  }

  setUser(user: UserResponse): void {
    const session = this.sessionSignal();
    if (!session) {
      return;
    }
    this.persist({ ...session, user });
  }

  clear(): void {
    this.sessionSignal.set(null);
    localStorage.removeItem(SESSION_KEY);
  }

  private persist(session: StoredSession): void {
    this.sessionSignal.set(session);
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  }

  private readSession(): StoredSession | null {
    try {
      const raw = localStorage.getItem(SESSION_KEY);
      if (!raw) {
        return null;
      }

      const session = JSON.parse(raw) as Partial<StoredSession>;
      if (
        typeof session.accessToken !== 'string' ||
        typeof session.tokenType !== 'string' ||
        typeof session.expiresAt !== 'number'
      ) {
        localStorage.removeItem(SESSION_KEY);
        return null;
      }

      return {
        accessToken: session.accessToken,
        tokenType: session.tokenType,
        expiresAt: session.expiresAt,
        user: session.user ?? null,
      };
    } catch {
      localStorage.removeItem(SESSION_KEY);
      return null;
    }
  }
}
