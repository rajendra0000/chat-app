"use client";

// ==============================
// Auth Hook — Login Flow State Machine
// ==============================

import { useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/auth-store";
import * as authService from "@/services/auth-service";
import { ApiError } from "@/services/api";
import type { RegisterRequest } from "@/types";

export type AuthStep = "phone" | "otp" | "register" | "welcome";

interface UseAuthReturn {
  step: AuthStep;
  phone: string;
  email: string;
  isLoading: boolean;
  error: string | null;
  isNewUser: boolean;

  /** Step 1: Submit phone number + email */
  submitPhone: (phone: string, email: string) => Promise<void>;
  /** Step 2: Verify OTP */
  submitOtp: (otp: string) => Promise<void>;
  /** Step 3: Register new user */
  submitRegistration: (data: Omit<RegisterRequest, "phone">) => Promise<void>;
  /** Resend OTP */
  resendOtp: () => Promise<void>;
  /** Go back to previous step */
  goBack: () => void;
  /** Clear error */
  clearError: () => void;
}

export function useAuth(): UseAuthReturn {
  const router = useRouter();
  const setToken = useAuthStore((s) => s.setToken);

  const [step, setStep] = useState<AuthStep>("phone");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isNewUser, setIsNewUser] = useState(false);

  const clearError = useCallback(() => setError(null), []);

  /**
   * Step 1: user enters phone (E.164, e.g. +919876543210) + email.
   * - If user exists → send OTP to email → go to otp step
   * - If new user → go to register step (OTP sent after registration)
   */
  const submitPhone = useCallback(async (phoneValue: string, emailValue: string) => {
    setIsLoading(true);
    setError(null);
    setPhone(phoneValue);
    setEmail(emailValue);

    try {
      const { userExists } = await authService.checkUser(phoneValue);

      if (userExists) {
        setIsNewUser(false);
        await authService.sendOtp(phoneValue, emailValue);
        setStep("otp");
      } else {
        setIsNewUser(true);
        setStep("register");
      }
    } catch (err) {
      const message =
        err instanceof ApiError
          ? err.message
          : "Failed to process. Please try again.";
      setError(message);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const submitOtp = useCallback(
    async (otp: string) => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await authService.verifyOtp(phone, otp);

        if (response.success && response.token) {
          setToken(response.token);
          setStep("welcome");
          setTimeout(() => {
            router.push("/chat");
          }, 1500);
        } else {
          setError("Invalid OTP. Please try again.");
        }
      } catch (err) {
        const message =
          err instanceof ApiError
            ? err.message
            : "OTP verification failed. Please try again.";
        setError(message);
      } finally {
        setIsLoading(false);
      }
    },
    [phone, setToken, router]
  );

  /**
   * Step 3: register new user, then immediately send OTP so user can verify.
   * Email comes from the registration form (same email used in the form).
   */
  const submitRegistration = useCallback(
    async (data: Omit<RegisterRequest, "phone">) => {
      setIsLoading(true);
      setError(null);

      try {
        const regResponse = await authService.registerUser({ ...data, phone });

        if (regResponse.success) {
          // Use the email from the registration form for OTP delivery
          const deliveryEmail = data.email || email;
          setEmail(deliveryEmail);
          await authService.sendOtp(phone, deliveryEmail);
          setIsNewUser(false);
          setStep("otp");
        } else {
          setError("Registration failed. Please try again.");
        }
      } catch (err) {
        const message =
          err instanceof ApiError
            ? err.message
            : "Registration failed. Please try again.";
        setError(message);
      } finally {
        setIsLoading(false);
      }
    },
    [phone, email]
  );

  const resendOtp = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    try {
      await authService.sendOtp(phone, email);
    } catch (err) {
      const message =
        err instanceof ApiError
          ? err.message
          : "Failed to resend OTP. Please try again.";
      setError(message);
    } finally {
      setIsLoading(false);
    }
  }, [phone, email]);

  const goBack = useCallback(() => {
    setError(null);
    if (step === "otp" || step === "register") {
      setStep("phone");
    }
  }, [step]);

  return {
    step,
    phone,
    email,
    isLoading,
    error,
    isNewUser,
    submitPhone,
    submitOtp,
    submitRegistration,
    resendOtp,
    goBack,
    clearError,
  };
}
