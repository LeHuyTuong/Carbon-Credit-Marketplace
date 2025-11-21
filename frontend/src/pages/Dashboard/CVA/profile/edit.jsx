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
import { tokens } from "@/themeCVA";
import Header from "@/components/Chart/Header.jsx";
import { useNavigate } from "react-router-dom";
import { updateKYCCVA, checkKYCCVA } from "@/apiCVA/apiAuthor.js";

const EditProfileCVA = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();

  const [cva, setCva] = useState({});
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: "",
    severity: "success",
  });

  //  Fetch data when component mounts 
  useEffect(() => {
    const fetchData = async () => {
      try {
        const res = await checkKYCCVA();
        if (res && Object.keys(res).length > 0) {
          setCva(res);
          localStorage.setItem("cvaData", JSON.stringify(res));
        }
      } catch (err) {
        console.error("Error fetching data:", err);
      }
    };
    fetchData();
  }, []);

  const handleSnackbarClose = () =>
    setSnackbar((prev) => ({ ...prev, open: false }));

  // Handle input changes
  const handleChange = (e) => {
    setCva({ ...cva, [e.target.name]: e.target.value });
  };

  // Upload new avatar 
  const handleImageUpload = (e) => {
    const file = e.target.files[0];
    if (file) {
      setCva((prev) => ({ ...prev, avatar: file, avatarUrl: URL.createObjectURL(file) }))
    }
  };

  //  Save profile 
  const handleSave = async () => {
  try {
    // Create form data
    const formData = new FormData();
    formData.append("name", cva.name);
    formData.append("email", cva.email);
    formData.append("organization", cva.organization);
    formData.append("positionTitle", cva.positionTitle);
    if (cva.avatar instanceof File) formData.append("avatar", cva.avatar);
    // Call API to update
    const updated = await updateKYCCVA(formData);
    console.log("Updated CVA:", updated);
    setSnackbar({ open: true, message: "Profile updated!", severity: "success" });
    setTimeout(() => navigate("/cva/view_profile_cva"), 1000);
  } catch (err) {
    setSnackbar({ open: true, message: err.message, severity: "error" });
  }
};


  // Render UI
  return (
    <Box m="20px" sx={{ marginLeft: "290px" }}>
      <Header title="EDIT PROFILE" subtitle="Update CVA Information" />

      <Snackbar
        open={snackbar.open}
        autoHideDuration={3000}
        onClose={handleSnackbarClose}
        anchorOrigin={{ vertical: "top", horizontal: "center" }}
      >
        <Alert
          severity={snackbar.severity}
          onClose={handleSnackbarClose}
          variant="filled"
        >
          {snackbar.message}
        </Alert>
      </Snackbar>

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

        {/* Avatar */}
        <Box display="flex" flexDirection="column" alignItems="center" mb={3} gap={1.5}>
          <Avatar
            src={
              cva.avatar instanceof File
                ? URL.createObjectURL(cva.avatar)
                : cva.avatarUrl || "/default_avatar.png"
            }
            alt="CVA Avatar"
            sx={{ width: 120, height: 120, border: `3px solid ${colors.greenAccent[400]}` }}
          />
          <Button
            variant="outlined"
            component="label"
            color="secondary"
            sx={{ fontSize: "0.85rem", textTransform: "none" }}
          >
            Upload New Avatar
            <input type="file" hidden accept="image/*" onChange={handleImageUpload} /> {/* Hidden file input */}
          </Button>
        </Box>

        {/* Form */}
        <Box display="grid" gap={2}>
          <TextField name="name" label="Full Name" value={cva.name || ""} onChange={handleChange} fullWidth />
          <TextField name="email" label="Email" value={cva.email || ""} onChange={handleChange} fullWidth InputProps={{ readOnly: true }} />
          <TextField name="organization" label="Organization" value={cva.organization || ""} onChange={handleChange} fullWidth />
          <TextField name="positionTitle" label="Position Title" value={cva.positionTitle || ""} onChange={handleChange} fullWidth InputProps={{ readOnly: true }} />
        </Box>

        {/* Buttons */}
        <Box mt={4} display="flex" justifyContent="flex-end" gap={2}>
          <Button variant="outlined" color="secondary" onClick={() => navigate("/cva/view_profile_cva")}>
            Cancel
          </Button>
          <Button variant="contained" color="success" onClick={handleSave}>
            Save
          </Button>
        </Box>
      </Paper>
    </Box>
  );
};

export default EditProfileCVA;
