export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
}

export interface MessageResponse {
  message: string;
}

export interface PasswordChangeResponse extends MessageResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}
