import React, { useState } from "react";
import { Box, Button, TextField, Typography, useTheme, Paper } from "@mui/material";
import { tokens } from "@/theme";
import SupervisorAccount from "@mui/icons-material/SupervisorAccount";
import { useNavigate } from "react-router-dom";

const AdminLogin = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();

  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    // demo logic đăng nhập — sau này bạn sẽ thay bằng API login thực tế
    if (form.email === "phanthuthuongta.aec@gmail.com" && form.password === "Maythichnhinko567@") {
      navigate("/admin/kyc");
    } else {
      setError("Invalid email or password!");
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
        {/* Icon + title */}
        <Box display="flex" flexDirection="column" alignItems="center" mb={3}>
          <SupervisorAccount
            sx={{
              fontSize: 48,
              color: colors.greenAccent[500],
              mb: 1,
            }}
          />
          <Typography variant="h4" fontWeight="bold" color={colors.blueAccent[400]}>
            Admin Login
          </Typography>
          <Typography variant="body2" color={colors.grey[300]}>
            Please sign in with your admin email
          </Typography>
        </Box>

        {/* Form login */}
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
            Login
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

export default AdminLogin;
