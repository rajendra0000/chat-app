// ==============================
// Auth Service
// ==============================

import { apiPostPublic } from "./api";
import type {
  CheckUserRequest,
  CheckUserResponse,
  OtpRequest,
  OtpVerifyRequest,
  OtpVerifyResponse,
  RegisterRequest,
  RegisterResponse,
} from "@/types";

/** Check if a phone number is already registered.
 *  phone must be E.164 format, e.g. +919876543210
 */
export async function checkUser(phone: string): Promise<CheckUserResponse> {
  return apiPostPublic<CheckUserResponse>("/auth/check-user", { phone } as CheckUserRequest);
}

/**
 * Send OTP to the given email address.
 * @param phone  E.164 phone number — the user's identity (e.g. +919876543210)
 * @param email  Email address where the OTP will be delivered (e.g. user@gmail.com)
 */
export async function sendOtp(phone: string, email: string): Promise<string> {
  return apiPostPublic<string>("/auth/send-otp", { phone, email } as OtpRequest, "text");
}

/** Verify OTP and get JWT token
 *  phone must be E.164 format, e.g. +919876543210
 */
export async function verifyOtp(phone: string, otp: string): Promise<OtpVerifyResponse> {
  return apiPostPublic<OtpVerifyResponse>("/auth/verify-otp", {
    phone,
    otp,
  } as OtpVerifyRequest);
}

/** Register a new user */
export async function registerUser(data: RegisterRequest): Promise<RegisterResponse> {
  return apiPostPublic<RegisterResponse>("/auth/register", data);
}
