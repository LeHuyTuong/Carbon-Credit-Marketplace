import React, { useEffect, useState } from "react";
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
import { useLocation, useNavigate } from "react-router-dom";
import { apiLogin, checkKYCCVA } from "@/apiCVA/apiAuthor.js";
import { useAuth } from "@/context/AuthContext.jsx";
import { useSnackbar } from "@/hooks/useSnackbar.jsx"; // import hook snackbar

const CVALogin = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();
  const { login } = useAuth();
  const { showSnackbar, SnackbarComponent } = useSnackbar(); // hook snackbar

  const [form, setForm] = useState({ email: "", password: "" });
  const [loading, setLoading] = useState(false);

  const location = useLocation();
  const preset = location.state?.preset || null;

  useEffect(() => {
    if (preset) {
      setForm({
        email: preset.email || "",
        password: preset.password || "",
      });
    }
  }, [preset]);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!form.email || !form.password) {
      showSnackbar("warning", "Please fill in all fields!");
      return;
    }

    try {
      setLoading(true);
      console.log("Attempting login with:", form.email);

      const res = await apiLogin(form.email, form.password);
      console.log("Login API response:", res);

      // Lấy đúng dữ liệu từ API
      const data = res?.responseData;

      if (!data) {
        showSnackbar(
          "error",
          "Invalid login response! No responseData returned."
        );
        return;
      }

      // Kiểm tra JWT
      if (!data.jwt) {
        showSnackbar("error", "Login failed! No JWT returned.");
        return;
      }

      // Kiểm tra roles
      if (!data.roles || data.roles.length === 0) {
        showSnackbar("error", "Login failed! No role returned.");
        return;
      }

      const role = data.roles[0];

      // Kiểm tra role có phải CVA không
      if (role !== "CVA") {
        showSnackbar("error", "Access denied! You are not a CVA user.");
        setLoading(false);
        return;
      }

      // Tạo user để lưu vào AuthContext
      const userObject = {
        email: form.email,
        role: role,
      };

      // Lưu vào AuthContext
      login(userObject, data.jwt, true);

      // Gọi check KYC
      const kycRes = await checkKYCCVA();
      console.log("Full KYC check:", kycRes);

      // Điều hướng dựa theo KYC
      if (kycRes && kycRes.id) {
        showSnackbar(
          "success",
          "Login successfull! Redirecting to dashboard..."
        );
        setTimeout(() => navigate("/cva/dashboard", { replace: true }), 3000);
      } else {
        showSnackbar("info", "No KYC found. Redirecting to KYC page...");
        setTimeout(() => navigate("/cva/kyc", { replace: true }), 3000);
      }
    } catch (err) {
      console.error("Login Error:", err);
      const apiMsg =
        err?.responseStatus?.responseMessage ||
        err?.message ||
        "Login failed! Please try again.";
      showSnackbar("error", apiMsg);
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
        {/* Header */}
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

        {/* Form */}
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

      {/* Snackbar Component */}
      {SnackbarComponent}
    </Box>
  );
};

export default CVALogin;
