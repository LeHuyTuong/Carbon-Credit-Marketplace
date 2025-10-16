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
import { useParams } from "react-router-dom";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { mockDataTeam } from "@/data/mockData";
import { useNavigate } from "react-router-dom";
import { useState } from "react";

const ViewUser = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();

  const { id } = useParams();
  const [data, setData] = useState(mockDataTeam);
  const [user, setUser] = useState(() => data.find((u) => u.id === Number(id)));

  const [editMode, setEditMode] = useState(false);
  const [editedUser, setEditedUser] = useState({ ...user });
  const [openSnackbar, setOpenSnackbar] = useState(false);

  const handleChange = (field, value) => {
    setEditedUser((prev) => ({ ...prev, [field]: value }));
  };

  const handleUpdate = () => {
    const updatedData = data.map((item) =>
      item.id === user.id ? editedUser : item
    );
    setData(updatedData);
    setUser(editedUser);
    localStorage.setItem("userData", JSON.stringify(updatedData));
    setEditMode(false);
    setOpenSnackbar(true);
  };

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

        {/* PERSONAL DETAILS */}
        <Typography variant="h6" fontWeight="bold" mb={2}>
          Personal Details
        </Typography>

        <Grid container spacing={3} mb={4}>
          <Grid item xs={12} md={6}>
            <TextField
              label="Full Name"
              fullWidth
              value={editedUser.name}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Email Address"
              fullWidth
              value={editedUser.email}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Phone"
              fullWidth
              value={editedUser.phone}
              InputProps={{ readOnly: true }}
            />
          </Grid>
          
          {/* Hiện field Company nếu access === "company" */}
          {editedUser.access === "company" && (
            <Grid item xs={12} md={6}>
              <TextField
                label="Company"
                fullWidth
                value={editedUser.company || "CarbonTech Solutions"} // giá trị mẫu
                InputProps={{ readOnly: !editMode }}
                onChange={(e) => handleChange("company", e.target.value)}
              />
            </Grid>
          )}
          
          <Grid item xs={12} md={6}>
            {editMode ? (
              <Box>
                <Typography variant="body2" sx={{ mb: 0.5 }}>
                  Role
                </Typography>
                <Select
                  fullWidth
                  value={editedUser.access}
                  onChange={(e) => handleChange("access", e.target.value)}
                >
                  <MenuItem value="ev_owner">Ev-Owner</MenuItem>
                  <MenuItem value="company">Company</MenuItem>
                  <MenuItem value="cva">CVA</MenuItem>
                  <MenuItem value="admin">Admin</MenuItem>
                </Select>
              </Box>
            ) : (
              <TextField
                label="Role"
                fullWidth
                value={editedUser.access}
                InputProps={{ readOnly: true }}
              />
            )}
          </Grid>

          

          <Grid item xs={12} md={6}>
            {editMode ? (
              <Box>
                <Typography variant="body2" sx={{ mb: 0.5 }}>
                  Status
                </Typography>
                <Select
                  fullWidth
                  value={editedUser.status}
                  onChange={(e) => handleChange("status", e.target.value)}
                >
                  <MenuItem value="active">Active</MenuItem>
                  <MenuItem value="inactive">Inactive</MenuItem>
                </Select>
              </Box>
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
              label="Created Day"
              fullWidth
              value={editedUser.date}
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
              value={editedUser.country || "Viet Nam"}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="City/State"
              fullWidth
              value={editedUser.city || "TP HCM"}
              InputProps={{ readOnly: true }}
            />
          </Grid>
        </Grid>

        {/* Buttons */}
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

      {/* Snackbar */}
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
