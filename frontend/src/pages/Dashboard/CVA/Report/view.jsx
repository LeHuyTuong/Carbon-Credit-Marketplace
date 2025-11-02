// src/pages/Dashboard/CVA/Report/ViewReport.jsx
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

  // Details dialog + pagination state
  const [detailsOpen, setDetailsOpen] = useState(false);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [details, setDetails] = useState([]);
  const [detailsPage, setDetailsPage] = useState(0);
  const [detailsSize, setDetailsSize] = useState(20);
  const [detailsTotal, setDetailsTotal] = useState(0);

  const [actionTaken, setActionTaken] = useState(false);

  // AI Evaluation
  const [aiResult, setAiResult] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);

  // Formula image
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

  // ===== Load report =====
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
            setSnackbarSeverity("error");
            setSnackbarMsg("Failed to load report data!");
            setOpenSnackbar(true);
          }
        } catch (err) {
          console.error("Failed to fetch report:", err);
          setSnackbarSeverity("error");
          setSnackbarMsg(err.message || "Failed to load report data!");
          setOpenSnackbar(true);
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

  // ===== Update status =====
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
      setSnackbarMsg(err.message || "Failed to update status!");
    } finally {
      setOpenSnackbar(true);
      setLoading(false);
    }
  };

  // ===== Fetch details (paginated) =====
  const fetchDetails = useCallback(
    async (page = detailsPage, size = detailsSize) => {
      if (!report?.id) return;
      setDetailsLoading(true);
      try {
        // getReportDetails phải hỗ trợ truyền {page, size}
        const pageRes = await getReportDetails(report.id, { page, size });
        // Chuẩn hóa (BE có thể trả về TuongCommonResponse/ CommonResponse/ raw Page)
        const pageObj =
          pageRes?.response ||
          pageRes?.responseData ||
          pageRes?.data ||
          pageRes ||
          {};

        const rows = pageObj.content ?? [];
        const total = pageObj.totalElements ?? rows.length ?? 0;

        // Chuẩn hóa field vehicleId (fallback vehiclePlate)
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
        setSnackbarSeverity("error");
        setSnackbarMsg(e.message || "Failed to load details!");
        setOpenSnackbar(true);
      } finally {
        setDetailsLoading(false);
      }
    },
    [report?.id, detailsPage, detailsSize]
  );

  const handleOpenDetails = async () => {
    if (!report?.id) return;
    setDetailsOpen(true);
    await fetchDetails(0, detailsSize); // mở lần đầu luôn page=0
  };
  const handleCloseDetails = () => setDetailsOpen(false);

  const handleChangeDetailsPage = async (_e, newPage) => {
    await fetchDetails(newPage, detailsSize);
  };
  const handleChangeDetailsRowsPerPage = async (e) => {
    const newSize = parseInt(e.target.value, 10);
    await fetchDetails(0, newSize);
  };

  // ===== AI analyze =====
  const handleAnalyzeByAI = async () => {
    if (!report?.id) return;
    setAiLoading(true);
    setSnackbarMsg("AI is analyzing this report...");
    setSnackbarSeverity("info");
    setOpenSnackbar(true);

    try {
      const result = await analyzeReportByAI(report.id);
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

  const handleCloseSnackbar = () => setOpenSnackbar(false);

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

  const statusColor = colorMap[report.status] || colors.grey[300];

  return (
    <Box m="20px">
      <Header title="REPORT DETAIL" subtitle={`Details of Report ${report.id}`} />

      <Grid container spacing={2}>
        {/* Left: formula image */}
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

        {/* Right: report info */}
        <Grid item xs={12} sm={6}>
          <Paper
            elevation={4}
            sx={{ backgroundColor: colors.primary[400], p: 3, borderRadius: 2, boxShadow: 3 }}
          >
            <Grid container spacing={2}>
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
                <Typography sx={{ color: statusColor, fontWeight: 600, textTransform: "capitalize" }}>
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

            {/* Actions */}
            <Box display="flex" justifyContent="flex-end" alignItems="center" gap={2} mt={4} flexWrap="nowrap">
              {!actionTaken && (
                <>
                  <Button variant="contained" color="success" onClick={() => handleUpdate(true)}>
                    Approve
                  </Button>
                  <Button variant="contained" color="error" onClick={() => handleUpdate(false)}>
                    Reject
                  </Button>
                </>
              )}
              <Button variant="contained" color="secondary" onClick={handleAnalyzeByAI} disabled={aiLoading}>
                {aiLoading ? "Analyzing..." : "Analyze by AI"}
              </Button>
              <Button variant="outlined" color="info" onClick={handleOpenDetails}>
                View details
              </Button>
              <Button variant="outlined" color="inherit" onClick={() => navigate(-1)}>
                Back
              </Button>
            </Box>

            {/* AI Evaluation */}
            {aiResult && (
              <Paper
                elevation={5}
                sx={{
                  mt: 3,
                  p: 3,
                  borderRadius: 2,
                  backgroundColor: theme.palette.mode === "dark" ? colors.primary[400] : "#fff",
                  color: theme.palette.mode === "dark" ? "#fff" : "#222",
                  boxShadow: "0 2px 8px rgba(0,0,0,0.15)",
                  position: "relative",
                  whiteSpace: "pre-line",
                }}
              >
                {/* Header */}
                <Box
                  display="flex"
                  justifyContent="space-between"
                  alignItems="center"
                  mb={2}
                  sx={{
                    borderBottom:
                      theme.palette.mode === "dark"
                        ? "1px solid rgba(255,255,255,0.15)"
                        : "1px solid rgba(0,0,0,0.1)",
                    pb: 1,
                  }}
                >
                  <Typography
                    variant="h6"
                    sx={{
                      color: "#E91E63",
                      fontWeight: 700,
                      display: "flex",
                      alignItems: "center",
                      gap: 1,
                    }}
                  >
                    AI Evaluation Summary
                  </Typography>
                  <Button variant="outlined" size="small" color="error" onClick={() => setAiResult(null)}>
                    CLOSE
                  </Button>
                </Box>

                {/* Risk banner */}
                {((aiResult.aiPreScore && aiResult.aiPreScore < 6) ||
                  /CRITICAL|HIGH/i.test(aiResult.aiPreNotes || "")) && (
                    <Box
                      sx={{
                        backgroundColor: "#ffebee",
                        border: "1px solid #f44336",
                        color: "#b71c1c",
                        borderRadius: 2,
                        p: 2,
                        mb: 2,
                        display: "flex",
                        alignItems: "center",
                        gap: 1,
                      }}
                    >
                      <Typography sx={{ fontWeight: 700, fontSize: "1rem" }}>
                        CRITICAL WARNING:
                      </Typography>
                      <Typography variant="body2">
                        AI detected serious anomalies or potential fraud risk in this report. Immediate manual review is
                        required.
                      </Typography>
                    </Box>
                  )}

                {/* Version + Score */}
                <Box
                  sx={{
                    backgroundColor: theme.palette.mode === "dark" ? "rgba(255,255,255,0.1)" : "#fafafa",
                    border: "1px solid rgba(0,0,0,0.08)",
                    p: 2,
                    borderRadius: 2,
                    mb: 2,
                  }}
                >
                  <Typography variant="body1" sx={{ mb: 0.5 }}>
                    <strong>Version:</strong> {aiResult.aiVersion || "v?.?"}
                  </Typography>
                  <Typography variant="body1">
                    <strong>Score:</strong>{" "}
                    <Typography
                      component="span"
                      sx={{
                        fontWeight: "bold",
                        color:
                          aiResult.aiPreScore >= 8
                            ? "#2E7D32"
                            : aiResult.aiPreScore >= 6
                              ? "#FB8C00"
                              : "#D32F2F",
                      }}
                    >
                      {aiResult.aiPreScore ?? "—"} / 10
                    </Typography>
                  </Typography>
                </Box>

                {/* Notes */}
                <Typography
                  variant="subtitle1"
                  sx={{
                    color: "#C2185B",
                    mb: 1,
                    fontWeight: 600,
                    display: "flex",
                    alignItems: "center",
                    gap: 1,
                  }}
                >
                  Detailed Analysis:
                </Typography>
                <Box
                  sx={{
                    backgroundColor: theme.palette.mode === "dark" ? "rgba(255,255,255,0.05)" : "#fff",
                    border: "1px solid rgba(0,0,0,0.1)",
                    borderRadius: 2,
                    p: 2,
                    lineHeight: 1.6,
                    fontSize: "0.95rem",
                  }}
                >
                  <Typography component="pre" sx={{ whiteSpace: "pre-wrap", fontFamily: "inherit" }}>
                    {aiResult.aiPreNotes || "No AI notes available."}
                  </Typography>
                </Box>
                {/* Optional sections */}
                {aiResult.aiRisk && (
                  <>
                    <Typography
                      variant="subtitle1"
                      sx={{
                        color: "#E65100",
                        mt: 2,
                        mb: 1,
                        fontWeight: 600,
                        display: "flex",
                        alignItems: "center",
                        gap: 1,
                      }}
                    >
                      Risk Summary:
                    </Typography>
                    <Box
                      sx={{
                        backgroundColor: theme.palette.mode === "dark" ? "rgba(255,255,255,0.05)" : "#fff",
                        border: "1px solid rgba(0,0,0,0.1)",
                        borderRadius: 2,
                        p: 2,
                      }}
                    >
                      <Typography component="pre" sx={{ whiteSpace: "pre-wrap" }}>
                        {aiResult.aiRisk}
                      </Typography>
                    </Box>
                  </>
                )}

                {aiResult.aiSuggestion && (
                  <>
                    <Typography
                      variant="subtitle1"
                      sx={{
                        color: "#0277BD",
                        mt: 2,
                        mb: 1,
                        fontWeight: 600,
                        display: "flex",
                        alignItems: "center",
                        gap: 1,
                      }}
                    >
                      Suggestions for CVA:
                    </Typography>
                    <Box
                      sx={{
                        backgroundColor: theme.palette.mode === "dark" ? "rgba(255,255,255,0.05)" : "#fff",
                        border: "1px solid rgba(0,0,0,0.1)",
                        borderRadius: 2,
                        p: 2,
                      }}
                    >
                      <Typography component="pre" sx={{ whiteSpace: "pre-wrap" }}>
                        {aiResult.aiSuggestion}
                      </Typography>
                    </Box>
                  </>
                )}
              </Paper>
            )}
          </Paper>
        </Grid>
      </Grid>

      {/* Details dialog */}
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