// ==============================
// Zod Validation Schemas
// ==============================

import { z } from "zod";
import { OTP_LENGTH } from "@/lib/constants";

/** Phone number: exactly 10 digits */
export const phoneSchema = z
  .string()
  .regex(/^\d{10}$/, "Please enter a valid 10-digit phone number");

/** OTP: exactly 6 digits */
export const otpSchema = z
  .string()
  .length(OTP_LENGTH, `OTP must be exactly ${OTP_LENGTH} digits`)
  .regex(/^\d+$/, "OTP must contain only digits");

/** Registration form */
export const registerSchema = z.object({
  fullName: z
    .string()
    .min(1, "Full name is required")
    .regex(/^[a-zA-Z\s]+$/, "Name can only contain letters and spaces"),
  email: z.string().email("Please enter a valid email address"),
  dateOfBirth: z
    .string()
    .min(1, "Date of birth is required")
    .refine(
      (val) => {
        const date = new Date(val);
        const today = new Date();
        const age = today.getFullYear() - date.getFullYear();
        return age >= 13;
      },
      { message: "You must be at least 13 years old" }
    )
    .refine(
      (val) => {
        return new Date(val) <= new Date();
      },
      { message: "Date of birth cannot be in the future" }
    ),
  gender: z.enum(["male", "female", "other", "MALE", "FEMALE", "OTHER"], {
    message: "Please select a gender",
  }),
  address: z.string().min(1, "Address is required"),
});

/** Profile update form */
export const profileSchema = z.object({
  fullName: z.string().min(1, "Full name is required"),
  dateOfBirth: z.string().optional(),
  gender: z.string().optional(),
  address: z.string().optional(),
});

/** Group creation */
export const createGroupSchema = z.object({
  title: z.string().min(1, "Group name is required").max(100, "Group name is too long"),
});

/** Edit message */
export const editMessageSchema = z.object({
  text: z.string().min(1, "Message cannot be empty").max(4000, "Message must not exceed 4000 characters"),
});

/** Type exports for form values */
export type PhoneFormValues = z.infer<typeof phoneSchema>;
export type RegisterFormValues = z.infer<typeof registerSchema>;
export type ProfileFormValues = z.infer<typeof profileSchema>;
export type CreateGroupFormValues = z.infer<typeof createGroupSchema>;
export type EditMessageFormValues = z.infer<typeof editMessageSchema>;
