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
import { useState, useEffect } from "react";
import { getUserById, updateUser } from "@/apiAdmin/userAdmin.js"; // API functions

const ViewUser = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();
  const { id } = useParams();

  const [user, setUser] = useState(null);
  const [editedUser, setEditedUser] = useState(null);
  const [editMode, setEditMode] = useState(false);
  const [openSnackbar, setOpenSnackbar] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchUser() {
      try {
        const res = await getUserById(id);
        setUser(res);
        setEditedUser(res);
      } catch (error) {
        console.error("Error fetching user:", error);
      } finally {
        setLoading(false);
      }
    }
    fetchUser();
  }, [id]);

  const handleChange = (field, value) => {
    setEditedUser((prev) => ({ ...prev, [field]: value }));
  };

  const handleUpdate = async () => {
    try {
      await updateUser(user.id, editedUser);
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
        {/* Header */}
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

        {/* Avatar */}
        <Box display="flex" alignItems="center" mb={3}>
          <Avatar
            src={`https://i.pravatar.cc/100?u=${user.id}`}
            alt={user.name}
            sx={{ width: 80, height: 80, mr: 3 }}
          />
          <Box>
            <Typography variant="h6" fontWeight="bold">
              {user.name}
            </Typography>
            <Typography variant="body2" color={colors.grey[300]}>
              {editedUser.access}
            </Typography>
          </Box>
        </Box>

        <Divider sx={{ mb: 3, borderColor: colors.grey[700] }} />

        {/* Personal Details */}
        <Typography variant="h6" fontWeight="bold" mb={2}>
          Personal Details
        </Typography>

        <Grid container spacing={3} mb={4}>
          <Grid item xs={12} md={6}>
            <TextField label="Full Name" fullWidth value={editedUser.name} InputProps={{ readOnly: !editMode }} onChange={(e) => handleChange("name", e.target.value)} />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField label="Email Address" fullWidth value={editedUser.email} InputProps={{ readOnly: !editMode }} onChange={(e) => handleChange("email", e.target.value)} />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField label="Phone" fullWidth value={editedUser.phone} InputProps={{ readOnly: !editMode }} onChange={(e) => handleChange("phone", e.target.value)} />
          </Grid>
          {editedUser.access === "company" && (
            <Grid item xs={12} md={6}>
              <TextField
                label="Company"
                fullWidth
                value={editedUser.company || ""}
                InputProps={{ readOnly: !editMode }}
                onChange={(e) => handleChange("company", e.target.value)}
              />
            </Grid>
          )}
          <Grid item xs={12} md={6}>
            {editMode ? (
              <Select fullWidth value={editedUser.access} onChange={(e) => handleChange("access", e.target.value)}>
                <MenuItem value="ev_owner">Ev-Owner</MenuItem>
                <MenuItem value="company">Company</MenuItem>
                <MenuItem value="cva">CVA</MenuItem>
                <MenuItem value="admin">Admin</MenuItem>
              </Select>
            ) : (
              <TextField label="Role" fullWidth value={editedUser.access} InputProps={{ readOnly: true }} />
            )}
          </Grid>

          <Grid item xs={12} md={6}>
            {editMode ? (
              <Select fullWidth value={editedUser.status} onChange={(e) => handleChange("status", e.target.value)}>
                <MenuItem value="active">Active</MenuItem>
                <MenuItem value="inactive">Inactive</MenuItem>
              </Select>
            ) : (
              <TextField label="Status" fullWidth value={editedUser.status} InputProps={{ readOnly: true }} />
            )}
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField label="Created Day" fullWidth value={editedUser.date} InputProps={{ readOnly: true }} />
          </Grid>
        </Grid>

        {/* Address */}
        <Typography variant="h6" fontWeight="bold" mb={2}>
          Address
        </Typography>
        <Grid container spacing={3} mb={2}>
          <Grid item xs={12} md={6}>
            <TextField label="Country" fullWidth value={editedUser.country || ""} InputProps={{ readOnly: !editMode }} onChange={(e) => handleChange("country", e.target.value)} />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField label="City/State" fullWidth value={editedUser.city || ""} InputProps={{ readOnly: !editMode }} onChange={(e) => handleChange("city", e.target.value)} />
          </Grid>
        </Grid>

        {/* Buttons */}
        <Box display="flex" justifyContent="flex-end" mt={4}>
          <Button
            variant="outlined"
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate("/admin/user_management")}
            sx={{ borderColor: colors.blueAccent[400], color: colors.blueAccent[400], textTransform: "none" }}
          >
            Back
          </Button>
        </Box>
      </Paper>

      {/* Snackbar */}
      <Snackbar open={openSnackbar} autoHideDuration={3000} onClose={() => setOpenSnackbar(false)} anchorOrigin={{ vertical: "top", horizontal: "center" }}>
        <Alert onClose={() => setOpenSnackbar(false)} severity="success" variant="filled" sx={{ width: "100%" }}>
          User information updated successfully!
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ViewUser;
