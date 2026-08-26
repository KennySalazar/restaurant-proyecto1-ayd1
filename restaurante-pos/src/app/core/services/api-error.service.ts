import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { MessageService } from 'primeng/api';
import { ProblemDetail } from '../models/api.models';

const FALLBACK_MESSAGES: Record<string, string> = {
  invalid_credentials: 'Las credenciales ingresadas no son válidas.',
  invalid_token: 'La sesión ya no es válida. Inicia sesión nuevamente.',
  authentication_required: 'Debes iniciar sesión para continuar.',
  account_disabled: 'La cuenta está deshabilitada.',
  forbidden: 'No tienes permisos para acceder a este recurso.',
  validation_error: 'Revisa los datos ingresados.',
  invalid_request: 'La solicitud contiene datos inválidos.',
  invalid_password: 'La contraseña no cumple la política requerida.',
  invalid_password_transition: 'La nueva contraseña debe ser diferente de la actual.',
  not_found: 'El recurso solicitado no fue encontrado.',
  internal_error: 'Ocurrió un error interno. Intenta nuevamente.',
};

@Injectable({ providedIn: 'root' })
export class ApiErrorService {
  constructor(private readonly messages: MessageService) {}

  getProblem(error: unknown): ProblemDetail {
    if (error instanceof HttpErrorResponse) {
      if (this.isProblemDetail(error.error)) {
        return error.error;
      }

      if (error.status === 0) {
        return {
          status: 0,
          title: 'Sin conexión',
          detail: 'No se pudo conectar con el servidor. Intenta nuevamente.',
          code: 'network_error',
        };
      }

      return {
        status: error.status,
        title: 'Error de solicitud',
        detail: 'No se pudo completar la solicitud.',
        code: 'request_error',
      };
    }

    return {
      status: 500,
      title: 'Error inesperado',
      detail: 'Ocurrió un error inesperado. Intenta nuevamente.',
      code: 'internal_error',
    };
  }

  getMessage(error: unknown): string {
    const problem = this.getProblem(error);

    return (
      problem.detail ||
      FALLBACK_MESSAGES[problem.code ?? ''] ||
      'No se pudo completar la solicitud.'
    );
  }

  isAuthenticationFailure(error: unknown): boolean {
    const problem = this.getProblem(error);

    return (
      problem.code === 'invalid_token' ||
      problem.code === 'authentication_required'
    );
  }

  present(error: unknown): void {
    const problem = this.getProblem(error);
    const detail = this.getMessage(error);

    this.messages.add({
      severity: this.severityFor(problem.status),
      summary: problem.title || 'Error',
      detail,
      life: 6500,
    });
  }

  private severityFor(status?: number): 'error' | 'warn' {
    return status === 400 || status === 404 || status === 409
      ? 'warn'
      : 'error';
  }

  private isProblemDetail(value: unknown): value is ProblemDetail {
    return (
      typeof value === 'object' &&
      value !== null &&
      ('detail' in value || 'code' in value)
    );
  }
}
