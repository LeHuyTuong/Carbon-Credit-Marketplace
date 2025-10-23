import React, { useState, useEffect } from "react";
import {
  Box,
  Typography,
  Button,
  Grid,
  TextField,
  Paper,
  Snackbar,
  Alert,
  useTheme,
  CircularProgress,
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { getProjectById, updateProjectById } from "@/apiAdmin/projectAdmin.js";
import dayjs from "dayjs";
import "dayjs/locale/en";
import { LocalizationProvider } from "@mui/x-date-pickers/LocalizationProvider";
import { AdapterDayjs } from "@mui/x-date-pickers/AdapterDayjs";
import { DatePicker } from "@mui/x-date-pickers/DatePicker";

const ViewProject = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const { id } = useParams();
  const navigate = useNavigate();

  const statusOptions = ["OPEN", "COMING SOON", "CLOSE"];

  const [formData, setFormData] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [updateLoading, setUpdateLoading] = useState(false);
  const [openSnackbar, setOpenSnackbar] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState("");
  const [snackbarSeverity, setSnackbarSeverity] = useState("success");

  // Fetch project details
  useEffect(() => {
    const fetchProject = async () => {
      try {
        const res = await getProjectById(id);
        const project = res?.response;
        if (project) {
          setFormData({
            projectid: project.id,
            projectname: project.title,
            shortdescription: project.description || "",
            starteddate: project.createdDate || "",
            enddate: project.endedDate || "",
            totalexpectedcredits: project.commitments || "",
            totalcompanies: project.technicalIndicators || "",
            measurementmethod: project.measurementMethod || "",
            logo: project.logo || "",
            legaldocurl: project.legalDocsUrl || "",
            status: project.status || "OPEN",
          });
        }
      } catch (err) {
        console.error("Error fetching project:", err);
      } finally {
        setLoading(false);
      }
    };
    fetchProject();
  }, [id]);

  const handleEdit = () => setIsEditing(true);

  const handleUpdate = async () => {
    try {
      setUpdateLoading(true);
      const payload = {
        requestTrace: `trace_${Date.now()}`,
        requestDateTime: new Date().toISOString(),
        title: formData.projectname,
        description: formData.shortdescription,
        commitments: formData.totalexpectedcredits,
        technicalIndicators: formData.totalcompanies,
        measurementMethod: formData.measurementmethod,
        logo: formData.logo || "",
        legalDocsUrl: formData.legaldocurl || "",
      };

      const res = await updateProjectById(formData.projectid, payload);

      if (res?.responseStatus?.responseCode === "00000000") {
        setIsEditing(false);
        setSnackbarMessage("Update successfully!");
        setSnackbarSeverity("success");
        setOpenSnackbar(true);
      } else {
        setSnackbarMessage(res?.responseStatus?.responseMessage || "Update failed!");
        setSnackbarSeverity("error");
        setOpenSnackbar(true);
      }
    } catch (err) {
      console.error("Error updating project:", err);
      setSnackbarMessage("Error updating project!");
      setSnackbarSeverity("error");
      setOpenSnackbar(true);
    } finally {
      setUpdateLoading(false);
    }
  };

  const handleCloseSnackbar = () => setOpenSnackbar(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  if (loading)
    return (
      <Box
        m="20px"
        display="flex"
        justifyContent="center"
        alignItems="center"
        height="60vh"
      >
        <CircularProgress />
      </Box>
    );

  if (!formData)
    return (
      <Box m="20px">
        <Typography variant="h5" color="error">
          Project not found.
        </Typography>
        <Button
          variant="contained"
          sx={{ mt: 2 }}
          onClick={() => navigate("/admin/project_management")}
        >
          Back
        </Button>
      </Box>
    );

  return (
    <Box m="20px">
      <Header title="PROJECT DETAILS" subtitle="Detailed information of project" />

      <Paper
        elevation={3}
        sx={{
          p: 3,
          mt: 3,
          backgroundColor: colors.primary[400],
        }}
      >
        <LocalizationProvider dateAdapter={AdapterDayjs} adapterLocale="en">
          <Grid container spacing={3}>
            {/* LEFT COLUMN */}
            <Grid item xs={12} sm={6}>
              <Typography variant="h6" fontWeight="600" gutterBottom>
                Project ID:
              </Typography>
              <Typography mb={2}>{formData.projectid}</Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Project Name:
              </Typography>
              <Typography mb={2}>{formData.projectname}</Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Short Description:
              </Typography>
              {isEditing ? (
                <TextField
                  fullWidth
                  name="shortdescription"
                  value={formData.shortdescription}
                  onChange={handleChange}
                  multiline
                  rows={3}
                  sx={{ mb: 2 }}
                />
              ) : (
                <Typography mb={2}>{formData.shortdescription || "—"}</Typography>
              )}

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Started Date:
              </Typography>
              {isEditing ? (
                <DatePicker
                  value={formData.starteddate ? dayjs(formData.starteddate) : null}
                  onChange={(date) =>
                    setFormData((prev) => ({
                      ...prev,
                      starteddate: date ? date.format("YYYY-MM-DD") : "",
                    }))
                  }
                  sx={{ mb: 2, width: "100%" }}
                />
              ) : (
                <Typography mb={2}>{formData.starteddate || "—"}</Typography>
              )}
            </Grid>

            {/* RIGHT COLUMN */}
            <Grid item xs={12} sm={6}>
              <Typography variant="h6" fontWeight="600" gutterBottom>
                End Date:
              </Typography>
              {isEditing ? (
                <DatePicker
                  value={formData.enddate ? dayjs(formData.enddate) : null}
                  onChange={(date) =>
                    setFormData((prev) => ({
                      ...prev,
                      enddate: date ? date.format("YYYY-MM-DD") : "",
                    }))
                  }
                  sx={{ mb: 2, width: "100%" }}
                />
              ) : (
                <Typography mb={2}>{formData.enddate || "—"}</Typography>
              )}

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Total Expected Credits:
              </Typography>
              {isEditing ? (
                <TextField
                  fullWidth
                  name="totalexpectedcredits"
                  value={formData.totalexpectedcredits}
                  onChange={handleChange}
                  sx={{ mb: 2 }}
                />
              ) : (
                <Typography mb={2}>{formData.totalexpectedcredits || "—"}</Typography>
              )}

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Technical Indicators:
              </Typography>
              {isEditing ? (
                <TextField
                  fullWidth
                  name="totalcompanies"
                  value={formData.totalcompanies}
                  onChange={handleChange}
                  sx={{ mb: 2 }}
                />
              ) : (
                <Typography mb={2}>{formData.totalcompanies || "—"}</Typography>
              )}

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Measurement Method:
              </Typography>
              {isEditing ? (
                <TextField
                  fullWidth
                  name="measurementmethod"
                  value={formData.measurementmethod}
                  onChange={handleChange}
                  sx={{ mb: 2 }}
                />
              ) : (
                <Typography mb={2}>{formData.measurementmethod || "—"}</Typography>
              )}

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Status:
              </Typography>
              <Typography mb={2}>{formData.status}</Typography>
            </Grid>
          </Grid>
        </LocalizationProvider>

        {/* ACTION BUTTONS */}
        <Box display="flex" justifyContent="flex-end" mt={4} gap={2}>
          {!isEditing ? (
            <Button
              variant="contained"
              color="secondary"
              onClick={handleEdit}
              sx={{ fontWeight: 600 }}
            >
              Edit
            </Button>
          ) : (
            <Button
              variant="contained"
              color="success"
              onClick={handleUpdate}
              sx={{ fontWeight: 600 }}
              disabled={updateLoading}
              startIcon={updateLoading && <CircularProgress size={20} color="inherit" />}
            >
              {updateLoading ? "Updating..." : "Update"}
            </Button>
          )}
          <Button
            variant="outlined"
            color="info"
            onClick={() => navigate("/admin/project_management")}
            sx={{ fontWeight: 600 }}
          >
            Back
          </Button>
        </Box>
      </Paper>

      {/* SNACKBAR */}
      <Snackbar
        open={openSnackbar}
        autoHideDuration={2500}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
      >
        <Alert
          onClose={handleCloseSnackbar}
          severity={snackbarSeverity}
          sx={{ width: "100%" }}
        >
          {snackbarMessage}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ViewProject;
