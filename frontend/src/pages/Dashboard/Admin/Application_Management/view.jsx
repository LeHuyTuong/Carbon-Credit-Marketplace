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
        console.log(" View page ID:", id);
        const res = await getProjectApplicationById(id);
        const data = res?.response || res;
        console.log(" Raw API response:", data);

        if (data && data.id) setApplication(data);
        else throw new Error("No valid data received");
      } catch (error) {
        console.error(" Error fetching detail:", error);
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
        <Button
          onClick={() => navigate("/admin/company_management")}
          variant="contained"
          sx={{ mt: 2 }}
        >
          Back
        </Button>
      </Box>
    );

  return (
    <Box m="20px">
      {/* Header + Buttons Row */}
      <Box
        display="flex"
        justifyContent="space-between"
        alignItems="center"
        mb={2}
      >
        <Header
          title="COMPANY APPLICATION DETAIL"
          subtitle={`ID: ${application.id}`}
        />

        <Box display="flex" gap={2}>
          <Button
            variant="outlined"
            color="secondary"
            onClick={() => navigate("/admin/company_management")}
          >
            Back
          </Button>
          <Button
            variant="contained"
            color="secondary"
            onClick={() => navigate(`/admin/edit_company/${application.id}`)}
          >
            Edit
          </Button>
        </Box>
      </Box>

      {/* Detail Content */}
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Project Title: {application.projectTitle}
        </Typography>

        <Typography sx={{ mt: 1 }}>Company: {application.companyName}</Typography>
        <Typography sx={{ mt: 1 }}>Status: {application.status}</Typography>
        <Typography sx={{ mt: 1 }}>
          Review Note: {application.reviewNote || "N/A"}
        </Typography>
        <Typography sx={{ mt: 1 }}>
          Final Review Note: {application.finalReviewNote || "N/A"}
        </Typography>

        <Typography sx={{ mt: 2 }}>
          Submitted At:{" "}
          {application.submittedAt
            ? (() => {
              const date = new Date(application.submittedAt);
              const day = String(date.getDate()).padStart(2, "0");
              const month = String(date.getMonth() + 1).padStart(2, "0");
              const year = date.getFullYear();
              const time = date.toLocaleTimeString();
              return `${day}/${month}/${year}, ${time}`;
            })()
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
              View Attached Docs
            </a>
          </Box>
        ) : (
          <Typography mt={2} color="text.secondary">
            No document attached
          </Typography>
        )}
      </Paper>

      {/* Snackbar */}
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
