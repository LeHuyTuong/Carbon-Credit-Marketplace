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
import { apiLogin } from "@/apiCVA/apiAuthor.js"; // API login CVA
import { checkKYCCVA } from "@/apiCVA/apiAuthor.js"; // API check KYC

const CVALogin = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();

  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      // Gọi API login
      const resData = await apiLogin(form.email, form.password);

      if (resData) {
        console.log(" Login success:", resData);

        // Sau khi login, check KYC status
        const kycStatus = await checkKYCCVA();
        console.log(" KYC check:", kycStatus);

        if (kycStatus) {
          // Đã có thông tin KYC
          navigate("/cva/dashboard");
        } else {
          // Chưa có → chuyển sang form KYC
          navigate("/cva/kyc");
        }
      }
    } catch (err) {
      console.error(" Login Error:", err);
      setError(err.message || "Login failed. Please try again!");
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
        <Box display="flex" flexDirection="column" alignItems="center" mb={3}>
          <SupervisorAccount
            sx={{
              fontSize: 48,
              color: colors.greenAccent[500],
              mb: 1,
            }}
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

        <form onSubmit={handleSubmit}>
          <TextField
            label="Email"
            name="email"
            type="email"
            value={form.email}
            onChange={handleChange}
            fullWidth
            required
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
            required
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
              "&:hover": {
                backgroundColor: colors.greenAccent[700],
              },
            }}
          >
            {loading ? <CircularProgress size={26} color="inherit" /> : "Login"}
          </Button>
        </form>

        <Typography
          variant="caption"
          display="block"
          sx={{ mt: 3, color: colors.grey[400] }}
        >
          © 2025 EV-Carbon Credit System
        </Typography>
      </Paper>
    </Box>
  );
};

export default CVALogin;
