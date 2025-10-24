import { useParams, useNavigate } from "react-router-dom";
import {
  Box,
  Typography,
  Button,
  Grid,
  Snackbar,
  Alert,
  Paper,
  TextField,
  MenuItem,
} from "@mui/material";
import { useTheme } from "@mui/material/styles";
import { tokens } from "@/themeCVA";
import Header from "@/components/Chart/Header.jsx";
import { mockDataCompanyCVA } from "@/data/mockData";
import { useState } from "react";

const ViewCompany = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  // tìm dữ liệu công ty
  const initial = mockDataCompanyCVA.find(
    (item) => item.id.toString() === id.toString()
  );

  if (!initial) {
    return (
      <Box m="20px">
        <Typography variant="h4" color={colors.grey[100]}>
          Company not found
        </Typography>
      </Box>
    );
  }

  const [company, setCompany] = useState(initial);
  const [isEditing, setIsEditing] = useState(false);
  const [status, setStatus] = useState(company.status || "Active");
  const [note, setNote] = useState(company.note || "");
  const [openSnackbar, setOpenSnackbar] = useState(false);

  const handleSaveChanges = () => {
    const updated = { ...company, status, note };
    setCompany(updated);
    setIsEditing(false);
    setOpenSnackbar(true);
  };

  const handleCloseSnackbar = () => setOpenSnackbar(false);

  return (
    <Box m="20px">
      <Header
        title="COMPANY DETAIL"
        subtitle={`Details of ${company.companyname}`}
      />

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
              Company ID:
            </Typography>
            <Typography>{company.companyid}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Company Name:
            </Typography>
            <Typography>{company.companyname}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Email:
            </Typography>
            <Typography>{company.email}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Phone:
            </Typography>
            <Typography>{company.phone}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Legal Representative:
            </Typography>
            <Typography>{company.legalrep}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Registration Date:
            </Typography>
            <Typography>{company.registrationdate}</Typography>
          </Grid>

          {/* Cột phải */}
          <Grid item xs={12} sm={6}>
            <Typography variant="h6" color={colors.grey[100]}>
              EV Owners Participating:
            </Typography>
            <Typography>{company.evowners}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Total Approved Credits:
            </Typography>
            <Typography>{company.totalcredits}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Joined Projects:
            </Typography>
            <Typography>{company.projects?.join(", ") || "—"}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Operational Status:
            </Typography>
            {isEditing ? (
              <TextField
                select
                fullWidth
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                SelectProps={{ native: true }}
                sx={{ mt: 1 }}
              >
                <option value="Active">Active</option>
                <option value="Suspended">Suspended</option>
                <option value="Banned">Banned</option>
              </TextField>
            ) : (
              <Typography>{company.status}</Typography>
            )}

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Attached KYC Documents:
            </Typography>
            <Typography>{company.kycfiles || "—"}</Typography>

            <Box mt={2}>
              <Typography variant="h6" color={colors.grey[100]}>
                Note:
              </Typography>
              {isEditing ? (
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  placeholder="Enter note..."
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                />
              ) : (
                <Typography sx={{ whiteSpace: "pre-wrap" }}>
                  {company.note || "—"}
                </Typography>
              )}
            </Box>
          </Grid>
        </Grid>

        <Box display="flex" justifyContent="flex-end" gap={2} mt={4}>
          {!isEditing ? (
            <Button
              variant="contained"
              color="warning"
              onClick={() => setIsEditing(true)}
            >
              ✏️ Edit
            </Button>
          ) : (
            <Button
              variant="contained"
              color="success"
              onClick={handleSaveChanges}
            >
              💾 Save Change
            </Button>
          )}
          <Button
            variant="outlined"
            color="inherit"
            onClick={() => navigate(-1)}
          >
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
        <Alert
          onClose={handleCloseSnackbar}
          severity="success"
          sx={{ width: "100%" }}
        >
          Changes saved successfully!
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ViewCompany;
