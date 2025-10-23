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

  //  Map status từ API
  const mapStatus = (status) => {
    switch (status) {
      case "ADMIN_APPROVED":
        return "APPROVED";
      case "ADMIN_REJECTED":
        return "REJECTED";
      case "NEEDS_REVISION":
        return "NEEDS_REVISION";
      case "UNDER_REVIEW":
        return "UNDER_REVIEW";
      default:
        return "SUBMITTED";
    }
  };

  //  Fetch dữ liệu
  useEffect(() => {
    const fetchApplication = async () => {
      try {
        const res = await getProjectApplicationById(id);
        console.log(" Raw API response:", res);

        const appData = res?.response || res;
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
        console.error(" Error fetching application:", error);
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

  //  Cập nhật dữ liệu form
  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  //  Gửi cập nhật duyệt (Save)
  const handleUpdate = async () => {
  try {
    const payload = {
      approved: formData.status === "APPROVED", //  chỉ approved nếu status = APPROVED
      note: formData.finalReviewNote || "No note provided",
    };

    console.log(" Sending update payload:", payload);

    const result = await updateApplicationDecision(id, payload);
    console.log(" Update result:", result);

    const code = result?.responseStatus?.responseCode;
    if (code === "200" || code === "00000000") {
      setSnackbar({
        open: true,
        message: " Updated successfully!",
        severity: "success",
      });
      setTimeout(() => navigate("/admin/company_management"), 1000);
    } else {
      throw new Error(result?.responseStatus?.responseMessage);
    }
  } catch (error) {
    console.error(" Update failed:", error);
    setSnackbar({
      open: true,
      message: "Application submission failed. Please check your data.",
      severity: "error",
    });
  }
};



  //  Loading state
  if (loading)
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height="70vh">
        <CircularProgress />
      </Box>
    );

  //  Không tìm thấy dữ liệu
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

  //  Giao diện form chính
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
          <MenuItem value="NEEDS_REVISION">Needs Revision</MenuItem>
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
          InputProps={{readOnly: true}}
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
          InputProps={{readOnly: true}}
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
