"use client";

// ==============================
// OTP Input Component
// ==============================

import {
  useState,
  useRef,
  useEffect,
  useCallback,
  type KeyboardEvent,
  type ClipboardEvent,
  type FormEvent,
} from "react";
import { Button } from "@/components/ui/button";
import { Loader2, ArrowLeft, ShieldCheck } from "lucide-react";
import { OTP_LENGTH, OTP_COUNTDOWN_SECONDS } from "@/lib/constants";

interface OtpInputProps {
  phone: string;
  email: string;
  onSubmit: (otp: string) => Promise<void>;
  onResend: () => Promise<void>;
  onBack: () => void;
  isLoading: boolean;
  error: string | null;
}

export function OtpInput({
  phone,
  email,
  onSubmit,
  onResend,
  onBack,
  isLoading,
  error,
}: OtpInputProps) {
  const [digits, setDigits] = useState<string[]>(Array(OTP_LENGTH).fill(""));
  const [countdown, setCountdown] = useState(OTP_COUNTDOWN_SECONDS);
  const [isResending, setIsResending] = useState(false);
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  // Countdown timer
  useEffect(() => {
    if (countdown <= 0) return;
    const timer = setInterval(() => {
      setCountdown((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [countdown]);

  // Auto-focus first input
  useEffect(() => {
    inputRefs.current[0]?.focus();
  }, []);

  const handleDigitChange = useCallback(
    (index: number, value: string) => {
      // Only allow single digit
      const digit = value.replace(/\D/g, "").slice(-1);
      const newDigits = [...digits];
      newDigits[index] = digit;
      setDigits(newDigits);

      // Auto-advance to next input
      if (digit && index < OTP_LENGTH - 1) {
        inputRefs.current[index + 1]?.focus();
      }

      // Auto-submit when all digits filled
      if (digit && index === OTP_LENGTH - 1) {
        const fullOtp = newDigits.join("");
        if (fullOtp.length === OTP_LENGTH) {
          onSubmit(fullOtp);
        }
      }
    },
    [digits, onSubmit]
  );

  const handleKeyDown = useCallback(
    (index: number, e: KeyboardEvent<HTMLInputElement>) => {
      if (e.key === "Backspace") {
        if (!digits[index] && index > 0) {
          // Move to previous input on backspace when current is empty
          inputRefs.current[index - 1]?.focus();
          const newDigits = [...digits];
          newDigits[index - 1] = "";
          setDigits(newDigits);
        }
      } else if (e.key === "ArrowLeft" && index > 0) {
        inputRefs.current[index - 1]?.focus();
      } else if (e.key === "ArrowRight" && index < OTP_LENGTH - 1) {
        inputRefs.current[index + 1]?.focus();
      }
    },
    [digits]
  );

  const handlePaste = useCallback(
    (e: ClipboardEvent<HTMLInputElement>) => {
      e.preventDefault();
      const pasted = e.clipboardData.getData("text").replace(/\D/g, "").slice(0, OTP_LENGTH);
      if (!pasted) return;

      const newDigits = [...digits];
      for (let i = 0; i < pasted.length; i++) {
        newDigits[i] = pasted[i];
      }
      setDigits(newDigits);

      // Focus appropriate input
      const focusIndex = Math.min(pasted.length, OTP_LENGTH - 1);
      inputRefs.current[focusIndex]?.focus();

      // Auto-submit if all digits pasted
      if (pasted.length === OTP_LENGTH) {
        onSubmit(pasted);
      }
    },
    [digits, onSubmit]
  );

  const handleResend = async () => {
    setIsResending(true);
    await onResend();
    setCountdown(OTP_COUNTDOWN_SECONDS);
    setDigits(Array(OTP_LENGTH).fill(""));
    inputRefs.current[0]?.focus();
    setIsResending(false);
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    const otp = digits.join("");
    if (otp.length === OTP_LENGTH) {
      onSubmit(otp);
    }
  };

  // Mask email: u***@g***le.com
  const maskedEmail = (() => {
    const at = email.indexOf("@");
    if (at <= 0) return email;
    const local = email.slice(0, at);
    const domain = email.slice(at + 1);
    const domainDot = domain.lastIndexOf(".");
    const domainName = domain.slice(0, domainDot);
    const tld = domain.slice(domainDot);
    return `${local[0]}***@${domainName[0]}***${tld}`;
  })();

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="text-center mb-2">
        <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-[#667eea] to-[#764ba2] flex items-center justify-center mx-auto mb-4 shadow-lg">
          <ShieldCheck className="w-8 h-8 text-white" />
        </div>
        <h2 className="text-2xl font-bold text-foreground">Verify OTP</h2>
        <p className="text-muted-foreground text-sm mt-1">
          Enter the 6-digit code sent to your email{" "}
          <span className="font-semibold text-foreground">{maskedEmail}</span>
        </p>
      </div>

      {/* OTP digit inputs */}
      <div className="flex justify-center gap-2.5">
        {digits.map((digit, index) => (
          <input
            key={index}
            ref={(el) => { inputRefs.current[index] = el; }}
            type="text"
            inputMode="numeric"
            maxLength={1}
            value={digit}
            onChange={(e) => handleDigitChange(index, e.target.value)}
            onKeyDown={(e) => handleKeyDown(index, e)}
            onPaste={index === 0 ? handlePaste : undefined}
            className="w-12 h-14 text-center text-xl font-bold rounded-xl border-2 border-input bg-background text-foreground focus:border-[#667eea] focus:ring-2 focus:ring-[#667eea]/30 outline-none transition-all duration-200"
            id={`otp-digit-${index}`}
            autoComplete="one-time-code"
          />
        ))}
      </div>

      {error && (
        <p className="text-destructive text-sm text-center animate-in fade-in slide-in-from-top-1 duration-200">
          {error}
        </p>
      )}

      <Button
        type="submit"
        size="lg"
        className="w-full rounded-xl bg-gradient-to-r from-[#667eea] to-[#764ba2] hover:from-[#5a6fd6] hover:to-[#6a4299] text-white shadow-lg"
        disabled={digits.join("").length !== OTP_LENGTH || isLoading}
        id="verify-otp-btn"
      >
        {isLoading ? (
          <>
            <Loader2 className="w-4 h-4 animate-spin" />
            <span>Verifying...</span>
          </>
        ) : (
          <span>Verify OTP</span>
        )}
      </Button>

      {/* Resend + Back */}
      <div className="flex items-center justify-between text-sm">
        <button
          type="button"
          onClick={onBack}
          className="text-muted-foreground hover:text-foreground transition-colors flex items-center gap-1 cursor-pointer"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          Go back
        </button>

        {countdown > 0 ? (
          <span className="text-muted-foreground">
            Resend in{" "}
            <span className="font-semibold text-foreground">{countdown}s</span>
          </span>
        ) : (
          <button
            type="button"
            onClick={handleResend}
            disabled={isResending}
            className="text-[#667eea] hover:text-[#5a6fd6] font-medium transition-colors cursor-pointer disabled:opacity-50"
          >
            {isResending ? "Sending..." : "Resend OTP"}
          </button>
        )}
      </div>
    </form>
  );
}
