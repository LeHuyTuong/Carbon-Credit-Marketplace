import React, { useState, useRef } from "react";
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
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import { useNavigate } from "react-router-dom";
import { verifyOTP } from "@/apiCVA/apiAuthor.js"; // API verify OTP
import { useAuth } from "@/context/AuthContext.jsx";

const OTPCVA = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();
  const { login } = useAuth();

  const [otp, setOtp] = useState(["", "", "", "", "", ""]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const inputRefs = useRef([]);

  const handleOtpChange = (index, value) => {
    if (!/^\d*$/.test(value)) return; // Chỉ nhập số
    const newOtp = [...otp];
    newOtp[index] = value;
    setOtp(newOtp);

    // Tự động focus ô tiếp theo
    if (value && index < 5) {
      inputRefs.current[index + 1].focus();
    }
  };

  const handleVerifyOTP = async () => {
    const otpStr = otp.join("");
    if (otpStr.length !== 6) {
      setError("Please enter the complete 6-digit OTP!");
      return;
    }
    try {
      setLoading(true);
      const res = await verifyOTP(otpStr); // Gọi API verify OTP trực tiếp
      if (!res?.jwt) throw new Error("Invalid OTP response");

      login({ ...res.user, role: "CVA" }, res.jwt, true);
      navigate("/cva/carbonX/mkp/login", { replace: true });
    } catch (err) {
      console.error("Verify OTP Error:", err);
      setError(err.message || "OTP verification failed");
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
          <LockOutlinedIcon
            sx={{ fontSize: 48, color: colors.greenAccent[500], mb: 1 }}
          />
          <Typography variant="h4" fontWeight="bold" color={colors.greenAccent[400]}>
            CVA OTP Login
          </Typography>
          <Typography variant="body2" color={colors.grey[300]}>
            Enter your 6-digit OTP to login
          </Typography>
        </Box>

        {/* OTP Inputs */}
        <Box display="flex" justifyContent="space-between" mb={2}>
          {otp.map((digit, idx) => (
            <TextField
              key={idx}
              inputRef={(el) => (inputRefs.current[idx] = el)}
              value={digit}
              onChange={(e) => handleOtpChange(idx, e.target.value)}
              inputProps={{ maxLength: 1, style: { textAlign: "center" } }}
              sx={{
                width: "45px",
                backgroundColor: colors.primary[400],
                borderRadius: "6px",
              }}
              variant="filled"
            />
          ))}
        </Box>

        {error && (
          <Typography color="error" variant="body2" sx={{ mb: 2, fontWeight: "bold" }}>
            {error}
          </Typography>
        )}

        {/* Verify OTP Button */}
        <Button
          fullWidth
          variant="contained"
          onClick={handleVerifyOTP}
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
          {loading ? <CircularProgress size={24} color="inherit" /> : "Verify OTP"}
        </Button>

        <Typography variant="caption" display="block" sx={{ mt: 3, color: colors.grey[400] }}>
          © 2025 EV-CarbonX System
        </Typography>
      </Paper>
    </Box>
  );
};

export default OTPCVA;
