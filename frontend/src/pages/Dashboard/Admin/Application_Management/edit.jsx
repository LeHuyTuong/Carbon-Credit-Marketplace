import React, { useEffect, useState } from "react";
import {
  Box,
  Typography,
  CircularProgress,
  Paper,
  Button,
  TextField,
  MenuItem,
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import {
  getProjectApplicationById,
  updateApplicationDecision,
} from "@/apiAdmin/companyAdmin.js";
import Header from "@/components/Chart/Header";
import { useSnackbar } from "@/hooks/useSnackbar.jsx";

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
  const { showSnackbar, SnackbarComponent } = useSnackbar();

  // Fetch dữ liệu
  useEffect(() => {
    const fetchApplication = async () => {
      try {
        const res = await getProjectApplicationById(id);
        console.log("Raw API response:", res);

        const appData = res?.response || res;
        if (appData && appData.id) {
          setApplication(appData);
          setFormData({
            projectTitle: appData.projectTitle || "",
            companyName: appData.companyName || "",
            status: appData.status,
            reviewNote: appData.reviewNote || "",
            finalReviewNote: appData.finalReviewNote || "",
            applicationDocsUrl: appData.applicationDocsUrl || "",
          });
        } else {
          throw new Error("Application not found");
        }
      } catch (error) {
        console.error("Error fetching application:", error);

        const message =
          error?.response?.data?.responseStatus?.responseDesc ||
          error?.response?.data?.message ||
          error?.message ||
          "Failed to fetch application.";

        showSnackbar("error", message);
      } finally {
        setLoading(false);
      }
    };

    fetchApplication();
  }, [id]);

  // Cập nhật dữ liệu form
  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  // Gửi cập nhật duyệt (Save)
  const handleUpdate = async () => {
    try {
      const payload = {
        approved: formData.status === "APPROVED",
        note: formData.finalReviewNote || "No note provided",
      };

      console.log("Sending update payload:", payload);

      const result = await updateApplicationDecision(id, payload);
      console.log("Update result:", result);

      const code = result?.responseStatus?.responseCode;
      if (code === "200" || code === "00000000") {
        showSnackbar("success", "Updated successfully!");
        setTimeout(() => navigate("/admin/company_management"), 1000);
      } else {
        throw new Error(result?.responseStatus?.responseMessage);
      }
    } catch (error) {
      console.error("Update failed:", error);

      const message =
        error?.response?.data?.responseStatus?.responseDesc ||
        error?.response?.data?.message ||
        error?.message ||
        "Application submission failed. Please check your data.";

      showSnackbar("error", message);
    }
  };

  // Loading state
  if (loading)
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height="70vh">
        <CircularProgress />
      </Box>
    );

  // Không tìm thấy dữ liệu
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

  // Giao diện form chính
  return (
    <Box m="20px" sx={{ marginLeft: "290px" }}>
      <Box display="flex" justifyContent="space-between" alignItems="center">
        <Header title="EDIT APPLICATION" subtitle={`ID: ${application.id}`} />
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
          value={formData.status || ""}
          onChange={(e) => handleChange("status", e.target.value)}
          fullWidth
          sx={{ mt: 2 }}
        >
          {/* Nếu status hiện tại không phải hai giá trị này, hiển thị dòng readonly,không hiển thị trong dropdown */}
          {!["ADMIN_APPROVED", "ADMIN_REJECTED"].includes(formData.status) && (
            <MenuItem value={formData.status} disabled style={{ display: "none" }}>
              {formData.status}
            </MenuItem>
          )}
          <MenuItem value="ADMIN_APPROVED">ADMIN_APPROVED</MenuItem>
          <MenuItem value="ADMIN_REJECTED">ADMIN_REJECTED</MenuItem>
        </TextField>

        <TextField
          label="Review Note"
          value={formData.reviewNote}
          onChange={(e) => handleChange("reviewNote", e.target.value)}
          fullWidth
          multiline
          rows={3}
          sx={{ mt: 2 }}
          InputProps={{ readOnly: true }}
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
          InputProps={{ readOnly: true }}
        />

        <Box mt={3} display="flex" gap={2}>
          {/*  Chỉ hiển thị nút Save nếu status KHÔNG phải CVA_REJECTED */}
          {formData.status !== "CVA_REJECTED" && (
            <Button variant="contained" onClick={handleUpdate}>
              Save
            </Button>
          )}

          <Button
            variant="outlined"
            onClick={() => navigate(`/admin/view_company/${application.id}`)}
          >
            Cancel
          </Button>
        </Box>
      </Paper>

      {SnackbarComponent}
    </Box>
  );
};

export default ApplicationEdit;
