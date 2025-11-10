import {
  Box,
  Typography,
  Button,
  Grid,
  Paper,
  TextField,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  TablePagination,
} from "@mui/material";
import { useNavigate, useParams } from "react-router-dom";
import { useTheme } from "@mui/material/styles";
import { tokens } from "@/themeCVA";
import Header from "@/components/Chart/Header.jsx";
import { useState, useEffect, useCallback } from "react";
import {
  verifyReportCVA,
  getReportById,
  getReportDetails,
} from "@/apiCVA/reportCVA.js";
import FormulaImage from "@/assets/z7155603890092_2ed7af1b23662f3986de0fc7dce736af.jpg";
import { analyzeReportByAI, analyzeReportData } from "@/apiCVA/aiCVA.js";
import { useSnackbar } from "@/hooks/useSnackbar.jsx";


const ViewReport = ({ report: initialReport }) => {
  const navigate = useNavigate();
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const { id } = useParams();
  const { showSnackbar, SnackbarComponent } = useSnackbar();

  const [report, setReport] = useState(initialReport || null);
  const [note, setNote] = useState(initialReport?.note || "");
  const [loading, setLoading] = useState(!initialReport);
  const [detailsOpen, setDetailsOpen] = useState(false);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [details, setDetails] = useState([]);
  const [detailsPage, setDetailsPage] = useState(0);
  const [detailsSize, setDetailsSize] = useState(20);
  const [detailsTotal, setDetailsTotal] = useState(0);
  const [actionTaken, setActionTaken] = useState(false);

  // AI + Data
  const [aiResult, setAiResult] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [dataAnalysis, setDataAnalysis] = useState(null);
  const [dataLoading, setDataLoading] = useState(false);

  // Dialogs for popup display
  const [openAIDialog, setOpenAIDialog] = useState(false);
  const [openDataDialog, setOpenDataDialog] = useState(false);

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

  // Load report 
  useEffect(() => {
    if (!report && id) {
      (async () => {
        setLoading(true);
        try {
          const data = await getReportById(id);
          if (data) {
            setReport(data);
            if (["CVA_APPROVED", "REJECTED"].includes(data.status)) {
              setActionTaken(true);
            }
          } else {
            console.error("Unexpected getReportById response:", data);
            showSnackbar("error", "Failed to load report data!");
          }
        } catch (err) {
          console.error("Failed to fetch report:", err);
          showSnackbar("error", err.message || "Failed to load report data!");
        } finally {
          setLoading(false);
        }
      })();
    } else if (report) {
      if (["CVA_APPROVED", "REJECTED"].includes(report.status)) {
        setActionTaken(true);
      }
    }
  }, [id, report]);

  // Update status 
  const handleUpdate = async (approved) => {
    if (!report) return;
    setLoading(true);
    try {
      await verifyReportCVA(report.id, { approved, comment: approved ? "" : note });
      setReport((prev) => ({ ...prev, status: approved ? "CVA_APPROVED" : "REJECTED" }));
      showSnackbar("success", "Status updated successfully!");
    } catch (err) {
      console.error("Update failed:", err);
      showSnackbar("error", err.message || "Failed to update status!");
    } finally {
      setLoading(false);
    }
  };

  // Fetch details
  const fetchDetails = useCallback(
    async (page = detailsPage, size = detailsSize) => {
      if (!report?.id) return;
      setDetailsLoading(true);
      try {
        const pageRes = await getReportDetails(report.id, { page, size });
        const pageObj =
          pageRes?.response ||
          pageRes?.responseData ||
          pageRes?.data ||
          pageRes ||
          {};
        const rows = pageObj.content ?? [];
        const total = pageObj.totalElements ?? rows.length ?? 0;
        const normalized = rows.map((r) => ({
          ...r,
          vehicleId: r.vehicleId ?? r.vehiclePlate ?? null,
        }));
        setDetails(normalized);
        setDetailsTotal(total);
        setDetailsPage(pageObj.number ?? page);
        setDetailsSize(pageObj.size ?? size);
      } catch (e) {
        console.error("Load details failed:", e);
        setDetails([]);
        setDetailsTotal(0);
        showSnackbar("error", e.message || "Failed to load details!");
      } finally {
        setDetailsLoading(false);
      }
    },
    [report?.id, detailsPage, detailsSize]
  );

  const handleOpenDetails = async () => {
    if (!report?.id) return;
    setDetailsOpen(true);
    await fetchDetails(0, detailsSize);
  };
  const handleCloseDetails = () => setDetailsOpen(false);

  const handleChangeDetailsPage = async (_e, newPage) => {
    await fetchDetails(newPage, detailsSize);
  };
  const handleChangeDetailsRowsPerPage = async (e) => {
    const newSize = parseInt(e.target.value, 10);
    await fetchDetails(0, newSize);
  };

  // AI analyze 
  const handleAnalyzeByAI = async () => {
    if (!report?.id) return;
    setAiLoading(true);
    showSnackbar("info", "Analyzing report with AI... please wait");
    try {
      const result = await analyzeReportByAI(report.id);
      setAiResult(result);
      setOpenAIDialog(true);
      showSnackbar("success", "AI analysis completed!");
    } catch (err) {
      console.error("AI analysis failed:", err);
      showSnackbar("error", err.message || "AI analysis failed!");
    } finally {
      setAiLoading(false);
    }
  };

  //  Data analyze 
  const handleAnalyzeData = async () => {
    if (!report?.id) return;
    setDataLoading(true);
    showSnackbar("info", "Analyzing report data...");
    try {
      const result = await analyzeReportData(report.id);
      setDataAnalysis(result);
      setOpenDataDialog(true);
      showSnackbar("success", "Data analysis completed!");
    } catch (err) {
      console.error("Data analysis failed:", err);
      showSnackbar("error", err.message || "Data analysis failed!");
    } finally {
      setDataLoading(false);
    }
  };

  if (loading)
    return (
      <Box m="20px" sx={{ marginLeft: "290px" }} display="flex" justifyContent="center" alignItems="center" height="70vh">
        <CircularProgress color="info" />
      </Box>
    );

  if (!report)
    return (
      <Box m="20px" sx={{ marginLeft: "290px" }} >
        <Typography variant="h4" color={colors.grey[100]}>
          Report data not available
        </Typography>
        <Button variant="outlined" color="info" sx={{ mt: 2 }} onClick={() => navigate(-1)}>
          Back
        </Button>
      </Box>
    );

  const statusColor = colorMap[report.status] || colors.grey[300];

  return (
    <Box m="20px" sx={{ marginLeft: "290px" }} textAlign="left" >
      <Header title="REPORT DETAIL" subtitle={`Details of Report ${report.id}`} />
      <Grid container spacing={2}>
        {/* LEFT: formula image */}
        {/* <Grid item xs={12} sm={6}>
          <Box display="flex" flexDirection="column" alignItems="center">
          </Box>
        </Grid> */}

        {/* RIGHT: report info */}
        <Grid item xs={12} sm={6}>
          <Paper elevation={4} sx={{ backgroundColor: colors.primary[400], p: 3, borderRadius: 2, maxWidth: "1000px", width: "100%" }}>
            <Grid container spacing={20}>
              <Grid item xs={12} sm={6}>
                <Typography variant="h6">Report ID:</Typography>
                <Typography>{report.id}</Typography>
                <Typography variant="h6" mt={2}>Company (Sender):</Typography>
                <Typography>{report.sellerName}</Typography>
                <Typography variant="body2">ID: {report.sellerId}</Typography>
                <Typography variant="h6" mt={2}>Project:</Typography>
                <Typography>{report.projectName}</Typography>
                <Typography variant="h6" mt={2}>Reporting Period:</Typography>
                <Typography>{report.period}</Typography>
                <Typography variant="h6" mt={2}>Total Energy:</Typography>
                <Typography>{report.totalEnergy}</Typography>
              </Grid>

              <Grid item xs={12} sm={6}>
                <Typography variant="h6">Total CO₂:</Typography>
                <Typography>{report.totalCo2}</Typography>
                <Typography variant="h6" mt={2}>Submission Date:</Typography>
                <Typography>
                  {report.submittedAt
                    ? new Date(report.submittedAt).toLocaleString("en-GB", {
                      day: "2-digit",
                      month: "2-digit",
                      year: "numeric",
                      hour: "2-digit",
                      minute: "2-digit",
                      second: "2-digit",
                    })
                    : "—"}
                </Typography>

                <Typography variant="h6" mt={2}>Attachment:</Typography>
                {report.uploadOriginalFilename ? (
                  <Button
                    variant="outlined"
                    color="info"
                    onClick={() => window.open(report.uploadStorageUrl, "_blank")}
                  >
                    Download
                  </Button>
                ) : (
                  <Typography>—</Typography>
                )}
                <Typography variant="h6" mt={2}>Status:</Typography>
                <Typography sx={{ color: statusColor, fontWeight: 600 }}>
                  {report.status?.replace("_", " ")}
                </Typography>

                {report.status === "REJECTED" && (
                  <Box mt={2}>
                    <Typography variant="h6">Note:</Typography>
                    <TextField
                      fullWidth
                      multiline
                      rows={3}
                      placeholder="Enter rejection reason..."
                      value={note}
                      onChange={(e) => setNote(e.target.value)}
                      sx={{ mt: 1 }}
                    />
                  </Box>
                )}
              </Grid>
            </Grid>

            {/* Actions */}
            <Box display="flex" justifyContent="flex-end" alignItems="center" gap={2} mt={4} flexWrap="nowrap">
              {!actionTaken && (
                <>
                  <Button variant="contained" color="success" onClick={() => handleUpdate(true)}>
                    Approved
                  </Button>
                  <Button variant="contained" color="error" onClick={() => handleUpdate(false)}>
                    Rejected
                  </Button>
                </>
              )}
              <Button variant="contained" color="secondary" onClick={handleAnalyzeByAI} disabled={aiLoading}>
                {aiLoading ? "Analyzing..." : "Analyze by AI"}
              </Button>
              <Button variant="contained" color="warning" onClick={handleAnalyzeData} disabled={dataLoading}>
                {dataLoading ? "Analyzing..." : "Analyze Data"}
              </Button>
              <Button variant="outlined" color="info" onClick={handleOpenDetails}>
                View details
              </Button>
              <Button variant="outlined" color="inherit" onClick={() => navigate(-1)}>
                Back
              </Button>
            </Box>
          </Paper>
        </Grid>
      </Grid>

      {/* Details Dialog */}
      <Dialog open={detailsOpen} onClose={handleCloseDetails} fullWidth maxWidth="md">
        <DialogTitle>Report #{report.id} – Vehicle details</DialogTitle>
        <DialogContent dividers>
          {detailsLoading ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : details?.length ? (
            <>
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
                      <TableCell>{row.vehicleId ?? "-"}</TableCell>
                      <TableCell>{row.period}</TableCell>
                      <TableCell align="right">{row.totalEnergy}</TableCell>
                      <TableCell align="right">{row.co2Kg}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <TablePagination
                component="div"
                count={detailsTotal}
                page={detailsPage}
                onPageChange={handleChangeDetailsPage}
                rowsPerPage={detailsSize}
                onRowsPerPageChange={handleChangeDetailsRowsPerPage}
                rowsPerPageOptions={[10, 20, 50, 100]}
                sx={{
                  mt: 1,
                  '& .MuiTablePagination-toolbar': {
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    flexWrap: 'nowrap',
                    minHeight: '52px',
                  },
                  '& .MuiTablePagination-displayedRows': {
                    margin: 0,
                    lineHeight: 1.6,
                  },
                  '& .MuiTablePagination-actions': {
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  },
                  '& .MuiTablePagination-selectLabel, & .MuiTablePagination-input': {
                    marginBottom: 0,
                    whiteSpace: 'nowrap',
                  },
                }}
              />
            </>
          ) : (
            <Typography variant="body2">No details found.</Typography>
          )}
          <Box display="flex" justifyContent="flex-end" mt={2}>
            <Button onClick={handleCloseDetails}>Close</Button>
          </Box>
        </DialogContent>
      </Dialog>

      {/* --- AI Analysis Popup --- */}
      <Dialog open={openAIDialog} onClose={() => setOpenAIDialog(false)} fullWidth maxWidth="sm">
        <DialogTitle>AI Evaluation Summary</DialogTitle>
        <DialogContent dividers>
          {aiLoading ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : aiResult ? (
            <Box>
              <Typography variant="body1">
                <strong>Version:</strong> {aiResult.aiVersion || "v?.?"}
              </Typography>
              <Typography variant="body1">
                <strong>Score:</strong> {aiResult.aiPreScore ?? "—"} / 10
              </Typography>
              <Typography sx={{ mt: 2, whiteSpace: "pre-wrap" }}>
                {aiResult.aiPreNotes || "No AI notes available."}
              </Typography>
            </Box>
          ) : (
            <Typography>No AI result available.</Typography>
          )}
        </DialogContent>
        {/* Exit popup */}
        <DialogActions>
          <Button onClick={() => setOpenAIDialog(false)} color="primary">
            Close
          </Button>
        </DialogActions>
      </Dialog>

      {/*  Data Analysis Popup (formatted)  */}
      <Dialog
        open={openDataDialog}
        onClose={() => setOpenDataDialog(false)}
        fullWidth
        maxWidth="md"
      >
        <DialogTitle>Data Analysis Summary</DialogTitle>
        <DialogContent dividers>
          {dataLoading ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : dataAnalysis?.response ? (
            <Box>
              {/* === Tổng quan === */}
              <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
                Overview
              </Typography>
              <Grid container spacing={2} sx={{ mb: 2 }}>
                <Grid item xs={6}>
                  <Typography>Report ID: {dataAnalysis.response.reportId}</Typography>
                  <Typography>Version: {dataAnalysis.response.version}</Typography>
                  <Typography>
                    Data Quality: {dataAnalysis.response.dataQualityScore} /{" "}
                    {dataAnalysis.response.dataQualityMax}
                  </Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography>
                    Fraud Risk: {dataAnalysis.response.fraudRiskScore} /{" "}
                    {dataAnalysis.response.fraudRiskMax}
                  </Typography>
                  <Typography>
                    Fraud Reasons:{" "}
                    {dataAnalysis.response.fraudReasons?.length
                      ? dataAnalysis.response.fraudReasons.join(", ")
                      : "None"}
                  </Typography>
                </Grid>
              </Grid>

              {/* === Bảng chi tiết rule === */}
              <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
                Rule Details
              </Typography>
              <Table size="small" sx={{ mt: 1 }}>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Score</TableCell>
                    <TableCell>Message</TableCell>
                    <TableCell>Evidence</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {dataAnalysis.response.details.map((rule) => (
                    <TableRow
                      key={rule.ruleId}
                      sx={{
                        backgroundColor:
                          rule.severity === "ERROR"
                            ? "#ffebee"
                            : rule.severity === "WARN"
                              ? "#fff8e1"
                              : "transparent",
                      }}
                    >
                      <TableCell>{rule.ruleId}</TableCell>
                      <TableCell>{rule.name}</TableCell>
                      <TableCell sx={{ whiteSpace: "nowrap" }}>
                        {rule.score} / {rule.maxScore}
                      </TableCell>
                      <TableCell>{rule.message}</TableCell>
                      <TableCell
                        sx={{
                          maxWidth: 300,
                          wordBreak: "break-word",
                          whiteSpace: "normal",
                        }}
                      >
                        {rule.evidence}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>

              </Table>

              {/* === Tổng điểm cuối === */}
              <Box mt={3} p={2} bgcolor="#f5f5f5" borderRadius={2}>
                <Typography fontWeight="bold">
                  Total Score:{" "}
                  {dataAnalysis.response.dataQualityScore +
                    dataAnalysis.response.fraudRiskScore}{" "}
                  /{" "}
                  {dataAnalysis.response.dataQualityMax +
                    dataAnalysis.response.fraudRiskMax}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Response: {dataAnalysis.responseStatus?.responseMessage}
                </Typography>
              </Box>
            </Box>
          ) : (
            <Typography>No data analysis available.</Typography>
          )}
        </DialogContent>
        {/* Exit popup */}
        <DialogActions>
          <Button onClick={() => setOpenDataDialog(false)} color="primary">
            Close
          </Button>
        </DialogActions>
      </Dialog>


      {/* Snackbar */}
      {SnackbarComponent}
    </Box>
  );
};

export default ViewReport;
