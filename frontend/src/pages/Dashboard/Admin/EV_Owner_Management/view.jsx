// ===================== ViewEvOwner.jsx =====================
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
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import { useParams, useNavigate } from "react-router-dom";
import { useState, useEffect, useMemo } from "react";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { getUserByEmail, updateUser } from "@/apiAdmin/userAdmin.js";

const ViewEvOwner = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();
  const { email } = useParams();

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

  // ===================== FETCH USER =====================
  useEffect(() => {
    async function fetchUser() {
      setLoading(true);
      try {
        const res = await getUserByEmail(email);
        const data = res?.responseData;
        if (!data) throw new Error("No user data found in responseData");

        const uiUser = {
          id: data.id ?? "",
          name: data.name || data.fullName || "",
          email: data.email || "",
          phone: data.phone || "",
          status: data.status || "ACTIVE",
          createdAt: data.createdAt || data.createAt || "",
          country: data.country || "",
          city: data.city || "",
          access: "ev_owner",
        };

        setUser(uiUser);
        setEditedUser(uiUser);
      } catch (error) {
        console.error("Error fetching user:", error);
      } finally {
        setLoading(false);
      }
    }

    fetchUser();
  }, [email]);

  const handleChange = (field, value) => {
    setEditedUser((prev) => ({ ...prev, [field]: value }));
  };

  const validateBeforeUpdate = useMemo(() => {
    return (u) => {
      if (!u) return "User is empty";
      if (!u.name?.trim()) return "Full Name is required";
      if (!u.email?.trim()) return "Email is required";
      if (!["ACTIVE", "INACTIVE"].includes(u.status)) return "Invalid status";
      return null;
    };
  }, []);

  // ===================== HANDLE UPDATE =====================
  const handleUpdate = async () => {
    const err = validateBeforeUpdate(editedUser);
    if (err) {
      alert(err);
      return;
    }

    const payload = {
      id: editedUser.id,
      email: editedUser.email,
      name: editedUser.name,
      phone: editedUser.phone || null,
      status: editedUser.status,
      roles: [{ name: "EV_OWNER" }],
      country: editedUser.country || null,
      city: editedUser.city || null,
    };

    try {
      await updateUser(editedUser.id, payload);
      setUser(editedUser);
      setEditMode(false);
      setOpenSnackbar(true);
    } catch (error) {
      console.error("Error updating user:", error);
      alert("Update failed! Please try again.");
    }
  };

  if (loading) return <Typography>Loading...</Typography>;
  if (!user) return <Typography>User not found</Typography>;

  // ===================== UI =====================
  return (
    <Box m="20px">
      <Header title="EV OWNER DETAILS" subtitle="View or edit your profile information" />

      <Paper
        elevation={2}
        sx={{
          p: 4,
          borderRadius: 3,
          backgroundColor: colors.primary[400],
          color: colors.grey[100],
        }}
      >
        {/* HEADER BUTTONS */}
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
          <Typography variant="h5" fontWeight="bold">
            Profile Information
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

        {/* AVATAR */}
        <Box display="flex" alignItems="center" mb={3}>
          <Avatar
            src={`https://i.pravatar.cc/100?u=${user.id || "na"}`}
            alt={user.name || "user"}
            sx={{ width: 80, height: 80, mr: 3 }}
          />
          <Box>
            <Typography variant="h6" fontWeight="bold">
              {user.name || "(No name)"}
            </Typography>
            <Typography variant="body2" color={colors.grey[300]}>
              EV Owner
            </Typography>
          </Box>
        </Box>

        <Divider sx={{ mb: 3, borderColor: colors.grey[700] }} />

        {/* PERSONAL DETAILS */}
        <Typography variant="h6" fontWeight="bold" mb={2}>
          Personal Details
        </Typography>

        <Grid container spacing={3} mb={4}>
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
              label="Email Address"
              fullWidth
              value={editedUser.email || ""}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Phone"
              fullWidth
              value={editedUser.phone || ""}
              InputProps={{ readOnly: !editMode }}
              onChange={(e) => handleChange("phone", e.target.value)}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            {editMode ? (
              <TextField
                select
                label="Status"
                fullWidth
                SelectProps={{ native: true }}
                value={editedUser.status}
                onChange={(e) => handleChange("status", e.target.value)}
              >
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
              </TextField>
            ) : (
              <TextField
                label="Status"
                fullWidth
                value={editedUser.status}
                InputProps={{ readOnly: true }}
              />
            )}
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Created At"
              fullWidth
              value={formatDate(editedUser.createdAt)}
              InputProps={{ readOnly: true }}
            />
          </Grid>
        </Grid>

        {/* ADDRESS */}
        <Typography variant="h6" fontWeight="bold" mb={2}>
          Address
        </Typography>
        <Grid container spacing={3} mb={2}>
          <Grid item xs={12} md={6}>
            <TextField
              label="Country"
              fullWidth
              value={editedUser.country || ""}
              InputProps={{ readOnly: !editMode }}
              onChange={(e) => handleChange("country", e.target.value)}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="City/State"
              fullWidth
              value={editedUser.city || ""}
              InputProps={{ readOnly: !editMode }}
              onChange={(e) => handleChange("city", e.target.value)}
            />
          </Grid>
        </Grid>

        {/* BUTTON BACK */}
        <Box display="flex" justifyContent="flex-end" mt={4}>
          <Button
            variant="outlined"
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate("/admin/ev_owner_management")}
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

      {/* SNACKBAR */}
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
          User information updated successfully!
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ViewEvOwner;
