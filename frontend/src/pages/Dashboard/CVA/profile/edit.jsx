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

  // ===== Fetch data from API when page loads =====
  useEffect(() => {
    const fetchData = async () => {
      try {
        const res = await checkKYCCVA();
        console.log("Fetched KYC data:", res);

        if (res && Object.keys(res).length > 0) {
          setCva(res);
          localStorage.setItem("cvaData", JSON.stringify(res));
          console.log("Saved data to localStorage:", res);
        } else {
          console.warn("API returned empty or invalid data:", res);
        }
      } catch (err) {
        console.error("Error fetching data:", err);
      }
    };
    fetchData();
  }, []);

  const handleSnackbarClose = () =>
    setSnackbar((prev) => ({ ...prev, open: false }));

  // ===== Handle input changes =====
  const handleChange = (e) => {
    setCva({ ...cva, [e.target.name]: e.target.value });
  };

  // ===== Upload new avatar =====
  const handleImageUpload = (e) => {
    const file = e.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setCva((prev) => ({ ...prev, avatar: reader.result }));
      };
      reader.readAsDataURL(file);
    }
  };

  // ===== Save profile information =====
  const handleSave = async () => {
    try {
      const payload = {
        name: cva.name || "CVA User",
        email: cva.email || "",
        organization: cva.organization || "CVA_VN",
        positionTitle: cva.positionTitle || "CVA",
        accreditationNo: cva.accreditationNo || "",
        notes: cva.notes || "Updated via frontend",
        capacityQuota: cva.capacityQuota || 1111,
        avatar: cva.avatar || null,
      };

      const requestBody = { data: payload };

      console.log("Sending to API:", requestBody);

      const response = await updateKYCCVA(requestBody);
      console.log("KYC update successful:", response);

      localStorage.setItem("cvaData", JSON.stringify(payload));

      setSnackbar({
        open: true,
        message: "Profile updated successfully!",
        severity: "success",
      });

      setTimeout(() => navigate("/cva/view_profile_cva"), 1000);
    } catch (error) {
      console.error("KYC update failed:", error);
      setSnackbar({
        open: true,
        message: `Update failed: ${error.message}`,
        severity: "error",
      });
    }
  };

  // ===== UI =====
  return (
    <Box m="20px">
      <Header title="EDIT PROFILE" subtitle="Update CVA Information" />

      {/* ===== Snackbar moved to TOP ===== */}
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

        {/* ===== Avatar ===== */}
        <Box
          display="flex"
          flexDirection="column"
          alignItems="center"
          mb={3}
          gap={1.5}
        >
          <Avatar
            src={cva.avatar}
            alt="CVA Avatar"
            sx={{
              width: 120,
              height: 120,
              border: `3px solid ${colors.greenAccent[400]}`,
            }}
          />
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

        {/* ===== Form ===== */}
        <Box display="grid" gap={2}>
          <TextField
            name="name"
            label="Full Name"
            value={cva.name || ""}
            onChange={handleChange}
            fullWidth
          />
          <TextField
            name="email"
            label="Email"
            value={cva.email || ""}
            onChange={handleChange}
            fullWidth
          />
          <TextField
            name="organization"
            label="Organization"
            value={cva.organization || ""}
            onChange={handleChange}
            fullWidth
          />
          <TextField
            name="positionTitle"
            label="Position Title"
            value={cva.positionTitle || ""}
            onChange={handleChange}
            fullWidth
          />
          <TextField
            name="accreditationNo"
            label="Accreditation No."
            value={cva.accreditationNo || ""}
            onChange={handleChange}
            fullWidth
          />
          <TextField
            name="capacityQuota"
            label="Capacity Quota"
            type="number"
            value={cva.capacityQuota || ""}
            onChange={handleChange}
            fullWidth
          />
          <TextField
            name="notes"
            label="Notes"
            multiline
            rows={3}
            value={cva.notes || ""}
            onChange={handleChange}
            fullWidth
          />
        </Box>

        {/* ===== Buttons ===== */}
        <Box mt={4} display="flex" justifyContent="flex-end" gap={2}>
          <Button
            variant="outlined"
            color="secondary"
            onClick={() => navigate("/cva/view_profile_cva")}
          >
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
