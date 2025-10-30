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
import { apiKYCCVA } from "@/apiCVA/apiAuthor.js"; 

const CVAKYC = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();

  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    email: localStorage.getItem("cva_email") || "",
    phone: "",
    dob: "",
    role: "CVA",
    country: "",
    city: "",
  });

  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    if (errors[e.target.name]) setErrors({ ...errors, [e.target.name]: "" });
  };

  const validate = () => {
    const newErrors = {};
    Object.entries(form).forEach(([key, value]) => {
      if (!value && key !== "email") newErrors[key] = "Please fill out this field.";
    });
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    try {
      setLoading(true);
      const formData = new FormData();
      Object.entries(form).forEach(([key, value]) => formData.append(key, value));

      const response = await apiKYCCVA(formData);
      alert("KYC submitted successfully!");
      navigate("/cva/dashboard");
    } catch (error) {
      alert("KYC submission failed: " + error.message);
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
          color={colors.greenAccent[400]}
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

        <form onSubmit={handleSubmit} noValidate>
          <Typography
            variant="h6"
            fontWeight="bold"
            color={colors.greenAccent[400]}
            mb={1}
          >
            Personal Information
          </Typography>

          <Grid container spacing={2} mb={2}>
            {[
              { name: "firstName", label: "First Name" },
              { name: "lastName", label: "Last Name" },
              { name: "email", label: "Email", disabled: true },
              { name: "phone", label: "Phone", type: "tel" },
              { name: "dob", label: "Date of Birth", type: "date" },
              { name: "role", label: "Role", disabled: true },
              { name: "country", label: "Country" },
              { name: "city", label: "City" },
            ].map((field) => (
              <Grid
                item
                xs={field.name === "email" || field.name === "country" || field.name === "city" ? 12 : 6}
                key={field.name}
              >
                <TextField
                  label={field.label}
                  name={field.name}
                  type={field.type || "text"}
                  value={form[field.name]}
                  onChange={handleChange}
                  fullWidth
                  disabled={field.disabled}
                  error={!!errors[field.name]}
                  helperText={errors[field.name]}
                  InputLabelProps={field.type === "date" ? { shrink: true } : undefined}
                  variant="filled"
                  sx={{
                    backgroundColor: colors.primary[400],
                    borderRadius: "6px",
                  }}
                />
              </Grid>
            ))}
          </Grid>

          <Button
            type="submit"
            fullWidth
            disabled={loading}
            variant="contained"
            sx={{
              mt: 1,
              py: 1.2,
              backgroundColor: colors.greenAccent[600],
              color: colors.grey[900],
              fontWeight: "bold",
              "&:hover": { backgroundColor: colors.greenAccent[700] },
            }}
          >
            {loading ? "Submitting..." : "SUBMIT KYC"}
          </Button>
        </form>
      </Paper>
    </Box>
  );
};

export default CVAKYC;
