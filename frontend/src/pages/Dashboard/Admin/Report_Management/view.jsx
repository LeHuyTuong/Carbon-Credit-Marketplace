import {
  Box,
  Typography,
  Paper,
  TextField,
  Button,
  Divider,
  useTheme,
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
import { issueCredits } from "@/apiAdmin/creditAdmin.js";
import { useSnackbar } from "@/hooks/useSnackbar.jsx";

const ViewReport = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();
  const { id } = useParams();

  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [note, setNote] = useState("");
  const { showSnackbar, SnackbarComponent } = useSnackbar();


  //  State điều khiển hiển thị nút
  const [approved, setApproved] = useState(false);
  const [issued, setIssued] = useState(false);

  useEffect(() => {
    const fetchReport = async () => {
      try {
        const res = await getReportByIdAdmin(id);
        const data = res.response || res.responseData || res;
        setReport(data);

        //  Chuẩn hóa status
        const status = data.status?.trim().toUpperCase();
        console.log("Report status:", status);

        //  Xử lý theo API thực tế
        if (status === "ADMIN_APPROVED") setApproved(true);
        if (status === "ISSUED") {
          setApproved(true); // vì issued chỉ xảy ra sau khi approved
          setIssued(true);
        }
        if (status === "REJECTED") {
          setApproved(false);
          setIssued(false);
        }
        //test nếu sai
        // const rawStatus = (data.status || "").trim().toUpperCase();

        // switch (rawStatus) {
        //   case "ADMIN_APPROVED":
        //     setApproved(true);
        //     setIssued(false);
        //     break;
        //   case "ISSUED":
        //     setApproved(true);
        //     setIssued(true);
        //     break;
        //   case "REJECTED":
        //     setApproved(false);
        //     setIssued(false);
        //     break;
        //   default:
        //     setApproved(false);
        //     setIssued(false);
        //     break;
        // }

      } catch (err) {
        console.error("Error fetching report:", err);
      } finally {
        setLoading(false);
      }
    };
    fetchReport();
  }, [id]);

  const handleApproval = async (isApproved) => {
    try {
      const res = await approveReportByAdmin(id, isApproved, note);
      setReport((prev) => ({
        ...prev,
        ...res.response,
        status: isApproved ? "Approved" : "Rejected",
      }));

      showSnackbar(
        "success",
        isApproved
          ? "Report approved successfully!"
          : "Report rejected successfully!"
      );

      if (isApproved) setApproved(true);
    } catch (err) {
      console.error(err);
      showSnackbar("error", "Failed to update report status!");
    }
  };

  const [issuing, setIssuing] = useState(false); // trạng thái đang cấp credit

  const handleIssueCredit = async () => {
    try {
      setIssuing(true); // disable nút
      showSnackbar("info", "Issuing credits... please wait (about 8 seconds)");
      await issueCredits(id);
      showSnackbar("success", "Credits issued successfully!");
      setIssued(true); // ẩn nút vĩnh viễn sau khi cấp thành công

      // Chuyển trang nhẹ sau 1s
      setTimeout(() => {
        navigate("/admin/credit_management");
      }, 1000);
    } catch (err) {
      console.error(err);
      showSnackbar("error", "Failed to issue credits!");
    } finally {
      // Không reset issuing nếu đã issued (để nút biến mất)
      if (!issued) setIssuing(false);
    }
  };

  if (loading) return <Typography m={3}>Loading...</Typography>;
  if (!report) return <Typography m={3}>Report not found.</Typography>;

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
            {/* Khi chưa duyệt thì hiện 2 nút Approve/Reject */}
            {!approved && !issued && (
              <>
                <Button
                  variant="contained"
                  color="success"
                  startIcon={<CheckCircleOutlineIcon />}
                  onClick={() => handleApproval(true)}
                  sx={{ textTransform: "none" }}
                >
                  Approved
                </Button>
                <Button
                  variant="contained"
                  color="error"
                  startIcon={<CancelOutlinedIcon />}
                  onClick={() => handleApproval(false)}
                  sx={{ textTransform: "none" }}
                >
                  Rejected
                </Button>
              </>
            )}

            {/* Khi đã approved thì hiện Issue Credit */}
            {approved && !issued && (
              <Button
                variant="contained"
                color="primary"
                onClick={handleIssueCredit}
                disabled={issuing}
                sx={{ textTransform: "none" }}
              >
                {issuing ? "Processing..." : "Issue Credit"}
              </Button>
            )}

          </Box>
        </Box>
      </Paper>

      {/* Snackbar */}
      {SnackbarComponent}
    </Box>
  );
};

export default ViewReport;
