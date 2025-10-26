import {
  Box,
  Typography,
  Paper,
  TextField,
  Button,
  Divider,
  Snackbar,
  Alert,
  useTheme,
  CircularProgress,
} from "@mui/material";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { useState, useEffect } from "react";
import {
  getCompanyKYCProfile,
  updateCompanyKYCProfile,
} from "@/apiAdmin/companiesAdmin.js";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import { useNavigate } from "react-router-dom";

const ViewCompanyKYC = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();

  const [profile, setProfile] = useState(null);
  const [editMode, setEditMode] = useState(false);
  const [openSnackbar, setOpenSnackbar] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    async function fetchProfile() {
      setLoading(true);
      try {
        const res = await getCompanyKYCProfile();
        const data = res?.response;
        if (data) {
          setProfile(data);
        }
      } catch (error) {
        console.error("❌ Error fetching company KYC profile:", error);
      } finally {
        setLoading(false);
      }
    }

    fetchProfile();
  }, []);

  const handleChange = (field, value) => {
    setProfile((prev) => ({ ...prev, [field]: value }));
  };

  const handleUpdate = async () => {
    setSaving(true);
    try {
      const payload = {
        businessLicense: profile.businessLicense,
        taxCode: profile.taxCode,
        companyName: profile.companyName,
        address: profile.address,
      };
      await updateCompanyKYCProfile(payload);
      setOpenSnackbar(true);
      setEditMode(false);
    } catch (err) {
      console.error("❌ Error updating KYC:", err);
      alert("Update failed. Please try again.");
    } finally {
      setSaving(false);
    }
  };

  if (loading)
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height="60vh">
        <CircularProgress />
      </Box>
    );

  if (!profile) return <Typography>No company KYC data found.</Typography>;

  return (
    <Box m="20px">
      <Header title="COMPANY KYC PROFILE" subtitle="View or edit your company verification information" />

      <Paper
        elevation={2}
        sx={{
          p: 4,
          borderRadius: 3,
          backgroundColor: colors.primary[400],
          color: colors.grey[100],
        }}
      >
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
          <Typography variant="h5" fontWeight="bold">
            Company Information
          </Typography>

          {!editMode ? (
            <Button
              variant="contained"
              startIcon={<EditOutlinedIcon />}
              onClick={() => setEditMode(true)}
              sx={{
                backgroundColor: "#3b82f6",
                textTransform: "none",
                "&:hover": { backgroundColor: "#2563eb" },
              }}
            >
              Edit
            </Button>
          ) : (
            <Box display="flex" gap={2}>
              <Button
                variant="contained"
                color="success"
                onClick={handleUpdate}
                disabled={saving}
                sx={{ textTransform: "none" }}
              >
                {saving ? "Saving..." : "Update"}
              </Button>
              <Button
                variant="outlined"
                color="inherit"
                onClick={() => setEditMode(false)}
                sx={{ textTransform: "none" }}
              >
                Cancel
              </Button>
            </Box>
          )}
        </Box>

        <Divider sx={{ mb: 3, borderColor: colors.grey[700] }} />

        {/* FORM FIELDS */}
        <Box display="grid" gridTemplateColumns="repeat(2, 1fr)" gap={3}>
          <TextField
            label="Company Name"
            fullWidth
            value={profile.companyName || ""}
            InputProps={{ readOnly: !editMode }}
            onChange={(e) => handleChange("companyName", e.target.value)}
          />
          <TextField
            label="Business License"
            fullWidth
            value={profile.businessLicense || ""}
            InputProps={{ readOnly: !editMode }}
            onChange={(e) => handleChange("businessLicense", e.target.value)}
          />
          <TextField
            label="Tax Code"
            fullWidth
            value={profile.taxCode || ""}
            InputProps={{ readOnly: !editMode }}
            onChange={(e) => handleChange("taxCode", e.target.value)}
          />
          <TextField
            label="Address"
            fullWidth
            value={profile.address || ""}
            InputProps={{ readOnly: !editMode }}
            onChange={(e) => handleChange("address", e.target.value)}
          />
          <TextField
            label="Created At"
            fullWidth
            value={new Date(profile.createAt).toLocaleString() || ""}
            InputProps={{ readOnly: true }}
          />
          <TextField
            label="Updated At"
            fullWidth
            value={new Date(profile.updatedAt).toLocaleString() || ""}
            InputProps={{ readOnly: true }}
          />
        </Box>

        {/* BACK BUTTON */}
        <Box display="flex" justifyContent="flex-end" mt={4}>
          <Button
            variant="outlined"
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate("/admin/companies_management")}
            sx={{
              borderColor: colors.blueAccent[400],
              color: colors.blueAccent[400],
              textTransform: "none",
            }}
          >
            Back
          </Button>
        </Box>
      </Paper>

      {/* Snackbar on top */}
      <Snackbar
        open={openSnackbar}
        autoHideDuration={3000}
        onClose={() => setOpenSnackbar(false)}
        anchorOrigin={{ vertical: "top", horizontal: "center" }}
      >
        <Alert
          onClose={() => setOpenSnackbar(false)}
          severity="success"
          variant="filled"
          sx={{ width: "100%" }}
        >
          Company KYC updated successfully!
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ViewCompanyKYC;
