import {
  Box,
  Typography,
  Grid,
  Paper,
  TextField,
  Button,
  Divider,
  Snackbar,
  Alert,
  useTheme,
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { getVehicles } from "@/apiAdmin/EVAdmin.js";

const ViewEV = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();
  const { id } = useParams();

  const [vehicle, setVehicle] = useState(null);
  const [errorSnackbar, setErrorSnackbar] = useState("");

  // Lấy dữ liệu vehicle từ API
  useEffect(() => {
    const fetchVehicle = async () => {
      try {
        const res = await getVehicles(0, 100);
        const found = res.data.find((v) => v.id === Number(id));
        if (found) {
          setVehicle(found);
        } else {
          setErrorSnackbar("Vehicle not found");
        }
      } catch (err) {
        console.error("Failed to fetch vehicle:", err);
        setErrorSnackbar("Failed to load vehicle data");
      }
    };
    fetchVehicle();
  }, [id]);

  if (!vehicle)
    return (
      <Box m="20px">
        <Typography variant="h5" color="error">
          Vehicle not found.
        </Typography>
        <Button
          variant="outlined"
          onClick={() => navigate("/admin/ev_management")}
        >
          Back to List
        </Button>
      </Box>
    );

  return (
    <Box m="20px">
      <Header
        title="EV DETAILS"
        subtitle="View electric vehicle information"
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
        {/* Header */}
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
          <Typography variant="h5" fontWeight="bold">
            Vehicle Information
          </Typography>
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
              value={vehicle.id}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Number Plate"
              fullWidth
              value={vehicle.plateNumber || ""}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Vehicle Brand"
              fullWidth
              value={vehicle.brand || ""}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Vehicle Model"
              fullWidth
              value={vehicle.model || ""}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Company ID"
              fullWidth
              value={vehicle.companyId || ""}
              InputProps={{ readOnly: true }}
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
        open={!!errorSnackbar}
        autoHideDuration={3000}
        onClose={() => setErrorSnackbar("")}
        anchorOrigin={{ vertical: "top", horizontal: "center" }}
      >
        <Alert
          onClose={() => setErrorSnackbar("")}
          severity="error"
          variant="filled"
          sx={{ width: "100%" }}
        >
          {errorSnackbar}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ViewEV;
