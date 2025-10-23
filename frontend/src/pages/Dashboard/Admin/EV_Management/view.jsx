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
import { useState, useEffect } from "react";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { getVehicles, updateVehicleById } from "@/apiAdmin/EVAdmin.js";

const ViewEV = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();
  const { id } = useParams();

  const [vehicle, setVehicle] = useState(null);
  const [editMode, setEditMode] = useState(false);
  const [editedVehicle, setEditedVehicle] = useState({});
  const [openSnackbar, setOpenSnackbar] = useState(false);
  const [errorSnackbar, setErrorSnackbar] = useState("");

  // Lấy dữ liệu vehicle từ API
  useEffect(() => {
    const fetchVehicle = async () => {
      try {
        const res = await getVehicles(0, 100); // hoặc fetch theo id riêng nếu có API get by ID
        const found = res.data.find((v) => v.id === Number(id));
        if (found) {
          setVehicle(found);
          setEditedVehicle({ ...found });
        }
      } catch (err) {
        console.error("Failed to fetch vehicle:", err);
        setErrorSnackbar("Failed to load vehicle data");
      }
    };
    fetchVehicle();
  }, [id]);

  const handleChange = (field, value) => {
    setEditedVehicle((prev) => ({ ...prev, [field]: value }));
  };

  const handleUpdate = async () => {
    try {
      await updateVehicleById(vehicle.id, {
        plateNumber: editedVehicle.plateNumber,
        brand: editedVehicle.brand,
        model: editedVehicle.model,
        companyId: editedVehicle.companyId,
      });

      setVehicle({ ...editedVehicle });
      setEditMode(false);
      setOpenSnackbar(true);
    } catch (err) {
      console.error("Update failed:", err);
      setErrorSnackbar("Failed to update vehicle");
    }
  };

  if (!vehicle)
    return (
      <Box m="20px">
        <Typography variant="h5" color="error">
          Vehicle not found.
        </Typography>
        <Button variant="outlined" onClick={() => navigate("/admin/ev_management")}>
          Back to List
        </Button>
      </Box>
    );

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
        {/* Header */}
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
              value={editedVehicle.id}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Number Plate"
              fullWidth
              value={editedVehicle.plateNumber}
              onChange={(e) => handleChange("plateNumber", e.target.value)}
              InputProps={{ readOnly: !editMode }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Vehicle Brand"
              fullWidth
              value={editedVehicle.brand}
              onChange={(e) => handleChange("brand", e.target.value)}
              InputProps={{ readOnly: !editMode }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Vehicle Model"
              fullWidth
              value={editedVehicle.model}
              onChange={(e) => handleChange("model", e.target.value)}
              InputProps={{ readOnly: !editMode }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Company ID"
              fullWidth
              value={editedVehicle.companyId}
              onChange={(e) => handleChange("companyId", Number(e.target.value))}
              InputProps={{ readOnly: !editMode }}
            />
          </Grid>
        </Grid>

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

      {/* Snackbar */}
      <Snackbar
        open={openSnackbar || !!errorSnackbar}
        autoHideDuration={3000}
        onClose={() => {
          setOpenSnackbar(false);
          setErrorSnackbar("");
        }}
        anchorOrigin={{ vertical: "top", horizontal: "center" }}
      >
        <Alert
          onClose={() => {
            setOpenSnackbar(false);
            setErrorSnackbar("");
          }}
          severity={openSnackbar ? "success" : "error"}
          variant="filled"
          sx={{ width: "100%" }}
        >
          {openSnackbar ? "Vehicle updated successfully!" : errorSnackbar}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ViewEV;
