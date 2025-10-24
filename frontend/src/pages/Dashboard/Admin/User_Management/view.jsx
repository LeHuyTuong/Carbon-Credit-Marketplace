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
  Select,
  MenuItem,
  Snackbar,
  Alert,
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { useState, useEffect, useMemo } from "react";
import { getUserByEmail, updateUser } from "@/apiAdmin/userAdmin.js";

// ===================== ROLE MAPPING =====================
const ACCESS_TO_ROLE = {
  ev_owner: "EV_OWNER",
  company: "COMPANY",
  cva: "CVA",
  admin: "ADMIN",
};
const ROLE_TO_ACCESS = Object.fromEntries(
  Object.entries(ACCESS_TO_ROLE).map(([k, v]) => [v, k])
);

const ViewUser = () => {
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

        const primaryRole =
          Array.isArray(data.roles) && data.roles.length
            ? data.roles[0].name
            : "EV_OWNER";
        const access = ROLE_TO_ACCESS[primaryRole] ?? "ev_owner";

        let companyName = "";
        if (access === "company") {
          companyName =
            data.wallet?.carbonCredit?.company?.companyName ||
            data.company?.companyName ||
            "";
        }

        let organization = "";
        if (access === "cva") {
          organization =
            data.organization ||
            data.wallet?.carbonCredit?.project?.applications?.[0]?.reviewer
              ?.organization ||
            "";
        }

        const uiUser = {
          id: data.id ?? "",
          name:
            data.name ||
            data.fullName ||
            data.wallet?.user ||
            data.wallet?.carbonCredit?.issuedBy ||
            "",
          email: data.email || "",
          phone: data.phone || "",
          access,
          status: data.status || "ACTIVE",
          createdAt: data.createdAt || data.createAt || "",
          country: data.country || "",
          city: data.city || "",
          company: companyName,
          organization,
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

  // ===================== HANDLE CHANGE =====================
  const handleChange = (field, value) => {
    setEditedUser((prev) => ({ ...prev, [field]: value }));
  };

  const validateBeforeUpdate = useMemo(() => {
    return (u) => {
      if (!u) return "User is empty";
      if (!u.name?.trim()) return "Full Name is required";
      if (!u.email?.trim()) return "Email is required";
      if (!u.access || !ACCESS_TO_ROLE[u.access]) return "Invalid role";
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
      roles: [{ name: ACCESS_TO_ROLE[editedUser.access] }],
      country: editedUser.country || null,
      city: editedUser.city || null,
      ...(editedUser.access === "company"
        ? { companyName: editedUser.company || null }
        : {}),
      ...(editedUser.access === "cva"
        ? { organization: editedUser.organization || null }
        : {}),
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
      <Header title="USER DETAILS" subtitle="View or edit user information" />
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
              {editedUser.access}
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
              <Select
                fullWidth
                value={editedUser.access}
                onChange={(e) => handleChange("access", e.target.value)}
              >
                <MenuItem value="ev_owner">EV Owner</MenuItem>
                <MenuItem value="company">Company</MenuItem>
                <MenuItem value="cva">CVA</MenuItem>
                <MenuItem value="admin">Admin</MenuItem>
              </Select>
            ) : (
              <TextField
                label="Role"
                fullWidth
                value={editedUser.access}
                InputProps={{ readOnly: true }}
              />
            )}
          </Grid>

          {editedUser.access === "company" && (
            <Grid item xs={12} md={6}>
              <TextField
                label="Company Name"
                fullWidth
                value={editedUser.company || ""}
                InputProps={{ readOnly: !editMode }}
                onChange={(e) => handleChange("company", e.target.value)}
              />
            </Grid>
          )}

          {editedUser.access === "cva" && (
            <Grid item xs={12} md={6}>
              <TextField
                label="Organization"
                fullWidth
                value={editedUser.organization || ""}
                InputProps={{ readOnly: !editMode }}
                onChange={(e) => handleChange("organization", e.target.value)}
              />
            </Grid>
          )}

          <Grid item xs={12} md={6}>
            {editMode ? (
              <Select
                fullWidth
                value={editedUser.status}
                onChange={(e) => handleChange("status", e.target.value)}
              >
                <MenuItem value="ACTIVE">Active</MenuItem>
                <MenuItem value="INACTIVE">Inactive</MenuItem>
              </Select>
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
            onClick={() => navigate("/admin/user_management")}
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

export default ViewUser;
