import {
  Box,
  Typography,
  Paper,
  TextField,
  Button,
  Divider,
  useTheme,
  Grid,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutline";
import CancelOutlinedIcon from "@mui/icons-material/CancelOutlined";
import { useEffect, useState } from "react";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import {
  approveReportByAdmin,
  getReportByIdAdmin,
  getCreditPreviewByReportId,
  issueCreditsForReport,
} from "@/apiAdmin/reportAdmin.js";
import { useSnackbar } from "@/hooks/useSnackbar.jsx";

const ViewReport = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();
  const { id } = useParams();
  const { showSnackbar, SnackbarComponent } = useSnackbar();

  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [note, setNote] = useState("");

  const [approved, setApproved] = useState(false);
  const [issued, setIssued] = useState(false);

  const [openPreview, setOpenPreview] = useState(false);
  const [previewData, setPreviewData] = useState(null);
  const [creditAmount, setCreditAmount] = useState("");
  const [loadingPreview, setLoadingPreview] = useState(false);
  const [issuing, setIssuing] = useState(false);

  // totalCredits: số tín chỉ được tính bởi preview API (mức tối đa có thể cấp)
  const [totalCredits, setTotalCredits] = useState(0);

  useEffect(() => {
    const fetchReport = async () => {
      try {
        const res = await getReportByIdAdmin(id);
        const data = res.response || res.responseData || res;
        setReport(data);

        const status = data.status?.trim().toUpperCase();
        if (status === "ADMIN_APPROVED") {
          setApproved(true);
          setIssued(false);
        } else if (status === "ISSUED" || status === "CREDIT_ISSUED") {
          setApproved(true);
          setIssued(true);
        } else if (status === "REJECTED") {
          setApproved(false);
          setIssued(false);
        } else {
          setApproved(false);
          setIssued(false);
        }
      } catch (err) {
        console.error("Error fetching report:", err);
        showSnackbar("error", "Failed to load report details!");
      } finally {
        setLoading(false);
      }
    };
    fetchReport();
  }, [id, showSnackbar]);

  // Mở preview (gọi API preview, set totalCredits, hiển thị dialog)
  const handleOpenPreview = async () => {
  try {
    setLoadingPreview(true);
    const res = await getCreditPreviewByReportId(id);
    const data = res.response || res.responseData || res;

    setPreviewData(data);
    setTotalCredits(data.creditsCount || 0);
    setCreditAmount(String(data.creditsCount || 0));
    setOpenPreview(true);

  } catch (err) {
    console.error("Preview error:", err);

    const message =
      err?.response?.data?.responseStatus?.responseMessage ||
      err?.response?.data?.message ||
      err?.message;

    if (message?.includes("already issued")) {
      showSnackbar("error", "Credits already issued for this report.");
      // cập nhật FE locked luôn
      setIssued(true);
      return;
    }

    showSnackbar("error", "Failed to load credit preview!");
  } finally {
    setLoadingPreview(false);
  }
};

  const handleApproval = async (isApproved) => {
    try {
      const res = await approveReportByAdmin(id, isApproved, note);
      setReport((prev) => ({
        ...prev,
        ...res.response,
        status: isApproved ? "ADMIN_APPROVED" : "REJECTED",
      }));

      showSnackbar(
        "success",
        isApproved ? "Report approved successfully!" : "Report rejected successfully!"
      );

      if (isApproved) setApproved(true);
      else setApproved(false);
    } catch (err) {
      console.error("Approval error:", err);
      showSnackbar("error", "Failed to update report status!");
    }
  };

  // Issue credits
  const handleIssueCredit = async () => {
    const amount = Number(creditAmount);

    // validation
    if (isNaN(amount) || amount <= 0) {
      showSnackbar("error", "Invalid credit amount!");
      return;
    }
    if (amount > totalCredits) {
      showSnackbar("error", `Amount cannot exceed calculated total credits (${totalCredits})`);
      return;
    }

    try {
      setIssuing(true);
      showSnackbar("info", "Issuing credits... please wait");

      //API (POST /api/v1/credits/issued/{reportId} with { approvedCredits })
      await issueCreditsForReport(id, amount);

      // Cập nhật trạng thái báo cáo sau khi cấp tín chỉ
      setIssued(true);
      setReport((prev) => ({
        ...prev,
        status: "CREDIT_ISSUED",
      }));

      showSnackbar("success", `Issued ${amount} credits successfully!`);
      setCreditAmount("");
      setOpenPreview(false);

      //quay về trang quản lý tín chỉ sau 1s
      setTimeout(() => navigate("/admin/credit_management"), 1000);
    } catch (err) {
      console.error("Issue credit error:", err);
      // lay message từ response nếu có
      const message =
        err?.response?.data?.responseStatus?.responseDesc ||
        err?.response?.data?.message ||
        err?.message ||
        "Failed to issue credits!";
      showSnackbar("error", message);
    } finally {
      setIssuing(false);
    }
  };

  if (loading) return <Typography m={3}>Loading...</Typography>;
  if (!report) return <Typography m={3}>Report not found.</Typography>;

  const locked = ["CREDIT_ISSUED", "PAID_OUT"].includes(
    report?.status?.trim().toUpperCase()
  );

  return (
    <Box m="20px" sx={{ marginLeft: "290px" }}>
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

        <Grid container spacing={3} mb={2}>
          <Grid item xs={12} md={6}>
            <TextField label="Report ID" value={report.id} fullWidth InputProps={{ readOnly: true }} />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField label="Seller Name" value={report.sellerName || ""} fullWidth InputProps={{ readOnly: true }} />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField label="Project Name" value={report.projectName || ""} fullWidth InputProps={{ readOnly: true }} />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField label="Reporting Period" value={report.period || ""} fullWidth InputProps={{ readOnly: true }} />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField label="Total Energy" value={report.totalEnergy || 0} fullWidth InputProps={{ readOnly: true }} />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField label="Total CO₂" value={report.totalCo2 || 0} fullWidth InputProps={{ readOnly: true }} />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField label="Verified by CVA" value={report.verifiedByCvaName || ""} fullWidth InputProps={{ readOnly: true }} />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField label="Status" value={report.status || ""} fullWidth InputProps={{ readOnly: true }} />
          </Grid>
          <Grid item xs={12}>
            <TextField
              label="Admin Note"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              fullWidth
              multiline
              rows={1}
              placeholder="Enter note for approval/rejection..."
            />
          </Grid>
        </Grid>

        <Divider sx={{ my: 3, borderColor: colors.grey[700] }} />

        {/* Buttons */}
        <Box display="flex" justifyContent="space-between" mt={3}>
          <Button
            variant="outlined"
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate(`/admin/view_report/${report.id}`)}
            sx={{
              borderColor: colors.blueAccent[400],
              color: colors.blueAccent[400],
              textTransform: "none",
            }}
          >
            Back to View
          </Button>

          <Box display="flex" gap={2}>

            {/* Nếu report đã hoàn thành cấp tín chỉ → ẩn toàn bộ nút */}
            {!locked && !approved && !issued && (
              <>
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
              </>
            )}

            {!locked && approved && !issued && (
              <Button
                variant="contained"
                color="primary"
                onClick={handleOpenPreview}
                disabled={loadingPreview}
                sx={{ textTransform: "none" }}
              >
                {loadingPreview ? "Loading Preview..." : "Issue Credit"}
              </Button>
            )}
          </Box>

        </Box>
      </Paper>

      {/* Dialog Preview Credit */}
      <Dialog open={openPreview} onClose={() => setOpenPreview(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Credit Preview</DialogTitle>
        <DialogContent>
          {loadingPreview ? (
            <Typography>Loading preview data...</Typography>
          ) : previewData ? (
            <Box display="flex" flexDirection="column" gap={2} mt={1}>
              <TextField label="Total tCO₂e" value={previewData.totalTco2e || 0} fullWidth InputProps={{ readOnly: true }} />
              <TextField label="Total Credits (calculated)" value={totalCredits} fullWidth InputProps={{ readOnly: true }} />

              <Typography variant="body2" color="textSecondary">
                Note: issuance is one-time. After confirming, credits will be issued and the report status will be updated.
              </Typography>

              <TextField
                label="Credits to Issue"
                type="number"
                value={creditAmount}
                 onChange={(e) => setCreditAmount(e.target.value)}
                fullWidth
                error={isNaN(Number(creditAmount)) || Number(creditAmount) <= 0 || Number(creditAmount) > totalCredits}
                helperText={
                  !creditAmount
                    ? "Enter credits to issue"
                    : Number(creditAmount) <= 0
                      ? "Must be greater than 0"
                      : Number(creditAmount) > totalCredits
                        ? `Cannot exceed calculated total credits (${totalCredits})`
                        : ""
                }
              />
            </Box>
          ) : (
            <Typography>No preview data available.</Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenPreview(false)} color="inherit">
            Cancel
          </Button>
          <Button
            onClick={handleIssueCredit}
            color="primary"
            variant="contained"
            disabled={
              issuing ||
              !previewData ||
              isNaN(Number(creditAmount)) ||
              Number(creditAmount) <= 0 ||
              Number(creditAmount) > totalCredits
            }
          >
            {issuing ? "Processing..." : "Confirm Issue"}
          </Button>
        </DialogActions>
      </Dialog>

      {SnackbarComponent}
    </Box>
  );
};

export default ViewReport;
