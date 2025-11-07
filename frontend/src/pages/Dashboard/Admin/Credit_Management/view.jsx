import { useParams, useNavigate } from "react-router-dom";
import {
  Box,
  Typography,
  Button,
  Grid,
  Snackbar,
  Alert,
  Paper,
  Link
} from "@mui/material";
import { useTheme } from "@mui/material/styles";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { useState, useEffect } from "react";
import { getIssuedCreditByBatchId } from "@/apiAdmin/creditAdmin.js";

const ViewCredit = () => {
  const { id } = useParams(); // đây là batchId
  const navigate = useNavigate();
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  const [creditData, setCreditData] = useState(null);
  const [status, setStatus] = useState("");
  const [openSnackbar, setOpenSnackbar] = useState(false);

  useEffect(() => {
    const fetchCredit = async () => {
      try {
        const res = await getIssuedCreditByBatchId(id);
        const data = res.response;
        setCreditData(data);
        setStatus(data?.status || "");
      } catch (err) {
        console.error("Error fetching issued credit:", err);
      }
    };
    fetchCredit();
  }, [id]);

  const handleCloseSnackbar = () => {
    setOpenSnackbar(false);
  };

  if (!creditData) {
    return (
      <Box m="20px">
        <Typography variant="h4" color={colors.grey[100]}>
          Credit not found
        </Typography>
      </Box>
    );
  }

  const colorMap = {
    Active: "#4CAF50",
    Revoked: "#E53935",
    Pending: "#42A5F5",
    Sold: "#FFB300",
    Listed: "#FDD835",
    Retired: "#757575",
  };
  const color = colorMap[status] || colors.grey[300];

  return (
    <Box m="20px" sx={{ marginLeft: "290px",maxWidth: "1000px",width: "100%", }}>
      <Header title="CREDIT DETAIL" subtitle={`Details of ${creditData.batchCode}`} />

      <Paper
        elevation={4}
        sx={{
          backgroundColor: colors.primary[400],
          p: "30px",
          borderRadius: "10px",
          boxShadow: 3,
        }}
      >
        <Grid container spacing={20}>
          {/* Cột trái */}
          <Grid item xs={12} sm={6}>
            <Typography variant="h6" color={colors.grey[100]}>Credit ID:</Typography>
            <Typography>{creditData.id}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>Batch Code:</Typography>
            <Typography>{creditData.batchCode}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>Company:</Typography>
            <Typography>{creditData.companyName}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>Project Name:</Typography>
            <Typography>{creditData.projectTitle}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>Vintage Year:</Typography>
            <Typography>{creditData.vintageYear}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>Number of Credits:</Typography>
            <Typography>{creditData.creditsCount}</Typography>
          </Grid>

          {/* Cột phải */}
          <Grid item xs={12} sm={6}>
            <Typography variant="h6" color={colors.grey[100]}>Estimated Value (tCO2e):</Typography>
            <Typography>{creditData.totalTco2e}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>Residual (tCO2e):</Typography>
            <Typography>{creditData.residualTco2e}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>Issued Day:</Typography>
            <Typography>
              {creditData.issuedAt
                ? (() => {
                  const date = new Date(creditData.issuedAt);
                  const day = String(date.getDate()).padStart(2, "0");
                  const month = String(date.getMonth() + 1).padStart(2, "0");
                  const year = date.getFullYear();
                  const time = date.toLocaleTimeString();
                  return `${day}/${month}/${year}, ${time}`;
                })()
                : "N/A"}
            </Typography>


            <Typography variant="h6" color={colors.grey[100]} mt={2}>Status:</Typography>
            <Typography sx={{ color, fontWeight: 600, textTransform: "capitalize" }}>
              {status}
            </Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>Serial From:</Typography>
            <Typography>{creditData.serialFrom}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>Serial To:</Typography>
            <Typography>{creditData.serialTo}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>Certificate:</Typography>
            {creditData.certificateUrl ? (
              <Button
                variant="contained"
                color="primary"
                onClick={() => window.open(creditData.certificateUrl, "_blank")}
              >
                View Certificate
              </Button>
            ) : (
              <Typography>No Certificate Available</Typography>
            )}
          </Grid>
        </Grid>

        {/* Buttons */}
        <Box display="flex" justifyContent="flex-end" gap={2} mt={4}>
          <Button variant="outlined" color="inherit" onClick={() => navigate(-1)}>
            Back
          </Button>
        </Box>
      </Paper>

      <Snackbar
        open={openSnackbar}
        autoHideDuration={3000}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
      >
        <Alert onClose={handleCloseSnackbar} severity="success" sx={{ width: "100%" }}>
          Status updated successfully!
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ViewCredit;
