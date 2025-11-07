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
import { apiKYCAdmin } from "@/apiAdmin/apiLogin.js"; //  Gọi API KYC
import { useSnackbar } from "@/hooks/useSnackbar.jsx"; //component snackbar to, rõ, dễ nhìn

const AdminKYC = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();

  const savedEmail =
    sessionStorage.getItem("admin_email") || localStorage.getItem("admin_email") || "";

  const { showSnackbar, SnackbarComponent } = useSnackbar();

  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    email: savedEmail, // tự động điền từ login
    phone: "",
    dob: "",
    role: "Admin",
    country: "",
    city: "",
  });


  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    if (errors[e.target.name]) {
      setErrors({ ...errors, [e.target.name]: "" }); // clear error when retyping
    }
  };

  //  Custom English validation
    const validate = () => {
    const newErrors = {};
    Object.entries(form).forEach(([key, value]) => {
      if (!value && key !== "email" && key !== "role") {
        newErrors[key] = "Please fill out this field.";
      }
    });

    // Kiểm tra định dạng ngày sinh dd/mm/yyyy
    if (form.dob) {
      const dateRegex = /^(0[1-9]|[12][0-9]|3[01])\/(0[1-9]|1[0-2])\/\d{4}$/;
      if (!dateRegex.test(form.dob)) {
        newErrors.dob = "Date of Birth must be in dd/mm/yyyy format.";
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };


  //  Handle Submit
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    try {
      setLoading(true);

      const formData = new FormData();
      formData.append("name", `${form.firstName} ${form.lastName}`.trim());

      Object.entries(form).forEach(([key, value]) => {
        if (key !== "name") formData.append(key, value);
      });

      const res = await apiKYCAdmin(formData);
      console.log(" Full KYC response:", res);

      //  Kiểm tra mã phản hồi thành công từ backend
      if (
        res?.responseStatus?.responseCode === "00000000" ||
        res?.responseStatus?.responseMessage?.toLowerCase().includes("success")
      ) {
        showSnackbar("success", "KYC submitted successfully!", 5000);
        setTimeout(() => navigate("/admin/dashboard"), 1500); // delay để snackbar hiển thị
      } else {
        console.error(" KYC failed:", res?.responseStatus?.responseMessage);
        showSnackbar("error", `KYC failed: ${res?.responseStatus?.responseMessage}`, 5000);
      }
    } catch (err) {
      console.error(" KYC Error:", err.message);
      showSnackbar("error", `KYC submission failed: ${err.message}`, 5000);
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

          <Grid container spacing={13} mb={2}>
            {/* Cột trái */}
            <Grid item xs={6}>
              <Box display="flex" flexDirection="column" gap={2}>
                <TextField
                  label="First Name"
                  name="firstName"
                  value={form.firstName}
                  onChange={handleChange}
                  fullWidth
                  error={!!errors.firstName}
                  helperText={errors.firstName}
                  variant="filled"
                  sx={{ backgroundColor: colors.primary[400], borderRadius: "6px" }}
                />
                <TextField
                  label="Last Name"
                  name="lastName"
                  value={form.lastName}
                  onChange={handleChange}
                  fullWidth
                  error={!!errors.lastName}
                  helperText={errors.lastName}
                  variant="filled"
                  sx={{ backgroundColor: colors.primary[400], borderRadius: "6px" }}
                />
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
                  sx={{ backgroundColor: colors.primary[400], borderRadius: "6px" }}
                />
              </Box>
            </Grid>

            {/* Cột phải */}
            <Grid item xs={6}>
              <Box display="flex" flexDirection="column" gap={2}>
                <TextField
                  label="Email"
                  name="email"
                  value={form.email}
                  fullWidth
                  disabled
                  variant="filled"
                  sx={{ backgroundColor: colors.primary[400], borderRadius: "6px" }}
                />
                <TextField
                  label="Date of Birth"
                  name="dob"
                  placeholder="dd/mm/yyyy"
                  value={form.dob}
                  onChange={handleChange}
                  fullWidth
                  error={!!errors.dob}
                  helperText={errors.dob}
                  variant="filled"
                  sx={{ backgroundColor: colors.primary[400], borderRadius: "6px" }}
                />

                <TextField
                  label="Role"
                  name="role"
                  value={form.role}
                  fullWidth
                  disabled
                  variant="filled"
                  sx={{ backgroundColor: colors.primary[400], borderRadius: "6px" }}
                />
              </Box>
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

          <Grid container spacing={13} mb={2}>
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
                sx={{ backgroundColor: colors.primary[400], borderRadius: "6px" }}
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
                sx={{ backgroundColor: colors.primary[400], borderRadius: "6px" }}
              />
            </Grid>
          </Grid>

          {/* Buttons */}
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
            {loading ? "Submitting..." : "SUBMIT KYC"}
          </Button>

          <Button
            fullWidth
            variant="text"
            onClick={() => navigate("/admin/carbonX/mkp/login",{ replace: true })}
            sx={{
              mt: 1.5,
              color: colors.blueAccent[400],
              "&:hover": { color: colors.blueAccent[300] },
            }}
          >
            Back
          </Button>
        </form>
      </Paper>
      {/* Snackbar */}
      {SnackbarComponent}
    </Box>
  );
};

export default AdminKYC;
