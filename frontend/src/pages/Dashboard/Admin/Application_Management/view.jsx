import React, { useEffect, useState } from "react";
import {
  Box,
  Typography,
  CircularProgress,
  Paper,
  Button,
  Snackbar,
  Alert,
  Grid,
  useTheme,
  Divider
} from "@mui/material";
import { useParams, useNavigate, } from "react-router-dom";
import { getProjectApplicationById } from "@/apiAdmin/companyAdmin.js";
import Header from "@/components/Chart/Header";
import { tokens } from "@/theme";
import { getCompanyKYCProfile } from "@/apiAdmin/companyAdmin.js";


const ApplicationView = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const [application, setApplication] = useState(null);
  const [loading, setLoading] = useState(true);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: "",
    severity: "success",
  });
  const [kyc, setKyc] = useState(null);

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

  // Load KYC Info của công ty
  useEffect(() => {
    if (application?.companyId) {
      (async () => {
        try {
          const res = await getCompanyKYCProfile(application.companyId);
          const data = res?.response || res;
          setKyc(data);
        } catch (error) {
          console.error(" Failed to fetch KYC info:", error);
          setSnackbar({
            open: true,
            message: "Failed to fetch company KYC profile.",
            severity: "error",
          });
        }
      })();
    }
  }, [application?.companyId]);


  if (loading)
    return (
      <Box
        display="flex"
        justifyContent="center"
        alignItems="center"
        height="70vh"
        sx={{ marginLeft: "290px" }}
      >
        <CircularProgress />
      </Box>
    );

  if (!application)
    return (
      <Box textAlign="center" sx={{ marginLeft: "290px" }} mt={5}>
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
    <Box m="20px" sx={{ marginLeft: "290px", maxWidth: "1400px", width: "100%", }}>
      <Header
        title="COMPANY APPLICATION DETAIL"
        subtitle={`Detailed information of application ID: ${application.id}`}
      />

      <Paper
        elevation={3}
        sx={{
          p: 3,
          mt: 3,
          backgroundColor: colors.primary[400],
        }}
      >
        <Grid container spacing={35}>
          {/* COLUMN 1: General Info */}
          <Grid item xs={12} md={4}>
            <Typography variant="h5" fontWeight="700" color="secondary" gutterBottom>
              General Info
            </Typography>

            <Typography variant="h6" fontWeight="600" gutterBottom>
              Project Title:
            </Typography>
            <Typography mb={2}>{application.projectTitle}</Typography>

            <Typography variant="h6" fontWeight="600" gutterBottom>
              Review by Admin:
            </Typography>
            <Typography mb={2}>{application.adminReviewerName || "N/A"}</Typography>

            <Typography variant="h6" fontWeight="600" gutterBottom>
              Status:
            </Typography>
            <Typography mb={2}>{application.status}</Typography>
          </Grid>

          {/* COLUMN 2: Review Info */}
          <Grid item xs={12} md={4}>
            <Typography variant="h5" fontWeight="700" color="secondary" gutterBottom>
              Review Info
            </Typography>

            <Typography variant="h6" fontWeight="600" gutterBottom>
              Verification by CVA:
            </Typography>
            <Typography mb={2}>{application.cvaReviewerName || "N/A"}</Typography>

            <Typography variant="h6" fontWeight="600" gutterBottom>
              Review Note Of CVA:
            </Typography>
            <Typography mb={2}>{application.reviewNote || "N/A"}</Typography>

            <Typography variant="h6" fontWeight="600" gutterBottom>
              Final Review Note Of Admin:
            </Typography>
            <Typography mb={2}>{application.finalReviewNote || "N/A"}</Typography>

            <Typography variant="h6" fontWeight="600" gutterBottom>
              Submitted At:
            </Typography>
            <Typography mb={2}>
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

            <Typography variant="h6" fontWeight="600" gutterBottom>
              Attached Documents:
            </Typography>
            {application.applicationDocsUrl ? (
              <Button
                variant="contained"
                color="info"
                size="small"
                onClick={() => window.open(application.applicationDocsUrl, "_blank")}
              >
                View Attached Docs
              </Button>
            ) : (
              <Typography>No document attached</Typography>
            )}
          </Grid>

          {/* COLUMN 3: Company KYC Info */}
          {kyc && (
            <Grid item xs={12} md={4}>
              <Typography variant="h5" fontWeight="700" color="secondary" gutterBottom>
                Company Registration
              </Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                ID:
              </Typography>
              <Typography mb={2}>{kyc.id || "N/A"}</Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Company Name:
              </Typography>
              <Typography mb={2}>{kyc.companyName || "N/A"}</Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Tax Code:
              </Typography>
              <Typography mb={2}>{kyc.taxCode || "N/A"}</Typography>
              
              <Typography variant="h6" fontWeight="600" gutterBottom>
                Business License:
              </Typography>
              <Typography mb={2}>{kyc.businessLicense || "N/A"}</Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Address:
              </Typography>
              <Typography mb={2}>{kyc.address || "N/A"}</Typography>
               
               <Typography variant="h6" fontWeight="600" gutterBottom>
                Created At:
              </Typography>
              <Typography mb={2}>{new Date(kyc.createAt).toLocaleString("vi-VN") || "N/A"}</Typography>

               <Typography variant="h6" fontWeight="600" gutterBottom>
                Update At:
              </Typography>
              <Typography mb={2}>{new Date(kyc.updatedAt).toLocaleString("vi-VN") || "N/A"}</Typography>
            </Grid>
          )}
        </Grid>




        {/* Action Buttons */}
        <Box display="flex" justifyContent="flex-end" gap={2} mt={4}>
          <Button
            variant="outlined"
            color="info"
            onClick={() => navigate("/admin/company_management")}
            sx={{ fontWeight: 600 }}
          >
            Back
          </Button>
          <Button
            variant="contained"
            color="secondary"
            onClick={() => navigate(`/admin/edit_company/${application.id}`)}
            sx={{ fontWeight: 600 }}
          >
            Edit
          </Button>
        </Box>
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
