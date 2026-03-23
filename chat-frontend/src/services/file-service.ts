// ==============================
// File Service
// ==============================

import { apiGet, apiUpload } from "./api";
import type { UploadResponse, DocumentUrlResponse } from "@/types";
import { MAX_FILE_SIZE } from "@/lib/constants";
import { useAuthStore } from "@/store/auth-store";

/** File validation error */
export class FileValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "FileValidationError";
  }
}

/**
 * Validate a file before upload.
 * Throws FileValidationError if validation fails.
 */
export function validateFile(file: File): void {
  if (file.size > MAX_FILE_SIZE) {
    const sizeMB = Math.round(MAX_FILE_SIZE / (1024 * 1024));
    throw new FileValidationError(`File size exceeds ${sizeMB}MB limit.`);
  }
}

/** Upload a file attachment (no progress) */
export async function uploadFile(file: File): Promise<UploadResponse> {
  validateFile(file);
  const formData = new FormData();
  formData.append("file", file);
  return apiUpload<UploadResponse>("/api/upload", formData);
}

/**
 * Upload a file with progress tracking.
 * Returns { promise, abort } where abort() cancels the upload.
 */
export function uploadFileWithProgress(
  file: File,
  onProgress: (percent: number) => void
): { promise: Promise<UploadResponse>; abort: () => void } {
  validateFile(file);

  const xhr = new XMLHttpRequest();
  const formData = new FormData();
  formData.append("file", file);

  const promise = new Promise<UploadResponse>((resolve, reject) => {
    xhr.upload.addEventListener("progress", (e) => {
      if (e.lengthComputable) {
        onProgress(Math.round((e.loaded / e.total) * 100));
      }
    });

    xhr.addEventListener("load", () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          resolve(JSON.parse(xhr.responseText));
        } catch {
          reject(new Error("Invalid response"));
        }
      } else {
        reject(new Error(`Upload failed: ${xhr.status}`));
      }
    });

    xhr.addEventListener("error", () => reject(new Error("Upload failed")));
    xhr.addEventListener("abort", () => reject(new Error("Upload cancelled")));

    const token = useAuthStore.getState().token;
    xhr.open("POST", "/api/upload");
    if (token) xhr.setRequestHeader("Authorization", `Bearer ${token}`);
    xhr.send(formData);
  });

  return { promise, abort: () => xhr.abort() };
}

/**
 * Regenerate a presigned URL for a document (when the old URL has expired).
 * NOTE: Backend DocumentController is currently a stub, so this may 404.
 * Falls back to original URL if regeneration fails.
 */
export async function getDocumentUrl(documentId: number): Promise<string | null> {
  try {
    const response = await apiGet<DocumentUrlResponse>(`/api/documents/${documentId}/url`);
    return response.url;
  } catch {
    console.warn(`Could not regenerate URL for document ${documentId} — endpoint may not exist yet`);
    return null;
  }
}

/**
 * Try to load an image URL. If it 403s (expired), try to regenerate via documentId.
 * Returns the working URL, or the original URL as fallback.
 */
export async function regenerateUrlOnError(
  originalUrl: string,
  documentId: number
): Promise<string> {
  try {
    const res = await fetch(originalUrl, { method: "HEAD" });
    if (res.ok) return originalUrl;
  } catch {
    // Network error — try regeneration
  }

  const newUrl = await getDocumentUrl(documentId);
  return newUrl || originalUrl; // Fallback to original if regeneration fails
}

