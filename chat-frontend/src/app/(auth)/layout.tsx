import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Login — SkibidiChat",
  description: "Sign in to SkibidiChat with OTP authentication",
};

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-[#667eea] to-[#764ba2] p-5">
      {children}
    </div>
  );
}
