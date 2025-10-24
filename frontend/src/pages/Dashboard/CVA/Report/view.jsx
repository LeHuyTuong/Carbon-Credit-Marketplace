// src/views/ViewReport.jsx
import {
  Box,
  Typography,
  Button,
  Grid,
  Paper,
  Snackbar,
  Alert,
  TextField,
  CircularProgress,
} from "@mui/material";
import { useNavigate, useParams } from "react-router-dom";
import { useTheme } from "@mui/material/styles";
import { tokens } from "@/themeCVA";
import Header from "@/components/Chart/Header.jsx";
import { useState, useEffect } from "react";
import { verifyReportCVA, getReportById } from "@/apiCVA/reportCVA.js";

const ViewReport = ({ report: initialReport }) => {
  const navigate = useNavigate();
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const { id } = useParams();

  // --- State ---
  const [report, setReport] = useState(initialReport || null);
  const [note, setNote] = useState(initialReport?.note || "");
  const [loading, setLoading] = useState(!initialReport);
  const [openSnackbar, setOpenSnackbar] = useState(false);
  const [snackbarMsg, setSnackbarMsg] = useState("");
  const [snackbarSeverity, setSnackbarSeverity] = useState("success");

  const colorMap = {
    Pending: "#42A5F5",
    Approved: "#4CAF50",
    Rejected: "#E53935",
    SUBMITTED: "#42A5F5",
    CVA_APPROVED: "#4CAF50",
    ADMIN_APPROVED: "#2E7D32",
  };

  // --- Fetch report khi không có prop ---
  useEffect(() => {
    if (!report && id) {
      const fetchReport = async () => {
        setLoading(true);
        try {
          const data = await getReportById(id);
          if (data) {
            setReport(data);
          } else {
            console.error("Unexpected response:", data);
          }
        } catch (err) {
          console.error("Failed to fetch report:", err);
          setSnackbarSeverity("error");
          setSnackbarMsg("Failed to load report data!");
          setOpenSnackbar(true);
        } finally {
          setLoading(false);
        }
      };
      fetchReport();
    }
  }, [id, report]);

  // --- Cập nhật trạng thái report ---
  const handleUpdate = async (approved) => {
    if (!report) return;
    setLoading(true);
    try {
      await verifyReportCVA(report.id, {
        approved,
        comment: approved ? "" : note,
      });

      setReport((prev) => ({
        ...prev,
        status: approved ? "CVA_APPROVED" : "REJECTED",
      }));

      setSnackbarSeverity("success");
      setSnackbarMsg("Status updated successfully!");
    } catch (err) {
      console.error("Update failed:", err);
      setSnackbarSeverity("error");
      setSnackbarMsg("Failed to update status!");
    } finally {
      setOpenSnackbar(true);
      setLoading(false);
    }
  };

  const handleCloseSnackbar = () => setOpenSnackbar(false);

  // --- Loading ---
  if (loading) {
    return (
      <Box
        m="20px"
        display="flex"
        justifyContent="center"
        alignItems="center"
        height="70vh"
      >
        <CircularProgress color="info" />
      </Box>
    );
  }

  // --- Không có report ---
  if (!report) {
    return (
      <Box m="20px" textAlign="center">
        <Typography variant="h4" color={colors.grey[100]}>
          Report data not available
        </Typography>
        <Button
          variant="outlined"
          color="info"
          sx={{ mt: 2 }}
          onClick={() => navigate(-1)}
        >
          Back
        </Button>
      </Box>
    );
  }

  const color = colorMap[report.status] || colors.grey[300];

  return (
    <Box m="20px">
      <Header
        title="REPORT DETAIL"
        subtitle={`Details of Report ${report.id}`}
      />

      <Paper
        elevation={4}
        sx={{
          backgroundColor: colors.primary[400],
          p: "30px",
          borderRadius: "10px",
          boxShadow: 3,
        }}
      >
        <Grid container spacing={2}>
          {/* Left Column */}
          <Grid item xs={12} sm={6}>
            <Typography variant="h6" color={colors.grey[100]}>
              Report ID:
            </Typography>
            <Typography>{report.id}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Company (Sender):
            </Typography>
            <Typography>{report.sellerName}</Typography>
            <Typography variant="body2" color={colors.grey[300]}>
              ID: {report.sellerId}
            </Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Project:
            </Typography>
            <Typography>{report.projectName}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Reporting Period:
            </Typography>
            <Typography>{report.period}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Total Energy:
            </Typography>
            <Typography>{report.totalEnergy}</Typography>
          </Grid>

          {/* Right Column */}
          <Grid item xs={12} sm={6}>
            <Typography variant="h6" color={colors.grey[100]}>
              Total CO₂:
            </Typography>
            <Typography>{report.totalCo2}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Submission Date:
            </Typography>
            <Typography>{report.submittedAt}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Attachment:
            </Typography>
            {report.uploadStorageUrl ? (
              <Button
                variant="outlined"
                color="info"
                onClick={() => window.open(report.uploadStorageUrl, "_blank")}
              >
                Download File
              </Button>
            ) : (
              <Typography>—</Typography>
            )}

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Status:
            </Typography>
            <Typography
              sx={{
                color,
                fontWeight: 600,
                textTransform: "capitalize",
              }}
            >
              {report.status?.replace("_", " ")}
            </Typography>

            {report.status === "REJECTED" && (
              <Box mt={2}>
                <Typography variant="h6" color={colors.grey[100]}>
                  Note:
                </Typography>
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  placeholder="Enter rejection reason or technical comments..."
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  sx={{ mt: 1 }}
                />
              </Box>
            )}
          </Grid>
        </Grid>

        {/* Action Buttons */}
        <Box display="flex" justifyContent="flex-end" gap={2} mt={4}>
          <Button
            variant="contained"
            color="success"
            onClick={() => handleUpdate(true)}
          >
            Approve
          </Button>
          <Button
            variant="contained"
            color="error"
            onClick={() => handleUpdate(false)}
          >
            Reject
          </Button>
          
          <Button
            variant="outlined"
            color="inherit"
            onClick={() => navigate(-1)}
          >
            Back
          </Button>
        </Box>
      </Paper>

      {/* Snackbar */}
      <Snackbar
        open={openSnackbar}
        autoHideDuration={3000}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
      >
        <Alert
          onClose={handleCloseSnackbar}
          severity={snackbarSeverity}
          sx={{ width: "100%" }}
        >
          {snackbarMsg}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ViewReport;