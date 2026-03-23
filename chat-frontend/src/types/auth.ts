// ==============================
// Auth Types
// ==============================

export interface CheckUserRequest {
  phone: string;
}

export interface CheckUserResponse {
  userExists: boolean;
}

export interface OtpRequest {
  /** E.164 phone number, e.g. +919876543210 — used as identity */
  phone: string;
  /** Email address where the OTP will be delivered */
  email: string;
}

export interface OtpVerifyRequest {
  /** E.164 phone number, e.g. +919876543210 */
  phone: string;
  otp: string;
}

export interface OtpVerifyResponse {
  success: boolean;
  token?: string;
  userData?: UserData;
}

export interface RegisterRequest {
  phone: string;
  fullName: string;
  email: string;
  dateOfBirth: string;
  gender: string;
  address: string;
}

export interface RegisterResponse {
  success: boolean;
  userData?: UserData;
  message?: string;
}

export interface UserData {
  phone: string;
  fullName: string;
  email: string;
}
