import type { Metadata } from "next";
import "./globals.css";
import Providers from "./providers";

export const metadata: Metadata = {
  title: "SkibidiChat — Real-Time Messaging",
  description:
    "A modern real-time chat application with AI integration, group chats, file sharing, and more.",
  keywords: ["chat", "messaging", "real-time", "AI", "group chat"],
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className="antialiased">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
