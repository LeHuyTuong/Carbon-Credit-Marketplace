import {
  Box,
  Typography,
  Grid,
  Paper,
  TextField,
  Button,
  Divider,
  Select,
  MenuItem,
  Snackbar,
  Alert,
  useTheme,
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import { useState } from "react";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import VisibilityIcon from "@mui/icons-material/Visibility";
import DownloadIcon from "@mui/icons-material/Download";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { mockDataEV } from "@/data/mockData";

const ViewEV = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();
  const { id } = useParams();

  // lấy dữ liệu từ mock
  const [data, setData] = useState(mockDataEV);
  const [vehicle, setVehicle] = useState(() =>
    data.find((v) => v.id === Number(id))
  );

  const [editMode, setEditMode] = useState(false);
  const [editedVehicle, setEditedVehicle] = useState({ ...vehicle });
  const [openSnackbar, setOpenSnackbar] = useState(false);

  const handleChange = (field, value) => {
    setEditedVehicle((prev) => ({ ...prev, [field]: value }));
  };

  const handleUpdate = () => {
    const updatedData = data.map((item) =>
      item.id === vehicle.id ? editedVehicle : item
    );
    setData(updatedData);
    setVehicle(editedVehicle);
    localStorage.setItem("evData", JSON.stringify(updatedData));

    setEditMode(false);
    setOpenSnackbar(true);
  };

  return (
    <Box m="20px">
      <Header title="EV DETAILS" subtitle="View or edit electric vehicle information" />

      <Paper
        elevation={2}
        sx={{
          p: 4,
          borderRadius: 3,
          backgroundColor: colors.primary[400],
          color: colors.grey[100],
        }}
      >
        {/* Header trong card */}
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
          <Typography variant="h5" fontWeight="bold">
            Vehicle Information
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
                  setEditedVehicle(vehicle);
                  setEditMode(false);
                }}
                sx={{ textTransform: "none" }}
              >
                Cancel
              </Button>
            </Box>
          )}
        </Box>

        <Divider sx={{ mb: 3, borderColor: colors.grey[700] }} />

        {/* Vehicle Details */}
        <Typography variant="h6" fontWeight="bold" mb={2}>
          Vehicle Details
        </Typography>

        <Grid container spacing={3} mb={4}>
          <Grid item xs={12} md={6}>
            <TextField
              label="EV ID"
              fullWidth
              value={editedVehicle.evid}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Number Plate"
              fullWidth
              value={editedVehicle.numberplate}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Vehicle Brand"
              fullWidth
              value={editedVehicle.vehiclebrand}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Vehicle Model"
              fullWidth
              value={editedVehicle.vehiclemodel}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Year of Manufacture"
              fullWidth
              value={editedVehicle.yearofmanufacture}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Company"
              fullWidth
              value={editedVehicle.aggregator}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            {editMode ? (
              <Box>
                <Typography variant="body2" sx={{ mb: 0.5 }}>
                  Status
                </Typography>
                <Select
                  fullWidth
                  value={editedVehicle.status}
                  onChange={(e) => handleChange("status", e.target.value)}
                >
                  <MenuItem value="approved">Approved</MenuItem>
                  <MenuItem value="rejected">Rejected</MenuItem>
                  <MenuItem value="pending">Pending</MenuItem>
                </Select>
              </Box>
            ) : (
              <TextField
                label="Status"
                fullWidth
                value={editedVehicle.status}
                InputProps={{ readOnly: true }}
              />
            )}
          </Grid>
        </Grid>

        {/* Driver License Section */}
        <Typography variant="h6" fontWeight="bold" mb={2}>
          Driver License
        </Typography>
        <Box display="flex" gap={2} mb={4}>
          <Button
            variant="contained"
            startIcon={<VisibilityIcon />}
            sx={{
              textTransform: "none",
              backgroundColor: colors.blueAccent[500],
              "&:hover": { backgroundColor: colors.blueAccent[700] },
            }}
          >
            View
          </Button>
          <Button
            variant="outlined"
            startIcon={<DownloadIcon />}
            sx={{
              textTransform: "none",
              borderColor: colors.greenAccent[400],
              color: colors.greenAccent[400],
              "&:hover": { borderColor: colors.greenAccent[300] },
            }}
          >
            Download
          </Button>
        </Box>

        {/* Back Button */}
        <Box display="flex" justifyContent="flex-end">
          <Button
            variant="outlined"
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate("/admin/ev_management")}
            sx={{
              borderColor: colors.blueAccent[400],
              color: colors.blueAccent[400],
              textTransform: "none",
            }}
          >
            Back to List
          </Button>
        </Box>
      </Paper>

      {/* Snackbar thông báo */}
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
          Vehicle updated successfully!
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ViewEV;
