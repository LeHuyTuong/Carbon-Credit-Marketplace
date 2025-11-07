import React, { useState, useEffect } from "react";
import {
  Box,
  TextField,
  Button,
  Paper,
  Typography,
  Avatar,
  useTheme,
  Snackbar,
  Alert,
} from "@mui/material";
import { LocalizationProvider, DatePicker } from "@mui/x-date-pickers";
import { AdapterDayjs } from "@mui/x-date-pickers/AdapterDayjs";
import dayjs from "dayjs";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { useNavigate } from "react-router-dom";
import { checkKYCAdmin, updateKYCAdmin } from "@/apiAdmin/apiLogin.js";

const EditProfile = ({ onAvatarUpdated }) => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();

  const [admin, setAdmin] = useState({
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    country: "",
    city: "",
    birthday: "",
    avatarUrl: null,
  });

  const [avatarFile, setAvatarFile] = useState(null);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: "",
    severity: "success", // success | error | warning | info
  });

  // Load admin KYC data
  useEffect(() => {
    const fetchAdminData = async () => {
      try {
        const res = await checkKYCAdmin();
        if (res) {
          setAdmin({
            firstName: res.firstName || res.name || "",
            lastName: res.lastName || "",
            email: res.email || "",
            phone: res.phone || "",
            country: res.country || "Viet Nam",
            city: res.city || "Ho Chi Minh",
            birthday: res.birthday || "",
            avatarUrl: res.avatarUrl || null,
          });
        }
      } catch (error) {
        console.error("Failed to fetch KYC admin data:", error.message);
      }
    };

    fetchAdminData();
  }, []);

  // Handle text field changes
  const handleChange = (e) => {
    setAdmin({ ...admin, [e.target.name]: e.target.value });
  };

  // Handle image upload
  const handleImageUpload = (e) => {
    const file = e.target.files[0];
    if (file) {
      setAvatarFile(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setAdmin((prev) => ({ ...prev, avatarUrl: reader.result }));
      };
      reader.readAsDataURL(file);
    }
  };

  // Handle save
  const handleSave = async () => {
    try {
      const formData = new FormData();
      formData.append("firstName", admin.firstName);
      formData.append("lastName", admin.lastName);
      formData.append("email", admin.email);
      formData.append("phone", admin.phone);
      formData.append("country", admin.country);
      formData.append("city", admin.city);
      formData.append("birthday", admin.birthday || "");
      formData.append("name", `${admin.firstName} ${admin.lastName}`.trim());

      if (avatarFile) {
        formData.append("avatar", avatarFile);
      }

      await updateKYCAdmin(formData);
      localStorage.setItem("avatar_updated", Date.now());
      window.dispatchEvent(new Event("avatar_updated"));
      if (onAvatarUpdated) onAvatarUpdated();
       
      setSnackbar({
        open: true,
        message: "Admin profile updated successfully!",
        severity: "success",
      });

      setTimeout(() => navigate("/admin/view_profile_admin"), 1500);
    } catch (error) {
      console.error("Update KYC API Error:", error.message);
      setSnackbar({
        open: true,
        message: "Update failed. Please try again.",
        severity: "error",
      });
    }
  };

  return (
    <Box m="20px" sx={{ marginLeft: "290px" }}>
      <Header title="EDIT PROFILE" subtitle="Update Administrator Information" />

      <Paper
        sx={{
          p: 4,
          borderRadius: "16px",
          backgroundColor: colors.primary[400],
        }}
      >
        <Typography
          variant="h5"
          fontWeight="bold"
          mb={3}
          color={colors.greenAccent[400]}
        >
          Edit Information
        </Typography>

        {/* Avatar Upload */}
        <Box
          display="flex"
          flexDirection="column"
          alignItems="center"
          mb={3}
          gap={1.5}
        >
          <Avatar
            src={admin.avatarUrl}
            alt="Admin Avatar"
            sx={{
              width: 120,
              height: 120,
              border: `3px solid ${colors.greenAccent[400]}`,
              bgcolor: colors.grey[700],
            }}
          >
            {!admin.avatarUrl && admin.firstName[0]}
          </Avatar>
          <Button
            variant="outlined"
            component="label"
            color="secondary"
            sx={{ fontSize: "0.85rem", textTransform: "none" }}
          >
            Upload New Avatar
            <input
              type="file"
              hidden
              accept="image/*"
              onChange={handleImageUpload}
            />
          </Button>
        </Box>

        {/* Text Fields */}
        <Box display="grid" gap={2}>
          <TextField
            name="firstName"
            label="First Name"
            value={admin.firstName}
            onChange={handleChange}
            fullWidth
          />
          <TextField
            name="lastName"
            label="Last Name"
            value={admin.lastName}
            onChange={handleChange}
            fullWidth
          />
          <TextField
            name="email"
            label="Email"
            value={admin.email}
            onChange={handleChange}
            fullWidth
            InputProps={{
              readOnly: true,
            }}
          />
          <TextField
            name="phone"
            label="Phone Number"
            value={admin.phone}
            onChange={handleChange}
            fullWidth
          />
          <TextField
            name="country"
            label="Country"
            value={admin.country}
            onChange={handleChange}
            fullWidth
          />
          <TextField
            name="city"
            label="City"
            value={admin.city}
            onChange={handleChange}
            fullWidth
          />

          {/*  Date Picker */}
          <LocalizationProvider dateAdapter={AdapterDayjs}>
            <DatePicker
              label="Date of Birth"
              value={admin.birthday ? dayjs(admin.birthday) : null}
              maxDate={dayjs()} // Chỉ cho chọn ngày trong quá khứ
              onChange={(newValue) => {
                setAdmin({
                  ...admin,
                  birthday: newValue ? newValue.format("YYYY-MM-DD") : "",
                });
              }}
              slotProps={{
                textField: {
                  fullWidth: true,
                },
              }}
            />
          </LocalizationProvider>
        </Box>

        {/* Buttons */}
        <Box mt={4} display="flex" justifyContent="flex-end" gap={2}>
          <Button
            variant="outlined"
            color="secondary"
            onClick={() => navigate("/admin/view_profile_admin")}
          >
            Cancel
          </Button>
          <Button variant="contained" color="success" onClick={handleSave}>
            Save
          </Button>
        </Box>
      </Paper>

      {/*  Snackbar thông báo */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={2500}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: "top", horizontal: "center" }}
      >
        <Alert
          onClose={() => setSnackbar({ ...snackbar, open: false })}
          severity={snackbar.severity}
          variant="filled"
          sx={{ width: "100%" }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default EditProfile;
