import React, { useEffect, useState } from "react";
import {
  Box,
  Typography,
  CircularProgress,
  Paper,
  Button,
  TextField,
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
  const [actionDone, setActionDone] = useState(false); // trạng thái nút đã click

  const [formData, setFormData] = useState({
    projectTitle: "",
    companyName: "",
    status: "",
    reviewNote: "",
    finalReviewNote: "",
    applicationDocsUrl: "",
    cvaReviewerName: "",
    adminReviewerName: "",
  });

  const { showSnackbar, SnackbarComponent } = useSnackbar();

  // Fetch dữ liệu
  useEffect(() => {
    const fetchApplication = async () => {
      try {
        const res = await getProjectApplicationById(id);
        const appData = res?.response || res;

        if (!appData?.id) throw new Error("Application not found");

        setApplication(appData);
        setFormData({
          projectTitle: appData.projectTitle || "",
          companyName: appData.companyName || "",
          status: appData.status,
          cvaReviewerName: appData.cvaReviewerName || "",
          adminReviewerName: appData.adminReviewerName || "",
          reviewNote: appData.reviewNote || "",
          finalReviewNote: appData.finalReviewNote || "",
          applicationDocsUrl: appData.applicationDocsUrl || "",
        });
      } catch (error) {
        console.error(error);
        showSnackbar(
          "error",
          error?.message || "Failed to fetch application."
        );
      } finally {
        setLoading(false);
      }
    };

    fetchApplication();
  }, [id]);

  // Cập nhật field
  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  // Xử lý Approve/Reject
  const handleDecision = async (isApproved) => {
    try {
      const payload = {
        approved: isApproved,
        note: formData.finalReviewNote || "No note provided",
      };

      const result = await updateApplicationDecision(id, payload);
      const code = result?.responseStatus?.responseCode;

      if (code === "200" || code === "00000000") {
        showSnackbar(
          "success",
          isApproved ? "Approved successfully!" : "Rejected successfully!"
        );
        setActionDone(true); // ẩn nút sau khi click
        setFormData((prev) => ({
          ...prev,
          status: isApproved ? "ADMIN_APPROVED" : "ADMIN_REJECTED",
        }));
      } else {
        throw new Error(result?.responseStatus?.responseMessage);
      }
    } catch (err) {
      console.error(err);
      showSnackbar("error", "Update failed. Please try again.");
    }
  };

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

  return (
    <Box m="20px" sx={{ marginLeft: "290px", marginTop: "-10px" }}>
      <Box display="flex" justifyContent="space-between" alignItems="center">
        <Header title="EDIT APPLICATION" subtitle={`ID: ${application.id}`} />
      </Box>

      <Paper sx={{ p: 3, mt: 1 }}>
        {/* Thông tin chỉ đọc */}
        <TextField
          label="Project Title"
          value={formData.projectTitle}
          fullWidth
          sx={{ mt: 2 }}
          InputProps={{ readOnly: true }}
          inputProps={{ style: { cursor: "pointer" } }} // Hiển thị con trỏ pointer
        />
        <TextField
          label="Company Name"
          value={formData.companyName}
          fullWidth
          sx={{ mt: 2 }}
          InputProps={{ readOnly: true }}
          inputProps={{ style: { cursor: "pointer" } }} // Hiển thị con trỏ pointer
        />
        <TextField
          label="Status"
          value={formData.status || "N/A"}
          fullWidth
          sx={{ mt: 2 }}
          InputProps={{ readOnly: true }}
          inputProps={{ style: { cursor: "pointer" } }} // Hiển thị con trỏ pointer
          
        />
        <TextField
          label="Verification by CVA"
          value={formData.cvaReviewerName || "N/A"}
          fullWidth
          sx={{ mt: 2 }}
          InputProps={{ readOnly: true }}
          inputProps={{ style: { cursor: "pointer" } }} // Hiển thị con trỏ pointer
        />
        <TextField
          label="Review Note Of CVA"
          value={formData.reviewNote || "N/A"}
          fullWidth
          multiline
          rows={2}
          sx={{ mt: 2 }}
          InputProps={{ readOnly: true }}
          inputProps={{ style: { cursor: "pointer" } }} // Hiển thị con trỏ pointer
        />

        {/* Admin Note */}
        <TextField
          label="Final Review Note Of Admin"
          value={formData.finalReviewNote}
          onChange={(e) => handleChange("finalReviewNote", e.target.value)}
          fullWidth
          multiline
          rows={2}
          sx={{ mt: 2 }}
        />

        {/* Nút Approve / Reject */}
        <Box mt={3} display="flex" gap={2}>
          {formData.status === "CVA_APPROVED" && !actionDone && (
            <>
              <Button
                variant="contained"
                color="success"
                onClick={() => handleDecision(true)}
              >
                Approve
              </Button>
              <Button
                variant="contained"
                color="error"
                onClick={() => handleDecision(false)}
              >
                Reject
              </Button>
            </>
          )}

          {/* Nút Cancel luôn hiện */}
          <Button
            variant="outlined"
            onClick={() => navigate(`/admin/view_company/${application.id}`)}
          >
            Cancel
          </Button>

          {/* Thông báo khi đã thao tác */}
          {(actionDone || formData.status !== "CVA_APPROVED") && (
            <Typography
              fontSize="0.9rem"
              variant="caption"
              color="text.secondary"
              sx={{ ml: 1 }}
            >
              Action completed or not allowed
            </Typography>
          )}
        </Box>
      </Paper>

      {SnackbarComponent}
    </Box>
  );
};

export default ApplicationEdit;
