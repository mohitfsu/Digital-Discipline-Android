import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        background: "#090D16",
        surface: "#0F172A",
        surfaceLight: "#1E293B",
        borderDark: "#1E293B",
        accent: {
          blue: "#38BDF8",
          indigo: "#2563EB",
          green: "#10B981",
          emerald: "#059669",
          red: "#EF4444",
          amber: "#FBBF24",
        }
      },
    },
  },
  plugins: [],
};
export default config;
