// ==============================
// API Client — Centralized Error Handling
// ==============================

/**
 * Custom error class for API responses.
 * Carries the HTTP status code and parsed error body.
 */
export class ApiError extends Error {
  public readonly status: number;
  public readonly body: unknown;

  constructor(status: number, message: string, body?: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }

  /** True if the error is a 401 Unauthorized */
  get isUnauthorized(): boolean {
    return this.status === 401;
  }

  /** True if the error is a 403 Forbidden */
  get isForbidden(): boolean {
    return this.status === 403;
  }

  /** True if the error is a 404 Not Found */
  get isNotFound(): boolean {
    return this.status === 404;
  }

  /** True if the error is a server error (5xx) */
  get isServerError(): boolean {
    return this.status >= 500;
  }
}

/**
 * Retrieves the JWT token from localStorage.
 * Returns null if not available (e.g. server-side rendering).
 */
function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("Authorization");
}

/**
 * Builds the standard auth headers for an API request.
 */
function getAuthHeaders(): Record<string, string> {
  const token = getToken();
  if (!token) return {};
  return { Authorization: `Bearer ${token}` };
}

/**
 * Processes a fetch Response — throws ApiError for non-2xx statuses.
 */
async function handleResponse<T>(response: Response, parseAs: "json" | "text" = "json"): Promise<T> {
  if (!response.ok) {
    let errorBody: unknown = null;
    let errorMessage = `HTTP ${response.status}`;

    try {
      const contentType = response.headers.get("content-type") || "";
      if (contentType.includes("application/json")) {
        errorBody = await response.json();
        errorMessage = (errorBody as Record<string, string>)?.message || errorMessage;
      } else {
        errorMessage = (await response.text()) || errorMessage;
      }
    } catch {
      // Ignore parse errors — use the default error message
    }

    const error = new ApiError(response.status, errorMessage, errorBody);

    // Auto-redirect to login on 401
    if (error.isUnauthorized && typeof window !== "undefined") {
      localStorage.removeItem("Authorization");
      window.location.href = "/login";
    }

    throw error;
  }

  if (parseAs === "text") {
    return (await response.text()) as T;
  }

  // Handle empty responses (e.g. 204 No Content)
  const text = await response.text();
  if (!text) return null as T;
  return JSON.parse(text) as T;
}

// ==============================
// HTTP Methods
// ==============================

/**
 * GET request with authentication.
 */
export async function apiGet<T>(url: string, parseAs: "json" | "text" = "json"): Promise<T> {
  const response = await fetch(url, {
    method: "GET",
    headers: {
      ...getAuthHeaders(),
    },
  });
  return handleResponse<T>(response, parseAs);
}

/**
 * POST request with JSON body and authentication.
 */
export async function apiPost<T>(
  url: string,
  body?: unknown,
  parseAs: "json" | "text" = "json"
): Promise<T> {
  const headers: Record<string, string> = {
    ...getAuthHeaders(),
  };
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(url, {
    method: "POST",
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  return handleResponse<T>(response, parseAs);
}

/**
 * PUT request with JSON body and authentication.
 */
export async function apiPut<T>(
  url: string,
  body?: unknown,
  parseAs: "json" | "text" = "json"
): Promise<T> {
  const headers: Record<string, string> = {
    ...getAuthHeaders(),
  };
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(url, {
    method: "PUT",
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  return handleResponse<T>(response, parseAs);
}

/**
 * DELETE request with authentication.
 */
export async function apiDelete<T>(url: string, parseAs: "json" | "text" = "json"): Promise<T> {
  const response = await fetch(url, {
    method: "DELETE",
    headers: { ...getAuthHeaders() },
  });
  return handleResponse<T>(response, parseAs);
}

/**
 * PATCH request with JSON body and authentication.
 */
export async function apiPatch<T>(
  url: string,
  body?: unknown,
  parseAs: "json" | "text" = "json"
): Promise<T> {
  const headers: Record<string, string> = { ...getAuthHeaders() };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const response = await fetch(url, {
    method: "PATCH",
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  return handleResponse<T>(response, parseAs);
}

/**
 * POST request with FormData body (for file uploads).
 * Does NOT set Content-Type — the browser sets it with the boundary.
 */
export async function apiUpload<T>(url: string, formData: FormData): Promise<T> {
  const response = await fetch(url, {
    method: "POST",
    headers: {
      ...getAuthHeaders(),
    },
    body: formData,
  });
  return handleResponse<T>(response);
}

/**
 * PUT request with FormData body (for file uploads like profile pic).
 */
export async function apiUploadPut<T>(url: string, formData: FormData): Promise<T> {
  const response = await fetch(url, {
    method: "PUT",
    headers: {
      ...getAuthHeaders(),
    },
    body: formData,
  });
  return handleResponse<T>(response, "text");
}

/**
 * POST request without authentication (for public auth endpoints).
 */
export async function apiPostPublic<T>(
  url: string,
  body: unknown,
  parseAs: "json" | "text" = "json"
): Promise<T> {
  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  return handleResponse<T>(response, parseAs);
}
