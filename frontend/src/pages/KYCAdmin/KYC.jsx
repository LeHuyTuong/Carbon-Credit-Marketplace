import React, { useState } from "react";
import {
  Box,
  Button,
  TextField,
  Typography,
  useTheme,
  Paper,
  Grid,
} from "@mui/material";
import { tokens } from "@/theme";
import { useNavigate } from "react-router-dom";

const AdminKYC = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();

  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    email: "phanthuthuongta.aec@gmail.com", // autofill
    phone: "",
    dob: "",
    role: "Admin", // fixed
    country: "",
    city: "",
  });

  const [errors, setErrors] = useState({});

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    if (errors[e.target.name]) {
      setErrors({ ...errors, [e.target.name]: "" }); // clear error when retyping
    }
  };

  // Custom English validation
  const validate = () => {
    const newErrors = {};
    Object.entries(form).forEach(([key, value]) => {
      if (!value && key !== "email") {
        newErrors[key] = "Please fill out this field.";
      }
    });
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!validate()) return;
    console.log("✅ KYC Data:", form);
    // TODO: Gửi API lưu KYC
    navigate("/admin/dashboard");
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
          width: "600px",
          borderRadius: "18px",
          backgroundColor: colors.primary[500],
        }}
      >
        <Typography
          variant="h4"
          fontWeight="bold"
          align="center"
          mb={2}
          color={colors.blueAccent[400]}
        >
          KYC Verification
        </Typography>

        <Typography
          variant="body2"
          align="center"
          mb={4}
          color={colors.grey[300]}
        >
          Please complete your personal and address information.
        </Typography>

        {/* 🧩 noValidate disables default browser popup */}
        <form onSubmit={handleSubmit} noValidate>
          {/* PERSONAL INFO */}
          <Typography
            variant="h6"
            fontWeight="bold"
            color={colors.greenAccent[400]}
            mb={1}
          >
            Personal Information
          </Typography>

          <Grid container spacing={2} mb={2}>
            <Grid item xs={6}>
              <TextField
                label="First Name"
                name="firstName"
                value={form.firstName}
                onChange={handleChange}
                fullWidth
                error={!!errors.firstName}
                helperText={errors.firstName}
                variant="filled"
                sx={{
                  backgroundColor: colors.primary[400],
                  borderRadius: "6px",
                }}
              />
            </Grid>

            <Grid item xs={6}>
              <TextField
                label="Last Name"
                name="lastName"
                value={form.lastName}
                onChange={handleChange}
                fullWidth
                error={!!errors.lastName}
                helperText={errors.lastName}
                variant="filled"
                sx={{
                  backgroundColor: colors.primary[400],
                  borderRadius: "6px",
                }}
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                label="Email"
                name="email"
                value={form.email}
                fullWidth
                disabled
                variant="filled"
                sx={{
                  backgroundColor: colors.primary[400],
                  borderRadius: "6px",
                }}
              />
            </Grid>

            <Grid item xs={6}>
              <TextField
                label="Phone"
                name="phone"
                type="tel"
                value={form.phone}
                onChange={handleChange}
                fullWidth
                error={!!errors.phone}
                helperText={errors.phone}
                variant="filled"
                sx={{
                  backgroundColor: colors.primary[400],
                  borderRadius: "6px",
                }}
              />
            </Grid>

            {/* Date of Birth + Role same row */}
            <Grid item xs={6}>
              <TextField
                label="Date of Birth"
                name="dob"
                type="date"
                value={form.dob}
                onChange={handleChange}
                fullWidth
                error={!!errors.dob}
                helperText={errors.dob}
                InputLabelProps={{ shrink: true }}
                variant="filled"
                sx={{
                  backgroundColor: colors.primary[400],
                  borderRadius: "6px",
                }}
              />
            </Grid>

            <Grid item xs={6}>
              <TextField
                label="Role"
                name="role"
                value={form.role}
                fullWidth
                disabled
                variant="filled"
                sx={{
                  backgroundColor: colors.primary[400],
                  borderRadius: "6px",
                }}
              />
            </Grid>
          </Grid>

          {/* ADDRESS */}
          <Typography
            variant="h6"
            fontWeight="bold"
            color={colors.greenAccent[400]}
            mb={1}
          >
            Address
          </Typography>

          <Grid container spacing={2} mb={2}>
            <Grid item xs={6}>
              <TextField
                label="Country"
                name="country"
                value={form.country}
                onChange={handleChange}
                fullWidth
                error={!!errors.country}
                helperText={errors.country}
                variant="filled"
                sx={{
                  backgroundColor: colors.primary[400],
                  borderRadius: "6px",
                }}
              />
            </Grid>

            <Grid item xs={6}>
              <TextField
                label="City"
                name="city"
                value={form.city}
                onChange={handleChange}
                fullWidth
                error={!!errors.city}
                helperText={errors.city}
                variant="filled"
                sx={{
                  backgroundColor: colors.primary[400],
                  borderRadius: "6px",
                }}
              />
            </Grid>
          </Grid>

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
            SUBMIT KYC
          </Button>
        </form>
      </Paper>
    </Box>
  );
};

export default AdminKYC;
