import {
  Box,
  Typography,
  Grid,
  Paper,
  TextField,
  Button,
  Divider,
  useTheme,
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import { tokens } from "@/themeCVA";
import Header from "@/components/Chart/Header.jsx";
import { mockDataLogCVA } from "@/data/mockData";

const ViewLog = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();
  const { id } = useParams();

  // lấy dữ liệu từ mockDataLogCVA
  const log = mockDataLogCVA.find((item) => item.id === Number(id));

  if (!log) {
    return (
      <Box m="20px">
        <Typography variant="h6" color="error">
          Log not found.
        </Typography>
        <Button
          variant="outlined"
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate("/cva/log")}
          sx={{
            mt: 2,
            borderColor: colors.blueAccent[400],
            color: colors.blueAccent[400],
            textTransform: "none",
          }}
        >
          Back to List
        </Button>
      </Box>
    );
  }

  return (
    <Box m="20px">
      <Header title="LOG DETAILS" subtitle="View log information" />

      <Paper
        elevation={2}
        sx={{
          p: 4,
          borderRadius: 3,
          backgroundColor: colors.primary[400],
          color: colors.grey[100],
        }}
      >
        {/* Header trong card */}
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
          <Typography variant="h5" fontWeight="bold">
            Log Information
          </Typography>
        </Box>

        <Divider sx={{ mb: 3, borderColor: colors.grey[700] }} />

        {/* Log Details */}
        <Grid container spacing={3} mb={4}>
          <Grid item xs={12} md={6}>
            <TextField
              label="Log ID"
              fullWidth
              value={log.logid}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Performer"
              fullWidth
              value={log.performer}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Operation"
              fullWidth
              value={log.operation}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Target"
              fullWidth
              value={log.target}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Time"
              fullWidth
              value={log.time}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12}>
            <TextField
              label="Note"
              fullWidth
              multiline
              minRows={3}
              value={log.note}
              InputProps={{ readOnly: true }}
            />
          </Grid>
        </Grid>

        {/* Back Button */}
        <Box display="flex" justifyContent="flex-end">
          <Button
            variant="outlined"
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate("/cva/log_management")}
            sx={{
              borderColor: colors.blueAccent[400],
              color: colors.blueAccent[400],
              textTransform: "none",
            }}
          >
            Back to List
          </Button>
        </Box>
      </Paper>
    </Box>
  );
};

export default ViewLog;
