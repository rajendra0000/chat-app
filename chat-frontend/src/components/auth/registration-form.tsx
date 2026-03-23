"use client";

// ==============================
// Registration Form Component
// ==============================

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { registerSchema, type RegisterFormValues } from "@/lib/validators";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Loader2, ArrowLeft, UserPlus } from "lucide-react";
import type { RegisterRequest } from "@/types";

interface RegistrationFormProps {
  phone: string;
  onSubmit: (data: Omit<RegisterRequest, "phone">) => Promise<void>;
  onBack: () => void;
  isLoading: boolean;
  error: string | null;
}

export function RegistrationForm({
  phone,
  onSubmit,
  onBack,
  isLoading,
  error,
}: RegistrationFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      fullName: "",
      email: "",
      dateOfBirth: "",
      gender: undefined,
      address: "",
    },
  });

  const onFormSubmit = async (data: RegisterFormValues) => {
    await onSubmit(data);
  };

  return (
    <form onSubmit={handleSubmit(onFormSubmit)} className="space-y-5">
      <div className="text-center mb-1">
        <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-[#667eea] to-[#764ba2] flex items-center justify-center mx-auto mb-4 shadow-lg">
          <UserPlus className="w-8 h-8 text-white" />
        </div>
        <h2 className="text-2xl font-bold text-foreground">Create Account</h2>
        <p className="text-muted-foreground text-sm mt-1">
          Complete your profile for{" "}
          <span className="font-semibold text-foreground">{phone}</span>
        </p>
      </div>

      {/* Full Name */}
      <div className="space-y-1.5">
        <Label htmlFor="fullName">Full Name</Label>
        <Input
          {...register("fullName")}
          id="fullName"
          placeholder="Enter your full name"
          className="h-11 rounded-xl"
          autoFocus
        />
        {errors.fullName && (
          <p className="text-destructive text-xs">{errors.fullName.message}</p>
        )}
      </div>

      {/* Email */}
      <div className="space-y-1.5">
        <Label htmlFor="email">Email</Label>
        <Input
          {...register("email")}
          id="email"
          type="email"
          placeholder="you@example.com"
          className="h-11 rounded-xl"
        />
        {errors.email && (
          <p className="text-destructive text-xs">{errors.email.message}</p>
        )}
      </div>

      {/* Date of Birth + Gender row */}
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1.5">
          <Label htmlFor="dateOfBirth">Date of Birth</Label>
          <Input
            {...register("dateOfBirth")}
            id="dateOfBirth"
            type="date"
            className="h-11 rounded-xl"
            max={new Date().toISOString().split("T")[0]}
          />
          {errors.dateOfBirth && (
            <p className="text-destructive text-xs">{errors.dateOfBirth.message}</p>
          )}
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="gender">Gender</Label>
          <select
            {...register("gender")}
            id="gender"
            className="flex h-11 w-full rounded-xl border border-input bg-transparent px-3 py-2 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <option value="">Select</option>
            <option value="male">Male</option>
            <option value="female">Female</option>
            <option value="other">Other</option>
          </select>
          {errors.gender && (
            <p className="text-destructive text-xs">{errors.gender.message}</p>
          )}
        </div>
      </div>

      {/* Address */}
      <div className="space-y-1.5">
        <Label htmlFor="address">Address</Label>
        <Input
          {...register("address")}
          id="address"
          placeholder="Enter your address"
          className="h-11 rounded-xl"
        />
        {errors.address && (
          <p className="text-destructive text-xs">{errors.address.message}</p>
        )}
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
        disabled={isLoading}
        id="register-btn"
      >
        {isLoading ? (
          <>
            <Loader2 className="w-4 h-4 animate-spin" />
            <span>Creating account...</span>
          </>
        ) : (
          <span>Register &amp; Get OTP</span>
        )}
      </Button>

      <div className="text-center">
        <button
          type="button"
          onClick={onBack}
          className="text-sm text-muted-foreground hover:text-foreground transition-colors flex items-center gap-1 mx-auto cursor-pointer"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          Back to phone input
        </button>
      </div>
    </form>
  );
}
