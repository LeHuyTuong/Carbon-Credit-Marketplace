import React, { useEffect, useState } from "react";
import {
  Box,
  Typography,
  CircularProgress,
  Paper,
  Button,
  TextField,
  MenuItem,
  Snackbar,
  Alert,
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import { updateApplicationDecision,getProjectApplicationByIdForCVA } from "@/apiCVA/registrationCVA.js";
import Header from "@/components/Chart/Header";

const ApplicationEdit = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [application, setApplication] = useState(null);
  const [loading, setLoading] = useState(true);
  const [formData, setFormData] = useState({
    projectTitle: "",
    companyName: "",
    status: "",
    reviewNote: "",
    finalReviewNote: "",
    applicationDocsUrl: "",
  });
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: "",
    severity: "success",
  });

  useEffect(() => {
    const fetchApplication = async () => {
      try {
        const data = await getProjectApplicationByIdForCVA(id);
        const appData = data?.response || data;

        if (appData && appData.id) {
          setApplication(appData);
          setFormData({
            projectTitle: appData.projectTitle || "",
            companyName: appData.companyName || "",
            status: appData.status || "",
            reviewNote: appData.reviewNote || "",
            finalReviewNote: appData.finalReviewNote || "",
            applicationDocsUrl: appData.applicationDocsUrl || "",
          });
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

  // 🟢 Cập nhật form
  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  // 🟢 Gửi update
  const handleUpdate = async () => {
    try {
      const payload = {
        approved: formData.status === "APPROVED",
        note: formData.finalReviewNote || formData.reviewNote || "",
      };

      console.log("📤 Sending update payload:", payload);
      const result = await updateApplicationDecision(id, payload);

      // 🟡 Xử lý theo chuẩn OpenAPI spec
      const responseCode = result?.responseStatus?.responseCode || "500";
      const responseMsg =
        result?.responseStatus?.responseMessage || "Unknown response from server.";

      if (responseCode === "00000000") {
        setSnackbar({
          open: true,
          message: "✅ Updated successfully!",
          severity: "success",
        });
        setTimeout(() => navigate(`/cva/view_registration_project/${id}`), 1200);
      } else {
        throw new Error(responseMsg);
      }
    } catch (error) {
      console.error("❌ Update failed:", error);
      setSnackbar({
        open: true,
        message: "Update failed!",
        severity: "error",
      });
    }
  };

  if (loading)
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height="70vh">
        <CircularProgress />
      </Box>
    );

  if (!application)
    return (
      <Box textAlign="center" mt={5}>
        <Typography variant="h6" color="error">
          Application not found.
        </Typography>
        <Button
          onClick={() => navigate("/cva/review_registration_project")}
          variant="contained"
          sx={{ mt: 2 }}
        >
          Back to List
        </Button>
      </Box>
    );

  return (
    <Box m="20px">
      <Header title="EDIT APPLICATION" subtitle={`ID: ${application.id}`} />

      <Paper sx={{ p: 3, mt: 2 }}>
        <TextField
          label="Project Title"
          value={formData.projectTitle}
          onChange={(e) => handleChange("projectTitle", e.target.value)}
          fullWidth
          sx={{ mt: 2 }}
        />

        <TextField
          label="Company Name"
          value={formData.companyName}
          onChange={(e) => handleChange("companyName", e.target.value)}
          fullWidth
          sx={{ mt: 2 }}
        />

        <TextField
          select
          label="Status"
          value={formData.status}
          onChange={(e) => handleChange("status", e.target.value)}
          fullWidth
          sx={{ mt: 2 }}
        >
          <MenuItem value="SUBMITTED">Submitted</MenuItem>
          <MenuItem value="REVIEWING">Reviewing</MenuItem>
          <MenuItem value="APPROVED">Approved</MenuItem>
          <MenuItem value="REJECTED">Rejected</MenuItem>
        </TextField>

        <TextField
          label="Review Note"
          value={formData.reviewNote}
          onChange={(e) => handleChange("reviewNote", e.target.value)}
          fullWidth
          multiline
          rows={3}
          sx={{ mt: 2 }}
        />

        <TextField
          label="Final Review Note"
          value={formData.finalReviewNote}
          onChange={(e) => handleChange("finalReviewNote", e.target.value)}
          fullWidth
          multiline
          rows={3}
          sx={{ mt: 2 }}
        />

        <TextField
          label="Application Docs URL"
          value={formData.applicationDocsUrl}
          onChange={(e) => handleChange("applicationDocsUrl", e.target.value)}
          fullWidth
          sx={{ mt: 2 }}
        />

        {/* 🟢 Hai nút Save & Cancel */}
        <Box mt={3} display="flex" gap={2}>
          <Button variant="contained" color="primary" onClick={handleUpdate}>
            Save
          </Button>
          <Button
            variant="outlined"
            color="secondary"
            onClick={() => navigate(`/cva/view_registration_project/${id}`)}
          >
            Cancel
          </Button>
        </Box>
      </Paper>

      {/* 🟢 Snackbar thông báo */}
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
