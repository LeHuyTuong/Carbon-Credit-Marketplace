import React, { useState } from "react";
import {
  Box,
  Button,
  TextField,
  Typography,
  useTheme,
  Paper,
  CircularProgress,
} from "@mui/material";
import { tokens } from "@/theme";
import SupervisorAccount from "@mui/icons-material/SupervisorAccount";
import { useNavigate } from "react-router-dom";
import { apiLogin, checkKYCCVA } from "@/apiCVA/apiAuthor.js";
import { useAuth } from "@/context/AuthContext.jsx";

const CVALogin = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();
  const { login } = useAuth();

  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  // 🧩 Cập nhật input form
  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  // 🚀 Submit login
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (!form.email || !form.password) {
      setError("Please fill in all fields!");
      return;
    }

    try {
      setLoading(true);
      console.log("Attempting login with:", form.email);

      // ✅ Gọi API login
      const res = await apiLogin(form.email, form.password);
      console.log("Login API response:", res);

      if (!res?.jwt) throw new Error("Invalid login response from server");

      // ✅ Lưu user vào context (role = CVA)
      login({ ...res.user, role: "CVA" }, res.jwt, true);

      // ✅ Gọi check KYC
      const kycRes = await checkKYCCVA();
      console.log("Full KYC check:", kycRes);

      // ✅ Điều hướng
      if (kycRes && kycRes.id) {
        console.log("✅ KYC found → Go to Dashboard");
        navigate("/cva/dashboard", { replace: true });
      } else {
        console.log("⚠️ No KYC found → Go to KYC page");
        navigate("/cva/kyc", { replace: true });
      }
    } catch (err) {
      console.error("Login Error:", err);

      // Chuẩn hóa message lỗi
      const apiMsg =
        err?.responseStatus?.responseMessage ||
        err?.message ||
        "Login failed! Please try again.";

      setError(apiMsg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      height="100vh"
      display="flex"
      justifyContent="center"
      alignItems="center"
      sx={{
        background: `linear-gradient(135deg, ${colors.primary[400]}, ${colors.greenAccent[700]})`,
      }}
    >
      <Paper
        elevation={8}
        sx={{
          p: 5,
          width: "380px",
          borderRadius: "18px",
          textAlign: "center",
          backgroundColor: colors.primary[500],
        }}
      >
        {/* 🧭 Header */}
        <Box display="flex" flexDirection="column" alignItems="center" mb={3}>
          <SupervisorAccount
            sx={{ fontSize: 48, color: colors.greenAccent[500], mb: 1 }}
          />
          <Typography
            variant="h4"
            fontWeight="bold"
color={colors.greenAccent[400]}
          >
            CVA Login
          </Typography>
          <Typography variant="body2" color={colors.grey[300]}>
            Please sign in with your CVA email
          </Typography>
        </Box>

        {/* 🧩 Form */}
        <form onSubmit={handleSubmit}>
          <TextField
            label="Email"
            name="email"
            type="email"
            value={form.email}
            onChange={handleChange}
            fullWidth
            variant="filled"
            sx={{
              mb: 2,
              backgroundColor: colors.primary[400],
              borderRadius: "6px",
            }}
          />
          <TextField
            label="Password"
            name="password"
            type="password"
            value={form.password}
            onChange={handleChange}
            fullWidth
            variant="filled"
            sx={{
              mb: 2,
              backgroundColor: colors.primary[400],
              borderRadius: "6px",
            }}
          />

          {error && (
            <Typography
              color="error"
              variant="body2"
              sx={{ mb: 2, fontWeight: "bold" }}
            >
              {error}
            </Typography>
          )}

          <Button
            type="submit"
            fullWidth
            variant="contained"
            disabled={loading}
            sx={{
              mt: 1,
              py: 1.2,
              backgroundColor: colors.greenAccent[600],
              color: colors.grey[900],
              fontWeight: "bold",
              "&:hover": { backgroundColor: colors.greenAccent[700] },
            }}
          >
            {loading ? <CircularProgress size={24} color="inherit" /> : "Login"}
          </Button>
        </form>

        <Typography
          variant="caption"
          display="block"
          sx={{ mt: 3, color: colors.grey[400] }}
        >
          © 2025 EV-CarbonX System
        </Typography>
      </Paper>
    </Box>
  );
};

export default CVALogin;