import React, { useEffect, useState } from "react";
import {
  Box,
  Typography,
  CircularProgress,
  Paper,
  Button,
  TextField,
  Snackbar,
  Alert,
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import {
  updateApplicationDecision,
  getProjectApplicationByIdForCVA,
} from "@/apiCVA/registrationCVA.js";
import Header from "@/components/Chart/Header";
import { useSnackbar } from "@/hooks/useSnackbar.jsx";

const ApplicationEdit = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [application, setApplication] = useState(null);
  const [loading, setLoading] = useState(true);
  const [finalReviewNote, setFinalReviewNote] = useState("");
  const { showSnackbar, SnackbarComponent } = useSnackbar();

  //  Fetch chi tiết application
  useEffect(() => {
    const fetchApplication = async () => {
      try {
        const data = await getProjectApplicationByIdForCVA(id);
        const appData = data?.responseData || data?.response || data;

        if (appData && (appData.id || appData.applicationId)) {
          setApplication(appData);
        } else {
          throw new Error("Application not found");
        }
      } catch (error) {
        console.error(" Error fetching application:", error);
        showSnackbar("error", "Failed to fetch application.");
      } finally {
        setLoading(false);
      }
    };

    fetchApplication();
  }, [id, showSnackbar]);

  //  Submit quyết định Approve / Reject
  const handleDecision = async (approved) => {
    try {
      const payload = {
        approved,
        note: finalReviewNote || (approved ? "Approved by CVA" : "Rejected by CVA"),
      };

      console.log(" Sending decision payload:", payload);
      const result = await updateApplicationDecision(id, payload);

      const code = result?.responseStatus?.responseCode;
      const msg = result?.responseStatus?.responseMessage;

      if (code === "00000000" || code === "200") {
        showSnackbar("success", approved ? "Application approved!" : "Application rejected!");

        //  Quay về đúng trang list (dưới nhánh /cva)
        setTimeout(() => {
          navigate("/cva/registration_project_management", { replace: true });
        }, 1000);
      } else {
        throw new Error(msg);
      }
    } catch (error) {
      console.error(" Decision failed:", error);
      showSnackbar("error", error.message || "Decision failed!");
    }
  };

  //  Loading
  if (loading)
    return (
      <Box display="flex" sx={{ marginLeft: "290px" }} justifyContent="center" alignItems="center" height="70vh">
        <CircularProgress />
      </Box>
    );

  //  Không có dữ liệu
  if (!application)
    return (
      <Box textAlign="center" sx={{ marginLeft: "290px" }} mt={5}>
        <Typography variant="h6" color="error">
          Application not found.
        </Typography>
        <Button
          onClick={() => navigate("/cva/registration_project_management")}
          variant="contained"
          sx={{ mt: 2 }}
        >
          Back to List
        </Button>
      </Box>
    );

  const status = application.status;

  return (
    <Box m="20px" sx={{ marginLeft: "290px" }}>
      <Header
        title="CVA DECISION"
        subtitle={`ID: ${application.id || application.applicationId}`}
      />

      <Paper
        sx={{
          p: 4,
          mt: 3,
          borderRadius: 3,
          boxShadow: 4,
          backgroundColor: "#fafafa",
          fontSize: "1rem", // chữ trong Typography, Button vẫn to
          "& .MuiInputBase-input": { fontSize: "1rem" }, // chữ trong ô input
          "& .MuiInputLabel-root": { fontSize: "1rem" }, // chữ label

        }}>
        {/* Thông tin chỉ đọc */}
        <TextField
          label="Project Title"
          value={application.projectTitle || ""}
          fullWidth
          InputProps={{ readOnly: true }}
          sx={{ mt: 2 }} //khoảng cách giữa các ô input
        />
        <TextField
          label="Company Name"
          value={application.companyName || ""}
          fullWidth
          InputProps={{ readOnly: true }}
          sx={{ mt: 4 }}
        />
        <TextField
          label="Current Status"
          value={status}
          fullWidth
          InputProps={{ readOnly: true }}
          sx={{ mt: 4 }}
        />

        {/*  Ghi chú quyết định */}
        <TextField
          label="Final Review Note"
          value={finalReviewNote}
          onChange={(e) => setFinalReviewNote(e.target.value)}
          fullWidth
          multiline
          rows={3}
          sx={{ mt: 4 }}
        />

        {/* Chỉ hiển thị nút duyệt khi đang UNDER_REVIEW */}
        {status === "UNDER_REVIEW" ? (
          <Box mt={3} display="flex" gap={2}>
            <Button variant="contained" color="success" onClick={() => handleDecision(true)}>
              Approved
            </Button>
            <Button variant="contained" color="error" onClick={() => handleDecision(false)}>
              Rejected
            </Button>
            <Button
              variant="outlined"
              color="secondary"
              onClick={() => navigate(`/cva/view_registration_project/${id}`)}
            >
              Cancel
            </Button>
          </Box>
        ) : (
          <Typography sx={{ mt: 3 }} color="text.secondary">
            This application is already {status}.
          </Typography>
        )}
      </Paper>

      {/*  Snackbar */}
      {SnackbarComponent}
    </Box >
  );
};

export default ApplicationEdit;
