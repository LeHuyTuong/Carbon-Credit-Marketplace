import React, { useEffect, useState } from "react";
import {
  Box,
  Typography,
  CircularProgress,
  Paper,
  Button,
  Snackbar,
  Alert,
  Divider,
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import { getProjectApplicationByIdForCVA } from "@/apiCVA/registrationCVA.js";
import Header from "@/components/Chart/Header";

const ApplicationView = () => {
  const { id } = useParams(); // ← id lấy từ route: /cva/view_registration_project/:id
  const navigate = useNavigate();

  const [application, setApplication] = useState(null);
  const [loading, setLoading] = useState(true);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: "",
    severity: "success",
  });

  useEffect(() => {
    const fetchDetail = async () => {
      try {
        console.log(" Fetching application with ID:", id);
        const res = await getProjectApplicationByIdForCVA(id);
        console.log(" Raw API response:", res);

        //  API chuẩn trả về responseData chứa dữ liệu
        const code = res?.responseStatus?.responseCode;
        if (code === "200" || code === "00000000") {
          const data =
            res?.responseData ||
            res?.response ||
            res; // fallback nếu backend trả khác format
          if (data) {
            setApplication(data);
          } else {
            setSnackbar({
              open: true,
              message: "Không tìm thấy dữ liệu trong phản hồi.",
              severity: "warning",
            });
          }
        } else {
          setSnackbar({
            open: true,
            message: `API trả về lỗi code ${code}`,
            severity: "error",
          });
        }
      } catch (error) {
        console.error(" Error fetching detail:", error);
        setSnackbar({
          open: true,
          message: "Không thể tải chi tiết đăng ký.",
          severity: "error",
        });
      } finally {
        setLoading(false);
      }
    };

    fetchDetail();
  }, [id]);

  //  Loading
  if (loading)
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height="70vh">
        <CircularProgress />
      </Box>
    );

  //  Không có dữ liệu
  if (!application)
    return (
      <Box textAlign="center" mt={5}>
        <Typography variant="h6" color="error">
          Application not found.
        </Typography>
        <Button onClick={() => navigate(-1)} variant="contained" sx={{ mt: 2 }}>
          Back
        </Button>
      </Box>
    );

  //  UI chính
  return (
    <Box m="20px">
      <Header
        title="APPLICATION DETAIL"
        subtitle={`Application ID: ${application.applicationId || id}`}
      />

      <Paper sx={{ p: 3, mt: 2, borderRadius: 3, boxShadow: 3 }}>
        <Typography variant="h6" gutterBottom>
          {application.projectTitle || "Untitled Project"}
        </Typography>
        <Divider sx={{ mb: 2 }} />

        <Typography> <b>Project ID:</b> {application.projectId || "—"}</Typography>
        <Typography> <b>Company ID:</b> {application.companyId || "—"}</Typography>
        <Typography> <b>Company Name:</b> {application.companyName || "—"}</Typography>
        <Typography> <b>Status:</b> {application.status || "—"}</Typography>

        <Typography mt={2}>
           <b>Review Note:</b> {application.reviewNote || "N/A"}
        </Typography>
        <Typography>
           <b>Final Review Note:</b> {application.finalReviewNote || "N/A"}
        </Typography>

        <Typography mt={2}>
           <b>Submitted At:</b>{" "}
          {application.submittedAt
            ? new Date(application.submittedAt).toLocaleString()
            : "N/A"}
        </Typography>

        {application.applicationDocsUrl ? (
          <Box mt={2}>
            <a
              href={application.applicationDocsUrl}
              target="_blank"
              rel="noopener noreferrer"
              style={{
                color: "#1976d2",
                textDecoration: "underline",
                fontWeight: 500,
              }}
            >
              📎 View Attached Documents
            </a>
          </Box>
        ) : (
          <Typography mt={2} color="text.secondary">
            No documents attached
          </Typography>
        )}

        <Box mt={4} display="flex" gap={2}>
          <Button
            variant="contained"
            color="primary"
            onClick={() => navigate(`/cva/edit_registration_project/${id}`)}
          >
            Edit
          </Button>
          <Button variant="outlined" onClick={() => navigate(-1)}>
            Back
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

export default ApplicationView;
