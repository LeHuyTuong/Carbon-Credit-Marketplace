// ===================== useSnackbar.js =====================
import { useState, useCallback } from "react";
import CustomSnackbar from "@/components/SnackBar/CustomSnackbar.jsx";

export const useSnackbar = () => {
  const [snackbar, setSnackbar] = useState({
    open: false,
    type: "info",
    text: "",
    duration: 5000,
  });

  // Mở snackbar
  const showSnackbar = useCallback((type, text, duration = 5000) => {
    setSnackbar({ open: true, type, text, duration });
  }, []);

  // Đóng snackbar
  const closeSnackbar = useCallback(() => {
    setSnackbar((prev) => ({ ...prev, open: false }));
  }, []);

  // Component snackbar JSX
  const SnackbarComponent = (
    <CustomSnackbar
      open={snackbar.open}
      type={snackbar.type}
      text={snackbar.text}
      onClose={closeSnackbar}
      duration={snackbar.duration}
    />
  );

  return { showSnackbar, SnackbarComponent };
};
