// src/scenes/admin/view_application.jsx
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
  const [snackbar, setSnackbar] = useState({ open: false, message: "", severity: "success" });

  useEffect(() => {
    const fetchDetail = async () => {
      try {
        const data = await getProjectApplicationById(id);
        setApplication(data);
      } catch (error) {
        console.error("Error fetching detail:", error);
        setSnackbar({ open: true, message: "Failed to fetch application.", severity: "error" });
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
        <Typography>Company: {application.companyName}</Typography>
        <Typography>Status: {application.status}</Typography>
        <Typography>Review Note: {application.reviewNote || "N/A"}</Typography>
        <Typography>Final Review Note: {application.finalReviewNote || "N/A"}</Typography>

        <Button
          variant="contained"
          sx={{ mt: 2 }}
          onClick={() => navigate(`/admin/edit_company/${id}`)}
        >
          Edit
        </Button>

        <Typography mt={2}>Submitted At: {new Date(application.submittedAt).toLocaleString()}</Typography>

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

        <Box mt={3}>
          <Button variant="contained" onClick={() => navigate(-1)}>
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
