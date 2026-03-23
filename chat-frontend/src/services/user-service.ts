// ==============================
// User Service
// ==============================

import { apiGet, apiPut, apiUploadPut } from "./api";
import type { User, UserProfile, UserSearchResult } from "@/types";
import type { UploadResponse } from "@/types/message";

/** Fetch current logged-in user */
export async function fetchCurrentUser(): Promise<User> {
  return apiGet<User>("/api/users/me");
}

/** Search users by phone number */
export async function searchUsers(phone: string): Promise<UserSearchResult[]> {
  const result = await apiGet<UserSearchResult | UserSearchResult[]>(
    `/api/users/search?phone=${encodeURIComponent(phone)}`
  );
  // API may return a single object or an array
  return Array.isArray(result) ? result : result ? [result] : [];
}

/** Get a user's profile picture URL */
export async function getUserProfilePicUrl(userId: number): Promise<string> {
  const url = await apiGet<string>(`/api/users/${userId}/pic-url`, "text");
  return url || "";
}

/** Get current user's profile picture URL */
export async function getMyProfilePicUrl(): Promise<string> {
  const url = await apiGet<string>("/api/profile/pic-url", "text");
  return url || "";
}

/** Get full profile details for editing */
export async function fetchUserProfile(): Promise<UserProfile> {
  return apiGet<UserProfile>("/api/profile/details");
}

/** Update profile details */
export async function updateUserProfile(data: UserProfile): Promise<User> {
  return apiPut<User>("/api/profile/update-details", data);
}

/** Upload profile picture — returns Cloudinary CDN URL for the new pic */
export async function uploadProfilePic(file: File): Promise<string> {
  const formData = new FormData();
  formData.append("file", file);

  // Backend returns FileUploadResponseDTO { url, fileName, documentId }
  const token = typeof window !== "undefined" ? localStorage.getItem("Authorization") : null;
  const headers: Record<string, string> = {};
  if (token) headers.Authorization = `Bearer ${token}`;

  const response = await fetch("/api/profile/upload-pic", {
    method: "PUT",
    headers,
    body: formData,
  });

  if (!response.ok) {
    throw new Error(`Upload failed: HTTP ${response.status}`);
  }

  const data: UploadResponse = await response.json();
  // Cloudinary returns permanent secure_url — no rewriting needed
  return data.url || "";
}
