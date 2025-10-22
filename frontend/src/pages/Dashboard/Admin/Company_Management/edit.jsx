// src/scenes/admin/application_edit.jsx
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
import {
  getProjectApplicationById,
  updateApplicationDecision,
} from "@/apiAdmin/companyAdmin.js";
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

  // Map status từ API về giá trị frontend
  const mapStatus = (status) => {
    switch (status) {
      case "ADMIN_APPROVED":
      case "APPROVED":
        return "APPROVED";
      case "ADMIN_REJECTED":
      case "REJECTED":
        return "REJECTED";
      case "UNDER_REVIEW":
        return "UNDER_REVIEW";
      case "SUBMITTED":
      default:
        return "SUBMITTED";
    }
  };

  useEffect(() => {
    const fetchApplication = async () => {
      try {
        const data = await getProjectApplicationById(id);
        console.log("📥 Raw API response:", data);

        const appData = data?.response || data;

        if (appData && appData.id) {
          setApplication(appData);
          setFormData({
            projectTitle: appData.projectTitle || "",
            companyName: appData.companyName || "",
            status: mapStatus(appData.status),
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

  // Cập nhật form
  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  // Gửi update
  const handleUpdate = async () => {
    if (!application || !application.id) {
      setSnackbar({
        open: true,
        message: "Application not found. Cannot update.",
        severity: "error",
      });
      return;
    }

    const payload = {
      approved: formData.status === "APPROVED",
      note: formData.finalReviewNote || formData.reviewNote || "",
    };

    console.log("📤 Sending update payload:", payload);

    try {
      const result = await updateApplicationDecision(application.id, payload);

      if (result?.responseStatus?.responseCode === "200") {
        setSnackbar({
          open: true,
          message: "✅ Updated successfully!",
          severity: "success",
        });
        setTimeout(() => navigate(`/admin/view_company/${application.id}`), 1200);
      } else {
        throw new Error(result?.responseStatus?.responseMessage || "Update failed");
      }
    } catch (error) {
      console.error("❌ Update failed:", error);
      setSnackbar({
        open: true,
        message: "Update failed! Please check your data.",
        severity: "error",
      });
    }
  };

  // Loading
  if (loading)
    return (
      <Box
        display="flex"
        justifyContent="center"
        alignItems="center"
        height="70vh"
      >
        <CircularProgress />
      </Box>
    );

  // Không có dữ liệu
  if (!application)
    return (
      <Box textAlign="center" mt={5}>
        <Typography variant="h6" color="error">
          Application not found.
        </Typography>
        <Button
          onClick={() => navigate("/admin/applications")}
          variant="contained"
          sx={{ mt: 2 }}
        >
          Back to List
        </Button>
      </Box>
    );

  // Form edit
  return (
    <Box m="20px">
      <Box display="flex" justifyContent="space-between" alignItems="center">
        <Header title="EDIT APPLICATION" subtitle={`ID: ${application.id}`} />
        <Button variant="outlined" onClick={() => navigate("/admin/applications")}>
          Back
        </Button>
      </Box>

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
          <MenuItem value="UNDER_REVIEW">Under Review</MenuItem>
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

        <Box mt={3} display="flex" gap={2}>
          <Button variant="contained" onClick={handleUpdate}>
            Save
          </Button>
          <Button
            variant="outlined"
            onClick={() => navigate(`/admin/view_company/${application.id}`)}
          >
            Cancel
          </Button>
        </Box>
      </Paper>

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
