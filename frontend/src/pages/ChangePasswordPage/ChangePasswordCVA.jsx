import React, { useState } from "react";
import {
  Box,
  Button,
  TextField,
  Typography,
  useTheme,
  Paper,
} from "@mui/material";
import { tokens } from "@/theme";
import LockResetIcon from "@mui/icons-material/LockReset";
import { useNavigate } from "react-router-dom";

const CVAChangePassword = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const handleSubmit = (e) => {
    e.preventDefault();

    if (!newPassword || !confirmPassword) {
      setError("Please fill in all fields!");
      setMessage("");
      return;
    }

    if (newPassword !== confirmPassword) {
      setError("New passwords do not match!");
      setMessage("");
      return;
    }

    // Giả lập đổi mật khẩu thành công (sau này thay bằng API thật)
    setMessage("Password has been changed successfully!");
    setError("");
    setTimeout(() => navigate("/cva/login"), 3000);
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
          width: "400px",
          borderRadius: "18px",
          textAlign: "center",
          backgroundColor: colors.primary[500],
        }}
      >
        <Box display="flex" flexDirection="column" alignItems="center" mb={3}>
          <LockResetIcon
            sx={{
              fontSize: 48,
              color: colors.greenAccent[500],
              mb: 1,
            }}
          />
          <Typography
            variant="h4"
            fontWeight="bold"
            color={colors.blueAccent[400]}
          >
            Change Password
          </Typography>
          <Typography variant="body2" color={colors.grey[300]}>
            Enter your new password to complete the reset process
          </Typography>
        </Box>

        <form onSubmit={handleSubmit}>
          <TextField
            label="New Password"
            type="password"
            fullWidth
            required
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            variant="filled"
            sx={{
              mb: 2,
              backgroundColor: colors.primary[400],
              borderRadius: "6px",
            }}
          />

          <TextField
            label="Confirm New Password"
            type="password"
            fullWidth
            required
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            variant="filled"
            sx={{
              mb: 2,
              backgroundColor: colors.primary[400],
              borderRadius: "6px",
            }}
          />

          {error && (
            <Typography color="error" variant="body2" sx={{ mb: 2 }}>
              {error}
            </Typography>
          )}

          {message && (
            <Typography
              color={colors.greenAccent[500]}
              variant="body2"
              sx={{ mb: 2 }}
            >
              {message}
            </Typography>
          )}

          <Button
            type="submit"
            fullWidth
            variant="contained"
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
            Update Password
          </Button>

          <Button
            fullWidth
            variant="text"
            onClick={() => navigate("/cva/login")}
            sx={{
              mt: 1.5,
              color: colors.blueAccent[400],
              "&:hover": {
                color: colors.blueAccent[300],
              },
            }}
          >
            Back to Login
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

export default CVAChangePassword;
