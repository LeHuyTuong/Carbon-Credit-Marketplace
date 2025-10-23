import {
  Box,
  Typography,
  Paper,
  TextField,
  Button,
  Divider,
  useTheme,
  Snackbar,
  Alert,
  Grid
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutline";
import CancelOutlinedIcon from "@mui/icons-material/CancelOutlined";
import { useEffect, useState } from "react";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { approveReportByAdmin, getReportByIdAdmin } from "@/apiAdmin/reportAdmin.js";

const ViewReport = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();
  const { id } = useParams();

  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [note, setNote] = useState("");
  const [openSnackbar, setOpenSnackbar] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState("");
  const [snackbarSeverity, setSnackbarSeverity] = useState("success");

  useEffect(() => {
  const fetchReport = async () => {
    try {
      const res = await getReportByIdAdmin(id);
      console.log(" API response:", res);
      setReport(res.response || res.responseData || res);
    } catch (err) {
      console.error("Error fetching report:", err);
    } finally {
      setLoading(false);
    }
  };
  fetchReport();
}, [id]);



  const handleApproval = async (approved) => {
    try {
      const res = await approveReportByAdmin(id, approved, note);
      setSnackbarSeverity("success");
      setSnackbarMessage(
        approved
          ? "Report approved successfully!"
          : "Report rejected successfully!"
      );
      setOpenSnackbar(true);
      setReport(res.response);
    } catch (err) {
      console.error(err);
      setSnackbarSeverity("error");
      setSnackbarMessage("Failed to update report status!");
      setOpenSnackbar(true);
    }
  };

  if (loading) return <Typography m={3}>Loading...</Typography>;
  if (!report) return <Typography m={3}>Report not found.</Typography>;

  return (
    <Box m="20px">
      <Header title="REPORT DETAILS" subtitle="Final approval for this report" />

      <Paper
        elevation={2}
        sx={{
          p: 4,
          borderRadius: 3,
          backgroundColor: colors.primary[400],
          color: colors.grey[100],
        }}
      >
        <Typography variant="h5" fontWeight="bold" mb={3}>
          Report Information
        </Typography>

        {/* ✅ Dùng Grid2 với prop 'size' thay vì item/xs/md */}
        <Grid container spacing={3} mb={2}>
          <Grid size={{ xs: 12, md: 6 }}>
            <TextField
              label="Report ID"
              value={report.id}
              fullWidth
              InputProps={{ readOnly: true }}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <TextField
              label="Seller Name"
              value={report.sellerName || ""}
              fullWidth
              InputProps={{ readOnly: true }}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <TextField
              label="Project Name"
              value={report.projectName || ""}
              fullWidth
              InputProps={{ readOnly: true }}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <TextField
              label="Reporting Period"
              value={report.period || ""}
              fullWidth
              InputProps={{ readOnly: true }}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <TextField
              label="Total Energy"
              value={report.totalEnergy || 0}
              fullWidth
              InputProps={{ readOnly: true }}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <TextField
              label="Total CO₂"
              value={report.totalCo2 || 0}
              fullWidth
              InputProps={{ readOnly: true }}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <TextField
              label="Vehicle Count"
              value={report.vehicleCount || 0}
              fullWidth
              InputProps={{ readOnly: true }}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <TextField
              label="Status"
              value={report.status || ""}
              fullWidth
              InputProps={{ readOnly: true }}
            />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <TextField
              label="Admin Note"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              fullWidth
              multiline
              rows={3}
              placeholder="Enter note (required if rejecting)"
            />
          </Grid>
        </Grid>

        <Divider sx={{ my: 3, borderColor: colors.grey[700] }} />

        {/* Buttons */}
        <Box display="flex" justifyContent="space-between" mt={3}>
          <Button
            variant="outlined"
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate("/admin/report_management")}
            sx={{
              borderColor: colors.blueAccent[400],
              color: colors.blueAccent[400],
              textTransform: "none",
            }}
          >
            Back to List
          </Button>

          <Box display="flex" gap={2}>
            <Button
              variant="contained"
              color="success"
              startIcon={<CheckCircleOutlineIcon />}
              onClick={() => handleApproval(true)}
              sx={{ textTransform: "none" }}
            >
              Approve
            </Button>
            <Button
              variant="contained"
              color="error"
              startIcon={<CancelOutlinedIcon />}
              onClick={() => handleApproval(false)}
              sx={{ textTransform: "none" }}
            >
              Reject
            </Button>
          </Box>
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
          severity={snackbarSeverity}
          variant="filled"
          sx={{ width: "100%" }}
        >
          {snackbarMessage}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ViewReport;
