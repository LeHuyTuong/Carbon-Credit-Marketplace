import { useParams, useNavigate } from "react-router-dom";
import {
  Box,
  Typography,
  Button,
  Grid,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Snackbar,
  Alert,
  Paper
} from "@mui/material";
import { useTheme } from "@mui/material/styles";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { mockDataCredits } from "@/data/mockData";
import { useState } from "react";

const ViewCredit = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  const creditData = mockDataCredits.find((item) => item.id === parseInt(id));

  const [isEditing, setIsEditing] = useState(false);
  const [status, setStatus] = useState(creditData?.status || "");
  const [openSnackbar, setOpenSnackbar] = useState(false);

  if (!creditData) {
    return (
      <Box m="20px">
        <Typography variant="h4" color={colors.grey[100]}>
          Credit not found
        </Typography>
      </Box>
    );
  }

  const handleUpdate = () => {
    setIsEditing(false);
    setOpenSnackbar(true);
  };

  const handleCloseSnackbar = () => {
    setOpenSnackbar(false);
  };

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
    <Box m="20px">
      <Header title="CREDIT DETAIL" subtitle={`Details of ${creditData.creditid}`} />

      <Paper
        elevation={4}
        sx={{
          backgroundColor: colors.primary[400],
          p: "30px",
          borderRadius: "10px",
          boxShadow: 3,
        }}
      >
        <Grid container spacing={2}>
          {/* Cột trái */}
          <Grid item xs={12} sm={6}>
            <Typography variant="h6" color={colors.grey[100]}>
              Credit ID:
            </Typography>
            <Typography>{creditData.creditid}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Company:
            </Typography>
            <Typography>{creditData.aggregator}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Project Name:
            </Typography>
            <Typography>{creditData.projectname}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Number of Credits:
            </Typography>
            <Typography>{creditData.numbercredit}</Typography>
          </Grid>

          {/* Cột phải */}
          <Grid item xs={12} sm={6}>
            <Typography variant="h6" color={colors.grey[100]}>
              Estimated Value:
            </Typography>
            <Typography>{creditData.estimatedvalue}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Issued Day:
            </Typography>
            <Typography>{creditData.issuedday}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Status:
            </Typography>
            {isEditing ? (
              <FormControl fullWidth>
                <InputLabel>Status</InputLabel>
                <Select
                  value={status}
                  onChange={(e) => setStatus(e.target.value)}
                  label="Status"
                >
                  {["Pending", "Revoked", "Active", "Listed", "Sold", "Retired"].map((s) => (
                    <MenuItem key={s} value={s}>{s}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            ) : (
              <Typography sx={{ color, fontWeight: 600, textTransform: "capitalize" }}>
                {status}
              </Typography>
            )}

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Expired Day:
            </Typography>
            <Typography>{creditData.expiredday}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Linked Certificate:
            </Typography>
            <Typography sx={{ mb: 1 }}>{creditData.linkedcertificate}</Typography>
            <Box display="flex" gap={2}>
              <Button variant="contained" color="info">View</Button>
              <Button variant="contained" color="secondary">Download</Button>
            </Box>
          </Grid>
        </Grid>

        {/* Buttons */}
        <Box display="flex" justifyContent="flex-end" gap={2} mt={4}>
          {!isEditing ? (
            <Button variant="contained" color="warning" onClick={() => setIsEditing(true)}>
              Edit
            </Button>
          ) : (
            <Button variant="contained" color="success" onClick={handleUpdate}>
              Update
            </Button>
          )}
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
