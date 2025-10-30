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
  Dialog,
  DialogTitle,
  DialogContent,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
} from "@mui/material";
import { useNavigate, useParams } from "react-router-dom";
import { useTheme } from "@mui/material/styles";
import { tokens } from "@/themeCVA";
import Header from "@/components/Chart/Header.jsx";
import { useState, useEffect } from "react";
import { verifyReportCVA, getReportById, getReportDetails } from "@/apiCVA/reportCVA.js";
import FormulaImage from "@/assets/z7155603890092_2ed7af1b23662f3986de0fc7dce736af.jpg";
import { analyzeReportByAI } from "@/apiCVA/aiCVA.js";

const ViewReport = ({ report: initialReport }) => {
  const navigate = useNavigate();
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const { id } = useParams();

  const [report, setReport] = useState(initialReport || null);
  const [note, setNote] = useState(initialReport?.note || "");
  const [loading, setLoading] = useState(!initialReport);
  const [openSnackbar, setOpenSnackbar] = useState(false);
  const [snackbarMsg, setSnackbarMsg] = useState("");
  const [snackbarSeverity, setSnackbarSeverity] = useState("success");

  const [detailsOpen, setDetailsOpen] = useState(false);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [details, setDetails] = useState([]);

  const [actionTaken, setActionTaken] = useState(false);
  // state cho AI Analysis
  const [aiResult, setAiResult] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);


  // state mới cho ảnh công thức
  const [showFormula, setShowFormula] = useState(false);

  const colorMap = {
    Pending: "#42A5F5",
    Approved: "#4CAF50",
    Rejected: "#E53935",
    SUBMITTED: "#42A5F5",
    CVA_APPROVED: "#4CAF50",
    ADMIN_APPROVED: "#2E7D32",
    REJECTED: "#E53935",
  };

  // Fetch report khi component mount
  useEffect(() => {
    if (!report && id) {
      const fetchReport = async () => {
        setLoading(true);
        try {
          const data = await getReportById(id);
          if (data) {
            setReport(data);
            if (["CVA_APPROVED", "REJECTED"].includes(data.status)) setActionTaken(true);
          } else console.error("Unexpected response:", data);
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
    } else if (report) {
      if (["CVA_APPROVED", "REJECTED"].includes(report.status)) setActionTaken(true);
    }
  }, [id, report]);

  const handleUpdate = async (approved) => {
    if (!report) return;
    setLoading(true);
    try {
      await verifyReportCVA(report.id, { approved, comment: approved ? "" : note });
      setReport((prev) => ({ ...prev, status: approved ? "CVA_APPROVED" : "REJECTED" }));
      setSnackbarSeverity("success");
      setSnackbarMsg("Status updated successfully!");
      setActionTaken(true);
    } catch (err) {
      console.error("Update failed:", err);
      setSnackbarSeverity("error");
      setSnackbarMsg("Failed to update status!");
    } finally {
      setOpenSnackbar(true);
      setLoading(false);
    }
  };

  const handleOpenDetails = async () => {
    if (!report?.id) return;
    setDetailsOpen(true);
    setDetailsLoading(true);
    try {
      const data = await getReportDetails(report.id);
      setDetails(Array.isArray(data) ? data : []);
    } catch (e) {
      console.error("Load details failed:", e);
      setSnackbarSeverity("error");
      setSnackbarMsg("Failed to load details!");
      setOpenSnackbar(true);
    } finally {
      setDetailsLoading(false);
    }
  };

  const handleCloseDetails = () => setDetailsOpen(false);
  const handleCloseSnackbar = () => setOpenSnackbar(false);
  // Giả lập gọi AI để phân tích báo cáo
  const handleAnalyzeByAI = async () => {
  if (!report?.id) return;
  setAiLoading(true);
  setSnackbarMsg("AI is analyzing this report...");
  setSnackbarSeverity("info");
  setOpenSnackbar(true);

  try {
    const result = await analyzeReportByAI(report.id);
    console.log("AI Result:", result);
    // API trả về có thể là { aiPreScore, aiVersion, aiPreNotes }
    setAiResult(result);
    setSnackbarMsg("AI analysis completed!");
    setSnackbarSeverity("success");
  } catch (err) {
    console.error("AI analysis failed:", err);
    setSnackbarMsg(err.message || "AI analysis failed!");
    setSnackbarSeverity("error");
  } finally {
    setOpenSnackbar(true);
    setAiLoading(false);
  }
};



  if (loading) {
    return (
      <Box m="20px" display="flex" justifyContent="center" alignItems="center" height="70vh">
        <CircularProgress color="info" />
      </Box>
    );
  }

  if (!report) {
    return (
      <Box m="20px" textAlign="center">
        <Typography variant="h4" color={colors.grey[100]}>
          Report data not available
        </Typography>
        <Button variant="outlined" color="info" sx={{ mt: 2 }} onClick={() => navigate(-1)}>
          Back
        </Button>
      </Box>
    );
  }

  const color = colorMap[report.status] || colors.grey[300];

  return (
    <Box m="20px">
      <Header title="REPORT DETAIL" subtitle={`Details of Report ${report.id}`} />

      <Grid container spacing={2}>
        {/* Cột trái: ảnh công thức */}
        <Grid item xs={12} sm={6}>
          <Box display="flex" flexDirection="column" alignItems="center">
            <Button
              variant="outlined"
              color="secondary"
              onClick={() => setShowFormula((prev) => !prev)}
              sx={{ mb: 2 }}
            >
              {showFormula ? "Close Formula" : "Open Formula"}
            </Button>
            {showFormula && (
              <Box
                sx={{
                  width: "100%",
                  height: "70vh",
                  borderRadius: 2,
                  overflow: "hidden",
                  boxShadow: 2,
                  display: "flex",
                  justifyContent: "center",
                  alignItems: "center",
                  backgroundColor: colors.primary[500],
                }}
              >
                <img
                  src={FormulaImage}
                  alt="Formula"
                  style={{ width: "100%", height: "100%", objectFit: "contain" }}
                />
              </Box>
            )}
          </Box>
        </Grid>

        {/* Cột phải: thông tin report */}
        <Grid item xs={12} sm={6}>
          <Paper
            elevation={4}
            sx={{ backgroundColor: colors.primary[400], p: 3, borderRadius: 2, boxShadow: 3 }}
          >
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <Typography variant="h6" color={colors.grey[100]}>Report ID:</Typography>
                <Typography>{report.id}</Typography>

                <Typography variant="h6" color={colors.grey[100]} mt={2}>Company (Sender):</Typography>
                <Typography>{report.sellerName}</Typography>
                <Typography variant="body2" color={colors.grey[300]}>ID: {report.sellerId}</Typography>

                <Typography variant="h6" color={colors.grey[100]} mt={2}>Project:</Typography>
                <Typography>{report.projectName}</Typography>

                <Typography variant="h6" color={colors.grey[100]} mt={2}>Reporting Period:</Typography>
                <Typography>{report.period}</Typography>

                <Typography variant="h6" color={colors.grey[100]} mt={2}>Total Energy:</Typography>
                <Typography>{report.totalEnergy}</Typography>
              </Grid>

              <Grid item xs={12} sm={6}>
                <Typography variant="h6" color={colors.grey[100]}>Total CO₂:</Typography>
                <Typography>{report.totalCo2}</Typography>

                <Typography variant="h6" color={colors.grey[100]} mt={2}>Submission Date:</Typography>
                <Typography>{report.submittedAt}</Typography>

                <Typography variant="h6" color={colors.grey[100]} mt={2}>Attachment:</Typography>
                {report.uploadStorageUrl ? (
                  <Button
                    variant="outlined"
                    color="info"
                    onClick={() => window.open(report.uploadStorageUrl, "_blank")}
                  >
                    Download File
                  </Button>
                ) : (<Typography>—</Typography>)}

                <Typography variant="h6" color={colors.grey[100]} mt={2}>Status:</Typography>
                <Typography sx={{ color, fontWeight: 600, textTransform: "capitalize" }}>
                  {report.status?.replace("_", " ")}
                </Typography>

                {report.status === "REJECTED" && (
                  <Box mt={2}>
                    <Typography variant="h6" color={colors.grey[100]}>Note:</Typography>
                    <TextField
                      fullWidth multiline rows={3}
                      placeholder="Enter rejection reason or technical comments..."
                      value={note} onChange={(e) => setNote(e.target.value)} sx={{ mt: 1 }}
                    />
                  </Box>
                )}
              </Grid>
            </Grid>

            {/* Gộp 4 nút trên một hàng */}
            <Box display="flex" justifyContent="flex-end" alignItems="center" gap={2} mt={4} flexWrap="nowrap">
              {!actionTaken && (
                <>
                  <Button variant="contained" color="success" onClick={() => handleUpdate(true)}>Approve</Button>
                  <Button variant="contained" color="error" onClick={() => handleUpdate(false)}>Reject</Button>
                </>
              )}
              <Button
                variant="contained"
                color="secondary"
                onClick={handleAnalyzeByAI}
                disabled={aiLoading}
              >
                {aiLoading ? "Analyzing..." : "Analyze by AI"}
              </Button>

              <Button variant="outlined" color="info" onClick={handleOpenDetails}>View details</Button>
              <Button variant="outlined" color="inherit" onClick={() => navigate(-1)}>Back</Button>
            </Box>
            {aiResult && (
              <Paper
                elevation={3}
                sx={{
                  mt: 3,
                  p: 2,
                  borderRadius: 2,
                  backgroundColor: colors.primary[400],
                  boxShadow: 2,
                  position: "relative",
                }}
              >
                <Box display="flex" justifyContent="space-between" alignItems="center">
                  <Typography variant="h6" color={colors.grey[100]}>
                    AI Evaluation
                  </Typography>
                  <Button
                    variant="outlined"
                    size="small"
                    color="error"
                    onClick={() => setAiResult(null)}
                  >
                    Close
                  </Button>
                </Box>
                <Typography mt={1}>Version: {aiResult.aiVersion}</Typography>
                <Typography>Score: {aiResult.aiPreScore}</Typography>
                <Typography>Notes: {aiResult.aiPreNotes}</Typography>
              </Paper>
            )}

          </Paper>
        </Grid>
      </Grid>

      {/* Dialog chi tiết */}
      <Dialog open={detailsOpen} onClose={handleCloseDetails} fullWidth maxWidth="md">
        <DialogTitle>Report #{report.id} – Vehicle details</DialogTitle>
        <DialogContent dividers>
          {detailsLoading ? (
            <Box display="flex" justifyContent="center" py={4}><CircularProgress /></Box>
          ) : details?.length ? (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>Vehicle ID</TableCell>
                  <TableCell>Period</TableCell>
                  <TableCell align="right">Total Energy</TableCell>
                  <TableCell align="right">CO₂ (kg)</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {details.map((row) => (
                  <TableRow key={row.id}>
                    <TableCell>{row.id}</TableCell>
                    <TableCell>{row.vehicleId}</TableCell>
                    <TableCell>{row.period}</TableCell>
                    <TableCell align="right">{row.totalEnergy}</TableCell>
                    <TableCell align="right">{row.co2Kg}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : (<Typography variant="body2">No details found.</Typography>)}
          <Box display="flex" justifyContent="flex-end" mt={2}>
            <Button onClick={handleCloseDetails}>Close</Button>
          </Box>
        </DialogContent>
      </Dialog>

      {/* Snackbar */}
      <Snackbar
        open={openSnackbar}
        autoHideDuration={3000}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
      >
        <Alert onClose={handleCloseSnackbar} severity={snackbarSeverity} sx={{ width: "100%" }}>
          {snackbarMsg}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ViewReport;
