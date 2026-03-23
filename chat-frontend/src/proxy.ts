// ==============================
// Next.js Proxy — Auth Route Protection (Next.js 16+)
// ==============================

import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

/**
 * Routes that do NOT require authentication.
 */
const PUBLIC_ROUTES = ["/login", "/register"];

/**
 * Static file patterns to skip middleware for.
 */
const STATIC_PATTERNS = [
  "/_next",
  "/favicon.ico",
  "/default-avatar.jpg",
  "/api",    // API routes are proxied to backend
  "/auth",   // Auth routes are proxied to backend  
  "/chat",   // WebSocket endpoint proxied to backend
];

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Skip middleware for static files and API routes
  if (STATIC_PATTERNS.some((pattern) => pathname.startsWith(pattern))) {
    return NextResponse.next();
  }

  // Check for auth token in cookies or Authorization header
  // Note: Since we use localStorage for JWT storage in the client,
  // middleware cannot directly check localStorage. Instead, we rely on
  // client-side auth checks. This middleware handles the redirect for
  // direct URL access patterns.
  const isPublicRoute = PUBLIC_ROUTES.some((route) => pathname.startsWith(route));

  // For the root path, redirect to /login (client-side will redirect to /chat if authenticated)
  if (pathname === "/") {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  // Allow public routes
  if (isPublicRoute) {
    return NextResponse.next();
  }

  // All other routes: allow through (client-side auth guard handles the rest)
  return NextResponse.next();
}

export const config = {
  matcher: [
    /*
     * Match all request paths except:
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     */
    "/((?!_next/static|_next/image|favicon.ico).*)",
  ],
};
