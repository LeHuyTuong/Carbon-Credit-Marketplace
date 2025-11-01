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
    name: "",
    email: localStorage.getItem("cva_email") || "",
    role: "CVA",
    organization: "",
    accreditationNo: "",
    capacityQuota: "",
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
      if (!value && key !== "email" && key !== "role") {
        newErrors[key] = "Please fill out this field.";
      }
    });
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
  e.preventDefault();
  if (!validate()) return;

  try {
    setLoading(true);

    // Tạo payload đúng format JSON
    const payload = {
      requestTrace: crypto.randomUUID(),
      requestDateTime: new Date().toISOString(),
      data: {
        name: form.name,
        email: form.email,
        organization: form.organization,
        positionTitle: form.role, // hoặc có thể thêm field riêng nếu BE yêu cầu
        accreditationNo: form.accreditationNo,
        capacityQuota: Number(form.capacityQuota) || 0,
        notes: "",
      },
    };

    await apiKYCCVA(payload);

    alert("KYC submitted successfully!");
    navigate("/cva/dashboard");
  } catch (error) {
    alert("KYC submission failed: " + error.message);
    console.error("KYC Error:", error);
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

        <Typography variant="body2" align="center" mb={4} color={colors.grey[300]}>
          Please complete your personal and organizational information.
        </Typography>

        <form onSubmit={handleSubmit} noValidate>
          <Grid container spacing={2} mb={2}>
            {/* Left Column */}
            <Grid item xs={6}>
              <TextField
                label="Full Name"
                name="name"
                value={form.name}
                onChange={handleChange}
                fullWidth
                error={!!errors.name}
                helperText={errors.name}
                variant="filled"
                sx={{
                  mb: 2,
                  backgroundColor: colors.primary[400],
                  borderRadius: "6px",
                }}
              />
              <TextField
                label="Organization"
                name="organization"
                value={form.organization}
                onChange={handleChange}
                fullWidth
                error={!!errors.organization}
                helperText={errors.organization}
                variant="filled"
                sx={{
                  mb: 2,
                  backgroundColor: colors.primary[400],
                  borderRadius: "6px",
                }}
              />
              <TextField
                label="Accreditation No"
                name="accreditationNo"
                value={form.accreditationNo}
                onChange={handleChange}
                fullWidth
                error={!!errors.accreditationNo}
                helperText={errors.accreditationNo}
                variant="filled"
                sx={{
                  backgroundColor: colors.primary[400],
                  borderRadius: "6px",
                }}
              />
            </Grid>

            {/* Right Column */}
            <Grid item xs={6}>
              <TextField
                label="Email"
                name="email"
                value={form.email}
                disabled
                fullWidth
                variant="filled"
                sx={{
                  mb: 2,
                  backgroundColor: colors.primary[400],
                  borderRadius: "6px",
                }}
              />
              <TextField
                label="Role"
                name="role"
                value={form.role}
                disabled
                fullWidth
                variant="filled"
                sx={{
                  mb: 2,
                  backgroundColor: colors.primary[400],
                  borderRadius: "6px",
                }}
              />
              <TextField
                label="Capacity Quota"
                name="capacityQuota"
                value={form.capacityQuota}
                onChange={handleChange}
                fullWidth
                error={!!errors.capacityQuota}
                helperText={errors.capacityQuota}
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

          <Button
            fullWidth
            variant="outlined"
            sx={{
              mt: 2,
              py: 1.2,
              color: colors.greenAccent[400],
              borderColor: colors.grey[400],
              fontWeight: "bold",
              "&:hover": { borderColor: colors.grey[200] },
            }}
            onClick={() => navigate(-1)}
          >
            BACK
          </Button>
        </form>
      </Paper>
    </Box>
  );
};

export default CVAKYC;
