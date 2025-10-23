import React, { useEffect, useState } from "react";
import {
  Box,
  Typography,
  CircularProgress,
  Paper,
  Button,
  TextField,
  Snackbar,
  Alert,
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import {
  updateApplicationDecision,
  getProjectApplicationByIdForCVA,
} from "@/apiCVA/registrationCVA.js";
import Header from "@/components/Chart/Header";

const ApplicationEdit = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [application, setApplication] = useState(null);
  const [loading, setLoading] = useState(true);
  const [finalReviewNote, setFinalReviewNote] = useState("");
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: "",
    severity: "success",
  });

  // 🔹 Fetch chi tiết application
  useEffect(() => {
    const fetchApplication = async () => {
      try {
        const data = await getProjectApplicationByIdForCVA(id);
        const appData = data?.responseData || data?.response || data;

        if (appData && (appData.id || appData.applicationId)) {
          setApplication(appData);
        } else {
          throw new Error("Application not found");
        }
      } catch (error) {
        console.error("❌ Error fetching application:", error);
        setSnackbar({
          open: true,
          message: "Failed to fetch application.",
          severity: "error",
        });
      } finally {
        setLoading(false);
      }
    };

    fetchApplication();
  }, [id]);

  // 🔹 Submit quyết định Approve / Reject
  const handleDecision = async (approved) => {
    try {
      const payload = {
        approved,
        note: finalReviewNote || (approved ? "Approved by CVA" : "Rejected by CVA"),
      };

      console.log(" Sending decision payload:", payload);
      const result = await updateApplicationDecision(id, payload);

      const code = result?.responseStatus?.responseCode;
      const msg = result?.responseStatus?.responseMessage;

      if (code === "00000000" || code === "200") {
        setSnackbar({
          open: true,
          message: approved ? " Application approved!" : " Application rejected!",
          severity: "success",
        });

        //  Quay về đúng trang list (dưới nhánh /cva)
        setTimeout(() => {
          navigate("/cva/registration_project_management", { replace: true });
        }, 1000);
      } else {
        throw new Error(msg);
      }
    } catch (error) {
      console.error(" Decision failed:", error);
      setSnackbar({
        open: true,
        message: "Decision failed!",
        severity: "error",
      });
    }
  };

  // 🔸 Loading
  if (loading)
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height="70vh">
        <CircularProgress />
      </Box>
    );

  // 🔸 Không có dữ liệu
  if (!application)
    return (
      <Box textAlign="center" mt={5}>
        <Typography variant="h6" color="error">
          Application not found.
        </Typography>
        <Button
          onClick={() => navigate("/cva/registration_project_management")}
          variant="contained"
          sx={{ mt: 2 }}
        >
          Back to List
        </Button>
      </Box>
    );

  const status = application.status;

  return (
    <Box m="20px">
      <Header
        title="CVA DECISION"
        subtitle={`ID: ${application.id || application.applicationId}`}
      />

      <Paper sx={{ p: 3, mt: 2 }}>
        {/* 🔸 Thông tin chỉ đọc */}
        <TextField
          label="Project Title"
          value={application.projectTitle || ""}
          fullWidth
          InputProps={{ readOnly: true }}
          sx={{ mt: 2 }}
        />
        <TextField
          label="Company Name"
          value={application.companyName || ""}
          fullWidth
          InputProps={{ readOnly: true }}
          sx={{ mt: 2 }}
        />
        <TextField
          label="Current Status"
          value={status}
          fullWidth
          InputProps={{ readOnly: true }}
          sx={{ mt: 2 }}
        />

        {/*  Ghi chú quyết định */}
        <TextField
          label="Final Review Note"
          value={finalReviewNote}
          onChange={(e) => setFinalReviewNote(e.target.value)}
          fullWidth
          multiline
          rows={3}
          sx={{ mt: 2 }}
        />

        {/* 🔹 Chỉ hiển thị nút duyệt khi đang UNDER_REVIEW */}
        {status === "UNDER_REVIEW" ? (
          <Box mt={3} display="flex" gap={2}>
            <Button variant="contained" color="success" onClick={() => handleDecision(true)}>
              Approve
            </Button>
            <Button variant="contained" color="error" onClick={() => handleDecision(false)}>
              Reject
            </Button>
            <Button
              variant="outlined"
              color="secondary"
              onClick={() => navigate(`/cva/view_registration_project/${id}`)}
            >
              Cancel
            </Button>
          </Box>
        ) : (
          <Typography sx={{ mt: 3 }} color="text.secondary">
            This application is already {status}.
          </Typography>
        )}
      </Paper>

      {/*  Snackbar */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={3000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: "top", horizontal: "center" }}
      >
        <Alert severity={snackbar.severity} variant="filled">
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ApplicationEdit;
