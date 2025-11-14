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
  Paper,
  TextField,
} from "@mui/material";
import { useTheme } from "@mui/material/styles";
import { tokens } from "@/themeCVA";
import Header from "@/components/Chart/Header.jsx";
import { useState } from "react";
import dayjs from "dayjs";
import { LocalizationProvider } from "@mui/x-date-pickers/LocalizationProvider";
import { AdapterDayjs } from "@mui/x-date-pickers/AdapterDayjs";
import { DatePicker } from "@mui/x-date-pickers/DatePicker";

const ViewCredit = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  const initial = mockDataCreditsCVA.find((item) => item.id === parseInt(id));

  if (!initial) {
    return (
      <Box m="20px">
        <Typography variant="h4" color={colors.grey[100]}>
          Credit not found
        </Typography>
      </Box>
    );
  }

  const [credit, setCredit] = useState(initial);
  const [isEditing, setIsEditing] = useState(false);
  const [status, setStatus] = useState(credit?.status || "");
  const [issuedDay, setIssuedDay] = useState(
    credit?.issuedday ? dayjs(credit.issuedday, "DD/MM/YYYY") : null
  );
  const [note, setNote] = useState(credit?.note || "");
  const [openSnackbar, setOpenSnackbar] = useState(false);

  const handleUpdate = () => {
    const updated = {
      ...credit,
      status,
      issuedday: issuedDay ? issuedDay.format("DD/MM/YYYY") : credit.issuedday,
      note: status === "Rejected" ? note : "",
    };

    setCredit(updated);
    setIsEditing(false);
    setOpenSnackbar(true);
  };

  const handleCloseSnackbar = () => setOpenSnackbar(false);

  const colorMap = {
    Active: "#4CAF50",
    Revoked: "#E53935",
    Pending: "#42A5F5",
    Sold: "#FFB300",
    Listed: "#FDD835",
    Retired: "#757575",
    Approved: "#4CAF50",
    Rejected: "#E53935",
  };
  const color = colorMap[credit.status] || colors.grey[300];

  return (
    <Box m="20px">
      <Header
        title="CREDIT DETAIL"
        subtitle={`Details of ${credit.creditid}`}
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
              Credit ID:
            </Typography>
            <Typography>{credit.creditid}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Company / Chủ sở hữu:
            </Typography>
            <Typography>{credit.aggregator}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Project Name:
            </Typography>
            <Typography>{credit.projectname}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Number of Credits (tCO₂):
            </Typography>
            <Typography>{credit.numbercredit}</Typography>
          </Grid>

          {/* Cột phải */}
          <Grid item xs={12} sm={6}>
            <Typography variant="h6" color={colors.grey[100]}>
              Estimated Value ($):
            </Typography>
            <Typography>{credit.estimatedvalue}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Issued Day:
            </Typography>
            {isEditing ? (
              <LocalizationProvider dateAdapter={AdapterDayjs}>
                <DatePicker
                  value={issuedDay}
                  onChange={(newValue) => setIssuedDay(newValue)}
                  format="DD/MM/YYYY"
                  slotProps={{ textField: { fullWidth: true } }}
                />
              </LocalizationProvider>
            ) : (
              <Typography>
                {credit.issuedday ? credit.issuedday : "—"}
              </Typography>
            )}

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
                  {["Pending", "Approved", "Rejected"].map((s) => (
                    <MenuItem key={s} value={s}>
                      {s}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            ) : (
              <Typography
                sx={{ color, fontWeight: 600, textTransform: "capitalize" }}
              >
                {credit.status}
              </Typography>
            )}

            {isEditing && status === "Rejected" && (
              <Box mt={2}>
                <Typography variant="h6" color={colors.grey[100]}>
                  Note:
                </Typography>
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  placeholder="Enter rejection reason..."
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                />
              </Box>
            )}

            {!isEditing && credit.status === "Rejected" && credit.note && (
              <Box mt={2}>
                <Typography variant="h6" color={colors.grey[100]}>
                  Note:
                </Typography>
                <Typography>{credit.note}</Typography>
              </Box>
            )}

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Expired Day:
            </Typography>
            <Typography>{credit.expiredday}</Typography>

            <Typography variant="h6" color={colors.grey[100]} mt={2}>
              Linked Certificate:
            </Typography>
            <Typography sx={{ mb: 1 }}>{credit.linkedcertificate}</Typography>
            <Box display="flex" gap={2}>
              <Button variant="contained" color="info">
                View
              </Button>
              <Button variant="contained" color="secondary">
                Download
              </Button>
            </Box>
          </Grid>
        </Grid>

        <Box display="flex" justifyContent="flex-end" gap={2} mt={4}>
          {!isEditing ? (
            <Button
              variant="contained"
              color="warning"
              onClick={() => {
                setIsEditing(true);
                setStatus(credit.status);
                setIssuedDay(
                  credit.issuedday
                    ? dayjs(credit.issuedday, "DD/MM/YYYY")
                    : null
                );
                setNote(credit.note || "");
              }}
            >
              Edit
            </Button>
          ) : (
            <Button variant="contained" color="success" onClick={handleUpdate}>
              Update
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
      {/* Audit Trail Section */}
      <Paper
        elevation={3}
        sx={{
          backgroundColor: colors.primary[400],
          p: "20px",
          borderRadius: "10px",
          mt: 4,
        }}
      >
        <Typography variant="h5" color={colors.grey[100]} mb={2}>
          Audit Trail
        </Typography>
        {mockDataLogCVA.slice(0, 4).map((log) => (
          <Box
            key={log.id}
            sx={{
              borderBottom: `1px solid ${colors.grey[700]}`,
              mb: 1,
              pb: 1,
            }}
          >
            <Typography variant="body1" color={colors.greenAccent[200]}>
              {log.operation} by {log.performer} on {log.time}
            </Typography>
            <Typography variant="body2" color={colors.grey[200]}>
              {log.note}
            </Typography>
          </Box>
        ))}
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
          Status updated successfully!
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ViewCredit;
