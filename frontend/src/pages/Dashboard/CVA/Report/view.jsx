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
  Divider
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
  getCompanyKYCProfile,
  getReportRules
} from "@/apiCVA/reportCVA.js";
import { analyzeReportByAI, analyzeReportData, } from "@/apiCVA/aiCVA.js";
import { useSnackbar } from "@/hooks/useSnackbar.jsx";


const ViewReport = ({ report: initialReport }) => {
  const navigate = useNavigate();
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const { id } = useParams();
  //snackbar component
  const { showSnackbar, SnackbarComponent } = useSnackbar();
  //kyc company
  const [companyProfile, setCompanyProfile] = useState(null);

  //report
  const [report, setReport] = useState(initialReport || null);
  const [note, setNote] = useState(initialReport?.note || "");
  const [loading, setLoading] = useState(!initialReport);

  //details
  const [detailsOpen, setDetailsOpen] = useState(false);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [details, setDetails] = useState([]);
  const [detailsPage, setDetailsPage] = useState(0);
  const [detailsSize, setDetailsSize] = useState(20);
  const [detailsTotal, setDetailsTotal] = useState(0);
  const [actionTaken, setActionTaken] = useState(false);

  //rules
  const [rulesOpen, setRulesOpen] = useState(false);
  const [rulesLoading, setRulesLoading] = useState(false);
  const [rulesData, setRulesData] = useState([]);


  // AI + Data
  const [aiResult, setAiResult] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [dataAnalysis, setDataAnalysis] = useState(null);
  const [dataLoading, setDataLoading] = useState(false);

  // Dialogs for popup display
  const [openAIDialog, setOpenAIDialog] = useState(false);
  const [openDataDialog, setOpenDataDialog] = useState(false);

  const colorMap = {

    SUBMITTED: "#42A5F5",
    CVA_APPROVED: "#4CAF50",
    ADMIN_APPROVED: "#2E7D32",
    CVA_REJECTED: "#E53935",
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

  // Load KYC profile của công ty gửi report
  useEffect(() => {
    if (report?.sellerId) {
      (async () => {
        try {
          const data = await getCompanyKYCProfile(report.sellerId);
          setCompanyProfile(data);
        } catch (err) {
          console.error("Failed to fetch company KYC profile:", err);
          showSnackbar("error", "Failed to load company profile!");
        }
      })();
    }
  }, [report?.sellerId]);


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
        //Gọi API lấy details
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
  // Open details dialog
  const handleOpenDetails = async () => {
    if (!report?.id) return;
    setDetailsOpen(true);
    await fetchDetails(0, detailsSize);
  };
  // Close details dialog
  const handleCloseDetails = () => setDetailsOpen(false);
  // Pagination handlers
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
      // Gọi API analyze
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
      // Gọi API analyze data
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
  //rules
  const handleOpenRules = async () => {
    setRulesOpen(true);
    setRulesLoading(true);
    try {
      //Gọi API lấy rules
      const data = await getReportRules();
      setRulesData(data || []);
    } catch (err) {
      console.error("Failed to load rules:", err);
      showSnackbar("error", err.message || "Failed to load rules!");
    } finally {
      setRulesLoading(false);
    }
  };
  const handleCloseRules = () => setRulesOpen(false);


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
    <Box m="20px" sx={{ marginLeft: "290px", maxWidth: "1200px", width: "100%", }} textAlign="left" >
      <Header title="REPORT DETAIL" subtitle={`Details of Report ${report.id}`} />
      <Paper
        sx={{
          p: 2,
          mt: 2,
          borderRadius: 3,
          boxShadow: 4,
          backgroundColor: "#fafafa",
        }}
      >
        {/* REPORT INFORMATION  */}
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
          <Typography
            variant="h5"
            sx={{
              fontWeight: "bold",
              color: "#1976d2",
              fontSize: "1.2rem",
            }}
          >
            Report Information
          </Typography>

          <Button
            variant="contained"
            color="info"
            onClick={handleOpenRules}
            size="medium"
          >
            View Rules
          </Button>
        </Box>

        <Divider sx={{ mb: 3 }} />

        <Grid container spacing={10}>
          <Grid item xs={12} sm={6}>
            <Typography sx={{ fontSize: "1rem" }}>
              <b>Report ID:</b> {report.id || "—"}
            </Typography>
            <Typography sx={{ fontSize: "1rem" }}>
              <b>Company (Sender):</b> {report.sellerName || "—"}
            </Typography>
            <Typography sx={{ fontSize: "1rem" }}>
              <b>Project:</b> {report.projectName || "—"}
            </Typography>
            <Typography sx={{ fontSize: "1rem" }}>
              <b>Reporting Period:</b> {report.period || "—"}
            </Typography>
          </Grid>

          <Grid item xs={12} sm={6}>
            <Typography sx={{ fontSize: "1rem" }}>
              <b>Total Energy:</b> {report.totalEnergy || "—"}
            </Typography>
            <Typography sx={{ fontSize: "1rem" }}>
              <b>Total CO₂:</b> {report.totalCo2 || "—"}
            </Typography>
            <Typography sx={{ fontSize: "1rem" }}>
              <b>Submission Date:</b>{" "}
              {report.submittedAt
                ? new Date(report.submittedAt).toDateString("vi-VN")
                : "—"}
            </Typography>
            <Typography sx={{ fontSize: "1rem" }}>
              <b>Attachment:</b>{" "}
              {report.uploadOriginalFilename ? (
                <Button
                  href={report.uploadStorageUrl}
                  target="_blank"
                  variant="outlined"
                  color="info"
                  sx={{ textTransform: "none", fontSize: "0.9rem", ml: 1 }}
                >
                  View CSV File
                </Button>
              ) : (
                "—"
              )}
            </Typography>
            <Typography sx={{ fontSize: "1rem" }}>
              <b>Status:</b>{" "}
              <span style={{ color: statusColor, fontWeight: 600 }}>
                {report.status?.replace("_", " ") || "—"}
              </span>
            </Typography>
          </Grid>
        </Grid>

        {/* COMPANY KYC SECTION  */}
        {companyProfile && (
          <>
            <Divider sx={{ my: 4 }} />
            <Typography
              variant="h5"
              sx={{
                fontWeight: "bold",
                mb: 2,
                color: "#388e3c",
                fontSize: "1.2rem",
              }}
            >
              Company Registration
            </Typography>
            {/* Show company KYC info */}
            <Grid container spacing={10}>
              <Grid item xs={12} sm={6}>
                <Typography sx={{ fontSize: "1rem" }}>
                  <b>Company Name:</b> {companyProfile.companyName || "—"}
                </Typography>
                <Typography sx={{ fontSize: "1rem" }}>
                  <b>Business License:</b> {companyProfile.businessLicense || "—"}
                </Typography>
                <Typography sx={{ fontSize: "1rem" }}>
                  <b>Tax Code:</b> {companyProfile.taxCode || "—"}
                </Typography>
              </Grid>

              <Grid item xs={12} sm={6}>
                <Typography sx={{ fontSize: "1rem" }}>
                  <b>Address:</b> {companyProfile.address || "—"}
                </Typography>
                <Typography sx={{ fontSize: "1rem" }}>
                  <b>Created At:</b>{" "}
                  {new Date(companyProfile.createAt).toDateString("vi-VN")}
                </Typography>
              </Grid>
            </Grid>
          </>
        )}

        {/* ACTION BUTTONS */}
        <Box
          mt={5}
          display="flex"
          gap={2}
          justifyContent="flex-end"
          alignItems="center"
        >
          {!actionTaken && (
            <>
              <Button
                variant="contained"
                color="success"
                size="large"
                onClick={() => handleUpdate(true)}
              >
                Approved
              </Button>
              <Button
                variant="contained"
                color="error"
                size="large"
                onClick={() => handleUpdate(false)}
              >
                Rejected
              </Button>
            </>
          )}

          <Button
            variant="contained"
            color="secondary"
            size="large"
            onClick={handleAnalyzeByAI}
            disabled={aiLoading}
          >
            {aiLoading ? "Analyzing..." : "Analyze by AI"}
          </Button>

          <Button
            variant="contained"
            color="warning"
            size="large"
            onClick={handleAnalyzeData}
            disabled={dataLoading}
          >
            {dataLoading ? "Analyzing..." : "Analyze Data"}
          </Button>

          <Button
            variant="outlined"
            color="info"
            size="large"
            onClick={handleOpenDetails}
          >
            View details
          </Button>

          <Button
            variant="outlined"
            color="inherit"
            size="large"
            onClick={() => navigate(-1)}
          >
            Back
          </Button>
        </Box>
      </Paper>
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

      {/* AI Analysis Popup  */}
      <Dialog
        open={openAIDialog}
        onClose={() => setOpenAIDialog(false)}
        maxWidth={false}
        PaperProps={{
          sx: { width: "95vw", maxWidth: "1600px" }
        }}
      >
        <DialogTitle>AI Evaluation Summary</DialogTitle>

        <DialogContent dividers>
          {aiLoading ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : aiResult ? (
            <>
              {/*  BASIC INFO */}
              <Typography variant="body1">
                <strong>Version:</strong> {aiResult.aiVersion || "Unknown"}
              </Typography>

              <Typography variant="body1" sx={{ mt: 1 }}>
                <strong>Score:</strong> {aiResult.aiPreScore ?? aiResult.score ?? "—"} / 10
              </Typography>

              {/* MAIN NOTES */}
              <Typography sx={{ mt: 2, mb: 2, whiteSpace: "pre-wrap" }}>
                {(aiResult.aiPreNotes || aiResult.notes || "")
                  .split("\n")
                  .filter(line => !line.includes("|")) // ẨN RUN DETAIL TRONG TEXT
                  .join("\n") || "No AI notes available."}
              </Typography>



              {/* RUN DETAIL PARSED TABLE  */}


              {(() => {
                const runText =
                  aiResult.aiRunDetail ||
                  aiResult.aiPreNotes ||
                  aiResult.notes ||
                  "";

                if (!runText.trim()) return null;

                // --- PARSER ---
                const parsed = runText
                  .split("\n")
                  .filter((line) => line.includes("|"))
                  .map((line) => {
                    const parts = line.split("|").map((x) => x.trim());
                    return {
                      rule: parts[0] || "",
                      description: parts[1] || "",
                      score: parts[2] || "",
                      note: parts.slice(3).join(" | ") || "",
                    };
                  });

                if (parsed.length === 0) return null;

                return (
                  <Paper sx={{ mt: 3, p: 1 }} elevation={2}>
                    <Typography variant="h6" sx={{ mb: 1 }}>

                    </Typography>
                    {/*  Bảng chi tiết rule  */}
                    <Table>
                      <TableHead>
                        <TableRow>
                          <TableCell><strong>Rule</strong></TableCell>
                          <TableCell><strong>Description</strong></TableCell>
                          <TableCell sx={{ width: 100 }}><strong>Score</strong></TableCell>
                          <TableCell><strong>Note</strong></TableCell>
                        </TableRow>
                      </TableHead>

                      <TableBody>
                        {parsed.map((row, idx) => (
                          <TableRow key={idx}>
                            <TableCell>{row.rule}</TableCell>
                            <TableCell>{row.description}</TableCell>
                            <TableCell sx={{ width: 100 }}>{row.score}</TableCell>
                            <TableCell>{row.note}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </Paper>
                );
              })()}


              {/*  RUBRIC TABLE  */}


              {aiResult.rubricData && aiResult.rubricData.length > 0 && (
                <Paper sx={{ mt: 4, p: 1 }} elevation={2}>
                  <Typography variant="h6" sx={{ mb: 1 }}>
                    Rubric
                  </Typography>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell><strong>ID</strong></TableCell>
                        <TableCell><strong>Name</strong></TableCell>
                        <TableCell><strong>Max Score</strong></TableCell>
                        <TableCell><strong>Description</strong></TableCell>
                        <TableCell><strong>Scoring Guideline</strong></TableCell>
                        <TableCell><strong>Evidence Hint</strong></TableCell>
                      </TableRow>
                    </TableHead>

                    <TableBody>
                      {aiResult.rubricData.map((r, idx) => (
                        <TableRow key={idx}>
                          <TableCell>{r.ruleId}</TableCell>
                          <TableCell>{r.name}</TableCell>
                          <TableCell>{r.maxScore}</TableCell>
                          <TableCell>{r.description}</TableCell>
                          <TableCell>{r.scoringGuideline}</TableCell>
                          <TableCell>{r.evidenceHint}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </Paper>
              )}
            </>
          ) : (
            <Typography>No AI result available.</Typography>
          )}
        </DialogContent>

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
              {/* Tổng quan */}
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

              {/*  Bảng chi tiết rule  */}
              <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
                Rule Details
              </Typography>
              <Table size="small" sx={{ mt: 1 }}>
                <TableHead>
                  <TableRow>
                    <TableCell>Rule ID</TableCell>
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

      {/* Rules Dialog */}
      <Dialog
        open={rulesOpen}
        onClose={handleCloseRules}
        fullWidth
        maxWidth="lg"
      >
        <DialogTitle>Evaluation Rules</DialogTitle>
        <DialogContent dividers>
          {rulesLoading ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : rulesData.length ? (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Rule ID</TableCell>
                  <TableCell>Name</TableCell>
                  <TableCell sx={{ width: 100 }}>Max Score</TableCell>
                  <TableCell>Description</TableCell>
                  <TableCell>Scoring Guideline</TableCell>
                  <TableCell>Evidence Hint</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rulesData.map((r, idx) => (
                  <TableRow key={idx}>
                    <TableCell>{r.ruleId}</TableCell>
                    <TableCell>{r.name}</TableCell>
                    <TableCell>{r.maxScore}</TableCell>
                    <TableCell>{r.description}</TableCell>
                    <TableCell>{r.scoringGuideline}</TableCell>
                    <TableCell>{r.evidenceHint}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : (
            <Typography>No rules available.</Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseRules} color="primary">
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