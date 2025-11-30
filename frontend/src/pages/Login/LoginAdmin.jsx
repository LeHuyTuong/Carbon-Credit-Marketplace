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
import { apiLogin, checkKYCAdmin } from "@/apiAdmin/apiLogin.js";
import { useAuth } from "@/context/AuthContext.jsx";
import { useSnackbar } from "@/hooks/useSnackbar.jsx";

const AdminLogin = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();
  const { login } = useAuth();

  const { showSnackbar, SnackbarComponent } = useSnackbar(); // hook snackbar

  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
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
    setError("");

    if (!form.email || !form.password) {
      showSnackbar("warning", "Please fill in all fields!");
      return;
    }

    try {
      setLoading(true);

      //  Gọi API login
      const res = await apiLogin(form.email, form.password);

      if (res?.jwt) {
        if (!res.roles || res.roles.length === 0) {
          showSnackbar("error", "Login failed! No role returned.");
          setLoading(false);
          return;
        }

        // Lấy role từ API
        const role = res.roles?.[0]; // ADMIN

        // Kiểm tra đúng role
        if (role !== "ADMIN") {
          showSnackbar("error", "Access denied! You are not an Admin.");
          setLoading(false);
          return;
        }

        // Tạo user để lưu vào AuthContext
        const userObject = {
          email: form.email,
          role: role,
        };

        // Lưu vào AuthContext
        login(userObject, res.jwt, true);

        //  Kiểm tra KYC
        const kycRes = await checkKYCAdmin();
        console.log(" Full KYC check:", kycRes);

        //  Điều hướng dựa theo KYC có hay chưa
        if (kycRes && kycRes.id) {
          showSnackbar(
            "success",
            "Login successfull! Redirecting to dashboard..."
          );
          setTimeout(
            () => navigate("/admin/dashboard", { replace: true }),
            3000
          );
        } else {
          showSnackbar("info", "No KYC found. Redirecting to KYC page...");
          setTimeout(() => navigate("/admin/kyc", { replace: true }), 3000);
        }
      } else {
        showSnackbar(
          "error",
          res?.message || "Login failed. Please try again."
        );
      }
    } catch (err) {
      console.error(" Login error:", err);
      showSnackbar("error", err.message || "An unexpected error occurred!");
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
        background: `linear-gradient(135deg, ${colors.primary[400]}, ${colors.blueAccent[700]})`,
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
        {/*  Header */}
        <Box display="flex" flexDirection="column" alignItems="center" mb={3}>
          <SupervisorAccount
            sx={{ fontSize: 48, color: colors.greenAccent[500], mb: 1 }}
          />
          <Typography
            variant="h4"
            fontWeight="bold"
            color={colors.blueAccent[400]}
          >
            Admin Login
          </Typography>
          <Typography variant="body2" color={colors.grey[300]}>
            Please sign in with your admin email
          </Typography>
        </Box>

        {/*  Form */}
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
              mb: 1,
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
              "&:hover": {
                backgroundColor: colors.greenAccent[700],
              },
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

export default AdminLogin;
