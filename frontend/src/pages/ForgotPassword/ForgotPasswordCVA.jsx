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
import MarkEmailReadIcon from "@mui/icons-material/MarkEmailRead";
import { useNavigate } from "react-router-dom";

const CVAForgotPassword = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const handleSubmit = (e) => {
    e.preventDefault();

    if (!email) {
      setError("Please enter your email!");
      return;
    }

    // Giả lập gửi yêu cầu reset (sau này bạn thay bằng API thật)
    if (email === "tinbaoblizard567@gmail.com") {
      setMessage("A reset link has been sent to your email.");
      setError("");
      setTimeout(() => navigate("/cva/change-password"), 3000); // quay lại sau 3s
    } else {
      setError("Email not found in cva records!");
      setMessage("");
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
        <Box display="flex" flexDirection="column" alignItems="center" mb={3}>
          <MarkEmailReadIcon
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
            Forgot Password
          </Typography>
          <Typography variant="body2" color={colors.grey[300]}>
            Enter your cva email to reset your password
          </Typography>
        </Box>

        <form onSubmit={handleSubmit}>
          <TextField
            label="Admin Email"
            type="email"
            fullWidth
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
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
            <Typography color={colors.greenAccent[500]} variant="body2" sx={{ mb: 2 }}>
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
            Send
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

export default CVAForgotPassword;
