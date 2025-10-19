import {
  Box,
  Typography,
  Button,
  Grid,
  Paper,
  Snackbar,
  Alert,
  TextField,
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import { useTheme } from "@mui/material/styles";
import { tokens } from "@/themeCVA";
import Header from "@/components/Chart/Header.jsx";
import { mockDataReportsCVA } from "@/data/mockData";
import { useState } from "react";

const ViewReport = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  const initial = mockDataReportsCVA.find((item) => item.id === parseInt(id));

  if (!initial) {
    return (
      <Box m="20px">
        <Typography variant="h4" color={colors.grey[100]}>
          Report not found
        </Typography>
      </Box>
    );
  }

  const [report, setReport] = useState(initial);
  const [status, setStatus] = useState(report.status || "Pending");
  const [note, setNote] = useState(report.note || "");
  const [openSnackbar, setOpenSnackbar] = useState(false);

  const handleUpdate = (newStatus) => {
    const updated = {
      ...report,
      status: newStatus,
      note: newStatus === "Rejected" ? note : "",
    };
    setReport(updated);
    setStatus(newStatus);
    setOpenSnackbar(true);
  };

  const handleSaveNote = () => {
    const updated = { ...report, note };
    setReport(updated);
    setOpenSnackbar(true);
  };

  const handleCloseSnackbar = () => setOpenSnackbar(false);

  const colorMap = {
    Pending: "#42A5F5",
    Approved: "#4CAF50",
    Rejected: "#E53935",
  };
  const color = colorMap[report.status] || colors.grey[300];

  return (
    <Box m="20px">
      <Header
        title="REPORT DETAIL"
        subtitle={`Details of Report ${report.reportid}`}
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
          {/* Cột trái */}
          <Grid item xs={12} sm={6}>
            <Typography variant="h6" color={colors.grey[100]}>
              Report ID:
            </Typography>
            <Typography>{report.reportid}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Company (Sender):
            </Typography>
            <Typography>{report.company}</Typography>

            <Typography variant="body2" color={colors.grey[300]}>
              ID: {report.companyid || "CMP-001"}
            </Typography>
            <Typography variant="body2" color={colors.grey[300]}>
              Email: {report.companyemail || "contact@company.com"}
            </Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Reporting Period:
            </Typography>
            <Typography>{report.reportingperiod}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Total EV Owners:
            </Typography>
            <Typography>{report.totalEV}</Typography>
          </Grid>

          {/* Cột phải */}
          <Grid item xs={12} sm={6}>
            <Typography variant="h6" color={colors.grey[100]}>
              Total Proposed Credits (tCO₂):
            </Typography>
            <Typography>{report.totalcredits}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Submission Date:
            </Typography>
            <Typography>{report.submissiondate}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Attachment:
            </Typography>
            {report.file ? (
              <Button variant="outlined" color="info">
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
              {report.status}
            </Typography>

            {report.status === "Rejected" && (
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
                <Box display="flex" justifyContent="flex-end" mt={2}>
                  <Button
                    variant="contained"
                    color="warning"
                    onClick={handleSaveNote}
                    disabled={!note.trim()}
                  >
                     Save Note
                  </Button>
                </Box>
              </Box>
            )}
          </Grid>
        </Grid>

        {/* Nút hành động */}
        <Box display="flex" justifyContent="flex-end" gap={2} mt={4}>
          <Button
            variant="contained"
            color="success"
            onClick={() => handleUpdate("Approved")}
          >
             Approve
          </Button>
          <Button
            variant="contained"
            color="error"
            onClick={() => handleUpdate("Rejected")}
          >
            Reject
          </Button>
          <Button variant="contained" color="info">
             Download Data
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
          severity="success"
          sx={{ width: "100%" }}
        >
          {status === "Rejected" && note
            ? "Note saved successfully!"
            : "Status updated successfully!"}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ViewReport;
