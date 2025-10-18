import React, { useEffect, useState } from "react";
import {
  Box,
  Typography,
  CircularProgress,
  Paper,
  Button,
  Snackbar,
  Alert,
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import { getProjectApplicationById } from "@/apiAdmin/companyAdmin.js";
import Header from "@/components/Chart/Header";

const ApplicationView = () => {
  const { id } = useParams();
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
        // ✅ Mock dữ liệu mẫu (y hệt cấu trúc API)
        const mockApplications = [
          {
            id: 1,
            projectId: 101,
            projectTitle: "AI-Powered Chatbot",
            companyId: 201,
            companyName: "TechNova Co., Ltd.",
            status: "SUBMITTED",
            reviewNote: "Awaiting first review.",
            finalReviewNote: "",
            applicationDocsUrl: "https://example.com/docs/ai-chatbot.pdf",
            submittedAt: "2025-10-17T18:50:29.847Z",
          },
          {
            id: 2,
            projectId: 102,
            projectTitle: "Smart Farming Drone",
            companyId: 202,
            companyName: "AgriTech Vietnam",
            status: "REVIEWING",
            reviewNote: "Under review by committee.",
            finalReviewNote: "",
            applicationDocsUrl: "",
            submittedAt: "2025-10-16T14:32:12.000Z",
          },
          {
            id: 3,
            projectId: 103,
            projectTitle: "Blockchain Logistics System",
            companyId: 203,
            companyName: "LogiChain Solutions",
            status: "APPROVED",
            reviewNote: "Approved with minor revisions.",
            finalReviewNote: "Project approved officially.",
            applicationDocsUrl: "https://example.com/docs/logichain.pdf",
            submittedAt: "2025-10-15T09:00:00.000Z",
          },
          {
            id: 4,
            projectId: 104,
            projectTitle: "Renewable Energy Management",
            companyId: 204,
            companyName: "GreenFuture Energy",
            status: "REJECTED",
            reviewNote: "Insufficient technical details.",
            finalReviewNote: "Rejected due to incomplete documentation.",
            applicationDocsUrl: "",
            submittedAt: "2025-10-14T12:15:45.000Z",
          },
        ];

        let data;
        try {
          data = await getProjectApplicationById(id);
        } catch {
          // ✅ Nếu API chưa có, dùng dữ liệu mô phỏng
          data = mockApplications.find((item) => item.id === parseInt(id));
        }

        if (data) {
          setApplication(data);
        } else {
          setSnackbar({
            open: true,
            message: "Application not found.",
            severity: "error",
          });
        }
      } catch (error) {
        console.error("Error fetching detail:", error);
        setSnackbar({
          open: true,
          message: "Failed to fetch application.",
          severity: "error",
        });
      } finally {
        setLoading(false);
      }
    };
    fetchDetail();
  }, [id]);

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
        <Button onClick={() => navigate(-1)} variant="contained" sx={{ mt: 2 }}>
          Back
        </Button>
      </Box>
    );

  return (
    <Box m="20px">
      <Header title="APPLICATION DETAIL" subtitle={`ID: ${application.id}`} />
      <Paper sx={{ p: 3, mt: 2 }}>
        <Typography variant="h6">Project Title: {application.projectTitle}</Typography>
        <Typography>Project ID: {application.projectId}</Typography>
        <Typography>Company ID: {application.companyId}</Typography>
        <Typography>Company: {application.companyName}</Typography>
        <Typography>Status: {application.status}</Typography>
        <Typography>Review Note: {application.reviewNote || "N/A"}</Typography>
        <Typography>Final Review Note: {application.finalReviewNote || "N/A"}</Typography>

        <Typography mt={2}>
          Submitted At:{" "}
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
              style={{ color: "#42A5F5", textDecoration: "underline" }}
            >
              View Attached Docs
            </a>
          </Box>
        ) : (
          <Typography mt={2} color="text.secondary">
            No document attached
          </Typography>
        )}

        <Box mt={3} display="flex" gap={2}>
          <Button
            variant="contained"
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
