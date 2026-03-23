"use client";

// ==============================
// Login Page — Multi-Step Auth Wizard
// ==============================

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/use-auth";
import { useAuthStore } from "@/store/auth-store";
import { PhoneInput } from "@/components/auth/phone-input";
import { OtpInput } from "@/components/auth/otp-input";
import { RegistrationForm } from "@/components/auth/registration-form";
import { CheckCircle } from "lucide-react";

/** Step indicator dots */
function StepIndicator({ current, total }: { current: number; total: number }) {
  return (
    <div className="flex items-center justify-center gap-2 mb-6">
      {Array.from({ length: total }, (_, i) => (
        <div
          key={i}
          className={`h-1.5 rounded-full transition-all duration-300 ${
            i < current
              ? "w-8 bg-gradient-to-r from-[#667eea] to-[#764ba2]"
              : i === current
              ? "w-8 bg-[#667eea]"
              : "w-4 bg-muted-foreground/30"
          }`}
        />
      ))}
    </div>
  );
}

/** Welcome screen — shown after successful OTP verification */
function WelcomeStep() {
  return (
    <div className="text-center py-4 animate-in fade-in zoom-in-95 duration-500">
      <div className="w-20 h-20 rounded-full bg-success/10 flex items-center justify-center mx-auto mb-5">
        <CheckCircle className="w-12 h-12 text-success" />
      </div>
      <h2 className="text-2xl font-bold text-foreground mb-2">
        Welcome to SkibidiChat!
      </h2>
      <p className="text-muted-foreground">
        Redirecting you to your chats...
      </p>
      <div className="mt-6 flex justify-center">
        <div className="w-8 h-8 border-3 border-[#667eea] border-t-transparent rounded-full animate-spin" />
      </div>
    </div>
  );
}

export default function LoginPage() {
  const router = useRouter();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  // Redirect to chat if already authenticated
  useEffect(() => {
    if (isAuthenticated()) {
      router.replace("/chat");
    }
  }, [isAuthenticated, router]);

  const {
    step,
    phone,
    email,
    isLoading,
    error,
    submitPhone,
    submitOtp,
    submitRegistration,
    resendOtp,
    goBack,
  } = useAuth();

  const stepIndex = { phone: 0, register: 1, otp: 2, welcome: 3 }[step] ?? 0;
  const stepCount = 3;

  return (
    <div className="bg-white/95 dark:bg-card/95 backdrop-blur-xl rounded-3xl p-8 sm:p-10 shadow-2xl max-w-md w-full transition-all duration-300">
      {/* Logo */}
      <div className="text-center mb-2">
        <h1 className="text-lg font-bold bg-gradient-to-r from-[#667eea] to-[#764ba2] bg-clip-text text-transparent tracking-tight">
          SkibidiChat
        </h1>
      </div>

      {/* Step indicator — hide on welcome */}
      {step !== "welcome" && (
        <StepIndicator current={stepIndex} total={stepCount} />
      )}

      {/* Step content with transition */}
      <div className="animate-in fade-in slide-in-from-bottom-2 duration-300" key={step}>
        {step === "phone" && (
          <PhoneInput
            onSubmit={submitPhone}
            isLoading={isLoading}
            error={error}
          />
        )}

        {step === "otp" && (
          <OtpInput
            phone={phone}
            email={email}
            onSubmit={submitOtp}
            onResend={resendOtp}
            onBack={goBack}
            isLoading={isLoading}
            error={error}
          />
        )}

        {step === "register" && (
          <RegistrationForm
            phone={phone}
            onSubmit={submitRegistration}
            onBack={goBack}
            isLoading={isLoading}
            error={error}
          />
        )}

        {step === "welcome" && <WelcomeStep />}
      </div>
    </div>
  );
}
