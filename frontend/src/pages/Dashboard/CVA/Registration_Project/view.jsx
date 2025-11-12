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
  Grid,
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import { getProjectApplicationByIdForCVA, getCompanyKYCProfile } from "@/apiCVA/registrationCVA.js";
import Header from "@/components/Chart/Header";


const ApplicationView = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [application, setApplication] = useState(null);
  const [kyc, setKyc] = useState(null);
  const [loading, setLoading] = useState(true);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: "",
    severity: "success",
  });

  useEffect(() => {
    const fetchDetail = async () => {
      try {
        const appRes = await getProjectApplicationByIdForCVA(id);
        const appCode = appRes?.responseStatus?.responseCode;
        let appData = null;

        if (appCode === "200" || appCode === "00000000") {
          appData = appRes?.responseData || appRes?.response || appRes;
          setApplication(appData);
        } else {
          setSnackbar({
            open: true,
            message: `Lỗi lấy dữ liệu Application (code ${appCode})`,
            severity: "error",
          });
        }

        if (appData?.companyId) {
          const kycRes = await getCompanyKYCProfile(appData.companyId);
          if (kycRes?.id) setKyc(kycRes);
        }
      } catch (error) {
        console.error("Error fetching details:", error);
        setSnackbar({
          open: true,
          message: "Không thể tải dữ liệu chi tiết hoặc KYC.",
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
        <CircularProgress size={60} thickness={4} />
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
    <Box m="20px" sx={{ marginLeft: "290px"}}>
      <Header
        title="APPLICATION DETAIL"
        subtitle={`Application ID: ${application.applicationId || id}`}
      />

      <Paper
        sx={{
          p: 4,
          mt: 3,
          borderRadius: 3,
          boxShadow: 4,
          backgroundColor: "#fafafa",
        }}
      >
        {/* Application Info */}
        <Typography
          variant="h5"
          sx={{ fontWeight: "bold", mb: 2, color: "#1976d2", fontSize: "1.2rem" }}
        >
          Application Information
        </Typography>
        <Divider sx={{ mb: 3 }} />

        <Grid container spacing={10}>
          <Grid item xs={12} sm={6}>
            <Typography sx={{ fontSize: "1rem" }}><b>Project Title:</b> {application.projectTitle || "Untitled Project"}</Typography>
            <Typography sx={{ fontSize: "1rem" }}><b>Project ID:</b> {application.projectId || "—"}</Typography>
            <Typography sx={{ fontSize: "1rem" }}><b>Status:</b> {application.status || "—"}</Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography sx={{ fontSize: "1rem" }}><b>Company Name:</b> {application.companyName || "—"}</Typography>
            <Typography sx={{ fontSize: "1rem" }}><b>Company ID:</b> {application.companyId || "—"}</Typography>
            <Typography sx={{ fontSize: "1rem" }}>
              <b>Submitted At:</b>{" "}
              {application.submittedAt
                ? new Date(application.submittedAt).toLocaleDateString("vi-VN")
                : "N/A"}
            </Typography>
          </Grid>
        </Grid>

        <Typography mt={2} sx={{ fontSize: "1rem" }}>
          <b>Review Note:</b> {application.reviewNote || "N/A"}
        </Typography>
        <Typography sx={{ fontSize: "1rem" }}>
          <b>Final Review Note:</b> {application.finalReviewNote || "N/A"}
        </Typography>

        {application.applicationDocsUrl ? (
          <Box mt={2}>
            <Button
              href={application.applicationDocsUrl}
              target="_blank"
              variant="outlined"
              sx={{ textTransform: "none", fontSize: "1rem" }}
            >
              View Attached Documents
            </Button>
          </Box>
        ) : (
          <Typography mt={2} color="text.secondary" sx={{ fontSize: "1rem" }}>
            No documents attached
          </Typography>
        )}

        {/* KYC Info */}
        {kyc && (
          <>
            <Divider sx={{ my: 4 }} />
            <Typography
              variant="h5"
              sx={{ fontWeight: "bold", mb: 2, color: "#388e3c", fontSize: "1.2rem" }}
            >
              Company Registration
            </Typography>

            <Grid container spacing={19}>
              <Grid item xs={12} sm={6}>
                <Typography sx={{ fontSize: "1rem" }}><b>KYC ID:</b> {kyc.id}</Typography>
                <Typography sx={{ fontSize: "1rem" }}><b>Company Name:</b> {kyc.companyName}</Typography>
                <Typography sx={{ fontSize: "1rem" }}><b>Tax Code:</b> {kyc.taxCode}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography sx={{ fontSize: "1rem" }}><b>Business License:</b> {kyc.businessLicense}</Typography>
                <Typography sx={{ fontSize: "1rem" }}><b>Address:</b> {kyc.address}</Typography>
                <Typography sx={{ fontSize: "1rem" }}>
                  <b>Created At:</b>{" "}
                  {new Date(kyc.createAt).toLocaleString("vi-VN")}
                </Typography>
              </Grid>
            </Grid>
          </>
        )}

        <Box mt={5} display="flex" gap={2} justifyContent="flex-end"  width="100%">
          <Button
            variant="contained"
            color="primary"
            size="large"
            onClick={() => navigate(`/cva/edit_registration_project/${id}`)}
          >
            Edit
          </Button>
          <Button variant="outlined" size="large" onClick={() => navigate(-1)}>
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