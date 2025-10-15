// src/themeCVA.js
import { createContext, useState, useMemo } from "react";
import { createTheme } from "@mui/material/styles";

// Màu xanh ngọc dịu – không chói
export const tokens = (mode) => ({
  ...(mode === "dark"
    ? {
        grey: {
          100: "#f0f0f0",
          200: "#d9d9d9",
          300: "#bfbfbf",
          400: "#a6a6a6",
          500: "#8c8c8c",
          600: "#666666",
          700: "#4d4d4d",
          800: "#333333",
          900: "#1a1a1a",
        },
        primary: {
          100: "#d6f2ec",
          200: "#ace5d8",
          300: "#82d8c5",
          400: "#58cbb1",
          500: "#2ebe9d", // xanh ngọc chính
          600: "#24987e",
          700: "#1a7260",
          800: "#114d41",
          900: "#092721",
        },
        greenAccent: {
          100: "#e5faf1",
          200: "#b8f1d9",
          300: "#8ae8c1",
          400: "#5ddfa9",
          500: "#30d691",
          600: "#26ab74",
          700: "#1d8157",
          800: "#13563a",
          900: "#0a2b1d",
        },
        blueAccent: {
          100: "#e0f7fa",
          200: "#b2ebf2",
          300: "#80deea",
          400: "#4dd0e1",
          500: "#26c6da",
          600: "#00bcd4",
          700: "#0097a7",
          800: "#006978",
          900: "#003b49",
        },
      }
    : {
        grey: {
          100: "#1a1a1a",
          200: "#333333",
          300: "#4d4d4d",
          400: "#666666",
          500: "#808080",
          600: "#999999",
          700: "#b3b3b3",
          800: "#cccccc",
          900: "#e6e6e6",
        },
        primary: {
          100: "#092721",
          200: "#114d41",
          300: "#1a7260",
          400: "#24987e",
          500: "#2ebe9d",
          600: "#58cbb1",
          700: "#82d8c5",
          800: "#ace5d8",
          900: "#d6f2ec",
        },
        greenAccent: {
          100: "#0a2b1d",
          200: "#13563a",
          300: "#1d8157",
          400: "#26ab74",
          500: "#30d691",
          600: "#5ddfa9",
          700: "#8ae8c1",
          800: "#b8f1d9",
          900: "#e5faf1",
        },
        blueAccent: {
          100: "#003b49",
          200: "#006978",
          300: "#0097a7",
          400: "#00bcd4",
          500: "#26c6da",
          600: "#4dd0e1",
          700: "#80deea",
          800: "#b2ebf2",
          900: "#e0f7fa",
        },
      }),
});

// Theme setup
export const themeSettings = (mode) => {
  const colors = tokens(mode);
  return {
    palette: {
      mode: mode,
      ...(mode === "dark"
        ? {
            primary: { main: colors.primary[500] },
            secondary: { main: colors.greenAccent[500] },
            neutral: {
              dark: colors.grey[700],
              main: colors.grey[500],
              light: colors.grey[100],
            },
            background: { default: colors.primary[900] }, // nền xanh đậm dịu
          }
        : {
            primary: { main: colors.primary[400] },
            secondary: { main: colors.greenAccent[500] },
            neutral: {
              dark: colors.grey[700],
              main: colors.grey[500],
              light: colors.grey[100],
            },
            background: { default: "#f4fbf9" }, // xanh ngọc nhạt pha trắng
          }),
    },
    typography: {
      fontFamily: ["Inter", "sans-serif"].join(","),
      fontSize: 13,
      h1: { fontSize: 38, fontWeight: 600 },
      h2: { fontSize: 30, fontWeight: 600 },
      h3: { fontSize: 24, fontWeight: 500 },
      h4: { fontSize: 20, fontWeight: 500 },
      h5: { fontSize: 16, fontWeight: 500 },
      h6: { fontSize: 14, fontWeight: 400 },
    },
  };
};

// Context & hook
export const ColorModeContext = createContext({
  toggleColorMode: () => {},
});

export const useMode = () => {
  const [mode, setMode] = useState("dark");

  const colorMode = useMemo(
    () => ({
      toggleColorMode: () =>
        setMode((prev) => (prev === "light" ? "dark" : "light")),
    }),
    []
  );

  const theme = useMemo(() => createTheme(themeSettings(mode)), [mode]);
  return [theme, colorMode];
};
