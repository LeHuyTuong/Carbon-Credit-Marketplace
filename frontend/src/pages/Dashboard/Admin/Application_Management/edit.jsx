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
  // State quản lý dữ liệu application
  const [application, setApplication] = useState(null);
  const [loading, setLoading] = useState(true);
  // Form data state
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
        // Gọi API lấy dữ liệu application theo ID
        const res = await getProjectApplicationById(id);
        console.log("Raw API response:", res);

        const appData = res?.response || res;
        if (appData && appData.id) {
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
      // Kiểm tra status — CHỈ CVA_APPROVED mới được SAVE
      if (formData.status !== "CVA_APPROVED") {
        showSnackbar("warning", "Status must be CVA_APPROVED before saving.");
        return;
      }
      // Chuẩn bị payload
      const payload = {
        approved: formData.status === "ADMIN_APPROVED",
        note: formData.finalReviewNote || "No note provided",
      };

      console.log("Sending update payload:", payload);
      // Gọi API cập nhật
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
    <Box m="20px" sx={{ marginLeft: "290px", marginTop: "-10px" }}>
      <Box display="flex" justifyContent="space-between" alignItems="center">
        <Header title="EDIT APPLICATION" subtitle={`ID: ${application.id}`} />
      </Box>
      {/* các field được liệt kê trong page */}
      <Paper sx={{ p: 3, mt: 1 }}>
        <TextField
          label="Project Title"
          value={formData.projectTitle}
          onChange={(e) => handleChange("projectTitle", e.target.value)}
          fullWidth // chiếm toàn bộ chiều rộng
          sx={{ mt: 2 }} // khoảng cách trên
          InputProps={{ readOnly: true }} // chỉ đọc
          inputProps={{ style: { cursor: "pointer" } }}
        />

        <TextField
          label="Company Name"
          value={formData.companyName}
          onChange={(e) => handleChange("companyName", e.target.value)}
          fullWidth
          sx={{ mt: 2 }} // khoảng cách trên
          InputProps={{ readOnly: true }} // chỉ đọc
          inputProps={{ style: { cursor: "pointer" } }} // Hiển thị con trỏ pointer
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
          {/* Các tùy chọn duyệt/từ chối */}
          <MenuItem value="ADMIN_APPROVED">ADMIN_APPROVED</MenuItem>
          <MenuItem value="ADMIN_REJECTED">ADMIN_REJECTED</MenuItem>
        </TextField>


        <TextField
          label="Verfication by CVA"
          value={formData.cvaReviewerName || "N/A"}
          onChange={(e) => handleChange("cvaReviewerName", e.target.value)}
          fullWidth // chiếm toàn bộ chiều rộng
          sx={{ mt: 2 }} // khoảng cách trên
          InputProps={{ readOnly: true }}
          inputProps={{ style: { cursor: "pointer" } }}
        />

        <TextField
          label="Review Note Of CVA"
          value={formData.reviewNote || "N/A"}
          onChange={(e) => handleChange("reviewNote", e.target.value)}
          fullWidth
          multiline // cho phép nhiều dòng
          rows={1}
          sx={{ mt: 2 }}
          InputProps={{ readOnly: true }}
          inputProps={{ style: { cursor: "pointer" } }} // Hiển thị con trỏ pointer
        />

        <TextField
          label="Final Review Note Of Admin"
          value={formData.finalReviewNote}
          onChange={(e) => handleChange("finalReviewNote", e.target.value)}
          fullWidth
          multiline
          rows={1}
          sx={{ mt: 2 }}
        />



        <Box mt={3} display="flex" gap={2}>
          {/*  Chỉ hiển thị nút Save nếu status KHÔNG phải CVA_REJECTED */}
          {formData.status == "CVA_APPROVED" && (
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

          {formData.status !== "CVA_APPROVED" && (
            <Typography fontSize="0.9rem" variant="caption" color="text.secondary" sx={{ ml: 1 }}>
              You can only save when status is CVA_APPROVED
            </Typography>
          )}
        </Box>
      </Paper>

      {SnackbarComponent}
    </Box>
  );
};

export default ApplicationEdit;
