import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
ChallengeResponse,
LoginRequest,
LoginResponse,
PasswordChangeRequest,
RecoveryRequest,
RecoveryVerificationRequest,
UserResponse,
VerifyChallengeRequest,
TwoFactorRequest,
} from '../models/auth.models';
import {
  AdminPingResponse,
  PasswordChangeResponse,
  MessageResponse,
} from '../models/api.models';
import { ApiErrorService } from './api-error.service';
import { AuthSessionService } from './auth-session.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authUrl = `${environment.apiBaseUrl}/auth`;

  constructor(
    private readonly http: HttpClient,
    private readonly session: AuthSessionService,
    private readonly errors: ApiErrorService,
  ) {}

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(
      `${this.authUrl}/login`,
      request,
    );
  }

  verifyLogin(
    request: VerifyChallengeRequest,
  ): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(
      `${this.authUrl}/login/verify`,
      request,
    );
  }

  requestPasswordRecovery(
  request: RecoveryRequest,
): Observable<ChallengeResponse> {
  return this.http.post<ChallengeResponse>(
    `${this.authUrl}/password-recovery`,
    request,
  );
}

verifyPasswordRecovery(
  request: RecoveryVerificationRequest,
): Observable<{ message: string }> {
  return this.http.post<{ message: string }>(
    `${this.authUrl}/password-recovery/verify`,
    request,
  );
}

  changePassword(
    request: PasswordChangeRequest,
  ): Observable<PasswordChangeResponse> {
    return this.http.post<PasswordChangeResponse>(
      `${this.authUrl}/password/change`,
      request,
    );
  }

  currentUser(): Observable<UserResponse> {
    return this.http.get<UserResponse>(
      `${this.authUrl}/me`,
    );
  }

  loadCurrentUser(): Observable<UserResponse> {
    return this.currentUser().pipe(
      tap((user) => this.session.setUser(user)),
      catchError((error: unknown) => {
        if (this.errors.isAuthenticationFailure(error)) {
          this.session.clear();
        }

        return throwError(() => error);
      }),
    );
  }

      requestTwoFactorEnable(
      request: TwoFactorRequest,
    ): Observable<ChallengeResponse> {
      return this.http.post<ChallengeResponse>(
        `${this.authUrl}/2fa/enable`,
        request,
      );
    }

    confirmTwoFactorEnable(
      request: VerifyChallengeRequest,
    ): Observable<MessageResponse> {
      return this.http.post<MessageResponse>(
        `${this.authUrl}/2fa/enable/verify`,
        request,
      );
    }

    requestTwoFactorDisable(
      request: TwoFactorRequest,
    ): Observable<ChallengeResponse> {
      return this.http.post<ChallengeResponse>(
        `${this.authUrl}/2fa/disable`,
        request,
      );
    }

    confirmTwoFactorDisable(
      request: VerifyChallengeRequest,
    ): Observable<MessageResponse> {
      return this.http.post<MessageResponse>(
        `${this.authUrl}/2fa/disable/verify`,
        request,
      );
    }

  adminPing(): Observable<AdminPingResponse> {
    return this.http.get<AdminPingResponse>(
      `${environment.apiBaseUrl}/admin/ping`,
    );
  }

  establishLogin(
    response: LoginResponse,
  ): Observable<UserResponse> {
    if (
      !response.accessToken ||
      !response.tokenType ||
      response.requiresTwoFactor
    ) {
      return throwError(
        () => new Error('La autenticación todavía requiere verificación 2FA'),
      );
    }

    this.session.establish({
      accessToken: response.accessToken,
      tokenType: response.tokenType,
      expiresIn: response.expiresIn,
    });

    return this.loadCurrentUser();
  }

  logout(): void {
    this.session.clear();
  }
}
