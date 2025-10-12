import {
  Box,
  Typography,
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
import DownloadIcon from "@mui/icons-material/Download";
import VisibilityIcon from "@mui/icons-material/Visibility";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { mockDataReports } from "@/data/mockData";
import { useState } from "react";

const ViewReport = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();

  const { id } = useParams();
  const [data, setData] = useState(mockDataReports);
  const [report, setReport] = useState(() =>
    data.find((r) => r.id === Number(id))
  );

  const [editMode, setEditMode] = useState(false);
  const [editedReport, setEditedReport] = useState({ ...report });
  const [openSnackbar, setOpenSnackbar] = useState(false);

  const handleChange = (field, value) => {
    setEditedReport((prev) => ({ ...prev, [field]: value }));
  };

  const handleUpdate = () => {
    const updatedData = data.map((item) =>
      item.id === report.id ? editedReport : item
    );
    setData(updatedData);
    setReport(editedReport);
    localStorage.setItem("reportData", JSON.stringify(updatedData));
    setEditMode(false);
    setOpenSnackbar(true);
  };

  return (
    <Box m="20px">
      <Header title="REPORT DETAILS" subtitle="View or edit report information" />

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
            Report Information
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
                  setEditedReport(report);
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

        {/* Report Details */}
        <Typography variant="h6" fontWeight="bold" mb={2}>
          Report Details
        </Typography>

        <Grid container spacing={3} mb={4}>
          <Grid size={{ xs: 12, md: 6 }}>
            <TextField
              label="Report ID"
              fullWidth
              value={editedReport.reportid}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid size={{ xs: 12, md: 6 }}>
            <TextField
              label="Company"
              fullWidth
              value={editedReport.aggregator}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid size={{ xs: 12, md: 6 }}>
            <TextField
              label="Reporting Period"
              fullWidth
              value={editedReport.reportingperiod}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid size={{ xs: 12, md: 6 }}>
            <TextField
              label="Total EV Owners"
              fullWidth
              value={editedReport.totalevowner}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid size={{ xs: 12, md: 6 }}>
            <TextField
              label="Submission Date"
              fullWidth
              value={editedReport.submissiondate}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid size={{ xs: 12, md: 6 }}>
            {editMode ? (
              <Box>
                <Typography variant="body2" sx={{ mb: 0.5 }}>
                  Status
                </Typography>
                <Select
                  fullWidth
                  value={editedReport.status}
                  onChange={(e) => handleChange("status", e.target.value)}
                >
                  <MenuItem value="Pending">Pending</MenuItem>
                  <MenuItem value="Approved">Approved</MenuItem>
                  <MenuItem value="Rejected">Rejected</MenuItem>
                </Select>
              </Box>
            ) : (
              <TextField
                label="Status"
                fullWidth
                value={editedReport.status}
                InputProps={{ readOnly: true }}
              />
            )}
          </Grid>
        </Grid>

        {/* Linked Report */}
        <Typography variant="h6" fontWeight="bold" mb={2}>
          Linked Report
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

        {/* Back button */}
        <Box display="flex" justifyContent="flex-end" mt={4}>
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
          Report updated successfully!
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ViewReport;
