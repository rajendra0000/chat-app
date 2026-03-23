"use client";

// ==============================
// Phone + Email Input Component
// ==============================

import { useState, type FormEvent } from "react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Loader2, Phone, Mail, ArrowRight, Globe } from "lucide-react";

// Top countries with dial codes — sorted by popularity for chat apps
const COUNTRY_CODES = [
  { code: "+91",  flag: "🇮🇳", name: "India" },
  { code: "+1",   flag: "🇺🇸", name: "USA / Canada" },
  { code: "+44",  flag: "🇬🇧", name: "UK" },
  { code: "+971", flag: "🇦🇪", name: "UAE" },
  { code: "+966", flag: "🇸🇦", name: "Saudi Arabia" },
  { code: "+61",  flag: "🇦🇺", name: "Australia" },
  { code: "+49",  flag: "🇩🇪", name: "Germany" },
  { code: "+33",  flag: "🇫🇷", name: "France" },
  { code: "+65",  flag: "🇸🇬", name: "Singapore" },
  { code: "+60",  flag: "🇲🇾", name: "Malaysia" },
  { code: "+880", flag: "🇧🇩", name: "Bangladesh" },
  { code: "+92",  flag: "🇵🇰", name: "Pakistan" },
  { code: "+94",  flag: "🇱🇰", name: "Sri Lanka" },
  { code: "+977", flag: "🇳🇵", name: "Nepal" },
  { code: "+81",  flag: "🇯🇵", name: "Japan" },
  { code: "+82",  flag: "🇰🇷", name: "South Korea" },
  { code: "+86",  flag: "🇨🇳", name: "China" },
  { code: "+55",  flag: "🇧🇷", name: "Brazil" },
  { code: "+7",   flag: "🇷🇺", name: "Russia" },
  { code: "+27",  flag: "🇿🇦", name: "South Africa" },
];

interface PhoneInputProps {
  onSubmit: (phone: string, email: string) => Promise<void>;
  isLoading: boolean;
  error: string | null;
}

export function PhoneInput({ onSubmit, isLoading, error }: PhoneInputProps) {
  const [dialCode, setDialCode] = useState("+91");
  const [localNumber, setLocalNumber] = useState("");
  const [email, setEmail] = useState("");

  // Only digits allowed in local part; length varies by country
  const handlePhoneInput = (value: string) => {
    const cleaned = value.replace(/\D/g, "").slice(0, 12);
    setLocalNumber(cleaned);
  };

  const isEmailValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  const isPhoneValid = localNumber.length >= 7;
  const canSubmit = isPhoneValid && isEmailValid && !isLoading;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!canSubmit) return;
    // Construct E.164 format: +[dialCode][localNumber]
    const e164Phone = `${dialCode}${localNumber}`;
    await onSubmit(e164Phone, email.trim().toLowerCase());
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <div className="text-center mb-2">
        <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-[#667eea] to-[#764ba2] flex items-center justify-center mx-auto mb-4 shadow-lg">
          <Globe className="w-8 h-8 text-white" />
        </div>
        <h2 className="text-2xl font-bold text-foreground">Welcome Back</h2>
        <p className="text-muted-foreground text-sm mt-1">
          Enter your details to receive a verification code
        </p>
      </div>

      {/* Phone number row — country code + number */}
      <div className="space-y-1.5">
        <label className="text-sm font-medium text-foreground flex items-center gap-1.5">
          <Phone className="w-3.5 h-3.5" />
          Phone Number
        </label>
        <div className="flex gap-2">
          {/* Country code select */}
          <select
            value={dialCode}
            onChange={(e) => setDialCode(e.target.value)}
            className="h-12 px-2 rounded-xl border border-input bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-[#667eea]/50 cursor-pointer shrink-0"
            id="country-code-select"
            aria-label="Country code"
          >
            {COUNTRY_CODES.map((c) => (
              <option key={c.code + c.name} value={c.code}>
                {c.flag} {c.code}
              </option>
            ))}
          </select>

          {/* Local number */}
          <Input
            type="tel"
            inputMode="numeric"
            placeholder="Mobile number"
            value={localNumber}
            onChange={(e) => handlePhoneInput(e.target.value)}
            className="h-12 text-base rounded-xl text-foreground bg-background flex-1"
            autoFocus
            id="phone-number-input"
          />
        </div>
        <p className="text-xs text-muted-foreground pl-1">
          Full number: <span className="font-mono text-foreground">{dialCode}{localNumber || "XXXXXXXXXX"}</span>
        </p>
      </div>

      {/* Email row */}
      <div className="space-y-1.5">
        <label className="text-sm font-medium text-foreground flex items-center gap-1.5">
          <Mail className="w-3.5 h-3.5" />
          Email <span className="text-muted-foreground font-normal">(OTP will be sent here)</span>
        </label>
        <Input
          type="email"
          inputMode="email"
          placeholder="you@example.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="h-12 text-base rounded-xl text-foreground bg-background"
          id="email-input"
          autoComplete="email"
        />
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
        disabled={!canSubmit}
        id="send-otp-btn"
      >
        {isLoading ? (
          <>
            <Loader2 className="w-4 h-4 animate-spin" />
            <span>Checking...</span>
          </>
        ) : (
          <>
            <span>Continue</span>
            <ArrowRight className="w-4 h-4" />
          </>
        )}
      </Button>
    </form>
  );
}
