import {
  Box,
  Typography,
  Avatar,
  Grid,
  Paper,
  TextField,
  Button,
  Divider,
  useTheme,
  Snackbar,
  Alert,
} from "@mui/material";
import { useNavigate } from "react-router-dom";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { useState, useEffect } from "react";
import {
  getKycProfileCVA,
  updateKycProfileCVA,
} from "@/apiAdmin/CVAAdmin.js";

const ViewUserCVA = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();

  const [user, setUser] = useState(null);
  const [editedUser, setEditedUser] = useState(null);
  const [editMode, setEditMode] = useState(false);
  const [openSnackbar, setOpenSnackbar] = useState(false);
  const [loading, setLoading] = useState(true);

  const formatDate = (iso) => {
    if (!iso) return "";
    try {
      return new Date(iso).toLocaleString();
    } catch {
      return iso;
    }
  };

  useEffect(() => {
    async function fetchUser() {
      setLoading(true);
      try {
        const res = await getKycProfileCVA();
        const data = res?.response;
        if (!data) throw new Error("No CVA KYC data found");

        const uiUser = {
          id: data.id ?? "",
          name: data.name || "",
          email: data.email || "",
          organization: data.organization || "",
          positionTitle: data.positionTitle || "",
          accreditationNo: data.accreditationNo || "",
          capacityQuota: data.capacityQuota ?? 0,
          notes: data.notes || "",
          status: data.status || "ACTIVE",
          createdAt: data.createdAt || "",
          updatedAt: data.updatedAt || "",
        };

        setUser(uiUser);
        setEditedUser(uiUser);
      } catch (error) {
        console.error("Error fetching KYC profile:", error);
      } finally {
        setLoading(false);
      }
    }

    fetchUser();
  }, []);

  const handleChange = (field, value) => {
    setEditedUser((prev) => ({ ...prev, [field]: value }));
  };

  const handleUpdate = async () => {
    try {
      const payload = {
        name: editedUser.name,
        email: editedUser.email,
        organization: editedUser.organization,
        positionTitle: editedUser.positionTitle,
        accreditationNo: editedUser.accreditationNo,
        capacityQuota: Number(editedUser.capacityQuota) || 0,
        notes: editedUser.notes,
      };

      await updateKycProfileCVA(payload);
      setUser(editedUser);
      setEditMode(false);
      setOpenSnackbar(true);
    } catch (error) {
      console.error("Error updating CVA KYC profile:", error);
      alert("Update failed! Please try again.");
    }
  };

  if (loading) return <Typography>Loading...</Typography>;
  if (!user) return <Typography>No CVA KYC profile found.</Typography>;

  return (
    <Box m="20px">
      <Header
        title="CVA KYC PROFILE"
        subtitle="View or update your KYC information"
      />
      <Paper
        elevation={2}
        sx={{
          p: 4,
          borderRadius: 3,
          backgroundColor: colors.primary[400],
          color: colors.grey[100],
        }}
      >
        <Box
          display="flex"
          justifyContent="space-between"
          alignItems="center"
          mb={3}
        >
          <Typography variant="h5" fontWeight="bold">
            My KYC Profile
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
                sx={{ textTransform: "none" }}
              >
                Update
              </Button>
              <Button
                variant="outlined"
                color="inherit"
                onClick={() => {
                  setEditedUser(user);
                  setEditMode(false);
                }}
                sx={{ textTransform: "none" }}
              >
                Cancel
              </Button>
            </Box>
          )}
        </Box>

        {/* Avatar */}
        <Box display="flex" alignItems="center" mb={3}>
          <Avatar
            src={`https://i.pravatar.cc/100?u=${user.id || "cva"}`}
            alt={user.name || "CVA user"}
            sx={{ width: 80, height: 80, mr: 3 }}
          />
          <Box>
            <Typography variant="h6" fontWeight="bold">
              {user.name || "(No name)"}
            </Typography>
            <Typography variant="body2" color={colors.grey[300]}>
              {user.email}
            </Typography>
          </Box>
        </Box>

        <Divider sx={{ mb: 3, borderColor: colors.grey[700] }} />

        {/* KYC Details */}
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <TextField
              label="Full Name"
              fullWidth
              value={editedUser.name || ""}
              InputProps={{ readOnly: !editMode }}
              onChange={(e) => handleChange("name", e.target.value)}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Email"
              fullWidth
              value={editedUser.email || ""}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Organization"
              fullWidth
              value={editedUser.organization || ""}
              InputProps={{ readOnly: !editMode }}
              onChange={(e) => handleChange("organization", e.target.value)}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Position Title"
              fullWidth
              value={editedUser.positionTitle || ""}
              InputProps={{ readOnly: !editMode }}
              onChange={(e) => handleChange("positionTitle", e.target.value)}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Accreditation No"
              fullWidth
              value={editedUser.accreditationNo || ""}
              InputProps={{ readOnly: !editMode }}
              onChange={(e) => handleChange("accreditationNo", e.target.value)}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Capacity Quota"
              type="number"
              fullWidth
              value={editedUser.capacityQuota || 0}
              InputProps={{ readOnly: !editMode }}
              onChange={(e) => handleChange("capacityQuota", e.target.value)}
            />
          </Grid>

          <Grid item xs={12}>
            <TextField
              label="Notes"
              fullWidth
              multiline
              minRows={3}
              value={editedUser.notes || ""}
              InputProps={{ readOnly: !editMode }}
              onChange={(e) => handleChange("notes", e.target.value)}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Created At"
              fullWidth
              value={formatDate(editedUser.createdAt)}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Updated At"
              fullWidth
              value={formatDate(editedUser.updatedAt)}
              InputProps={{ readOnly: true }}
            />
          </Grid>
        </Grid>

        <Box display="flex" justifyContent="flex-end" mt={4}>
          <Button
            variant="outlined"
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate("/admin/cva_management")}
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
          CVA KYC profile updated successfully!
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ViewUserCVA;
