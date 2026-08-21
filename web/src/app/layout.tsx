import type { Metadata } from "next";
import "./globals.css";
import { AuthProvider } from "@/context/AuthContext";
import { FamilyProvider } from "@/context/FamilyContext";
import LayoutClient from "./LayoutClient";

export const metadata: Metadata = {
  title: "Digital Discipline — Parent Control Center",
  description: "Modern cloud control center for parents to guide healthy digital habits, app limits, and mindful interventions.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="dark">
      <body className="bg-[#090D16] text-slate-100 min-h-screen antialiased">
        <AuthProvider>
          <FamilyProvider>
            <LayoutClient>{children}</LayoutClient>
          </FamilyProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
