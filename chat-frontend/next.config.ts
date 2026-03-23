import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Fix workspace root — prevents Turbopack from using a stray lockfile
  // outside this project as the root directory
  turbopack: {
    root: import.meta.dirname,
  },

  // Proxy API/auth requests to Spring Boot backend
  // NOTE: /chat rewrite removed — SockJS uses absolute backend URL directly
  // to avoid conflicting with the /chat/[id] page route
  async rewrites() {
    const backendUrl = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

    return [
      {
        source: "/api/:path*",
        destination: `${backendUrl}/api/:path*`,
      },
      {
        source: "/auth/:path*",
        destination: `${backendUrl}/auth/:path*`,
      },
    ];
  },

  // Allow Cloudinary CDN images to be optimized by Next.js Image component
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "res.cloudinary.com",
      },
    ],
  },
};

export default nextConfig;
