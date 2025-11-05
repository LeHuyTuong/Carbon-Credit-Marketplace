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
  MenuItem,
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

  const [formData, setFormData] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [updateLoading, setUpdateLoading] = useState(false);
  const [openSnackbar, setOpenSnackbar] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState("");
  const [snackbarSeverity, setSnackbarSeverity] = useState("success");

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
            starteddate: project.startedDate || "",
            enddate: project.endDate || "",
            totalcompanies: project.technicalIndicators || "",
            measurementmethod: project.measurementMethod || "",
            emissionFactor: project.emissionFactorKgPerKwh || "",
            logo: project.logo || "",
            legaldocurl: project.legalDocsFile || "",
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
        technicalIndicators: formData.totalcompanies,
        measurementMethod: formData.measurementmethod,
        emissionFactorKgPerKwh: parseFloat(formData.emissionFactor) || 0,
        logo: formData.logo || "",
        legalDocsFile: formData.legaldocurl || "",
        status: formData.status,
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
    if (name === "emissionFactor" && !/^\d*\.?\d*$/.test(value)) return;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleFileUpload = (e, field) => {
    const file = e.target.files[0];
    if (file) {
      const fakeURL = URL.createObjectURL(file);
      setFormData((prev) => ({ ...prev, [field]: fakeURL }));
    }
  };

  if (loading)
    return (
      <Box m="20px" display="flex" justifyContent="center" alignItems="center" height="60vh">
        <CircularProgress />
      </Box>
    );

  if (!formData)
    return (
      <Box m="20px">
        <Typography variant="h5" color="error">
          Project not found.
        </Typography>
        <Button variant="contained" sx={{ mt: 2 }} onClick={() => navigate("/admin/project_management")}>
          Back
        </Button>
      </Box>
    );

  return (
    <Box m="20px">
      <Header title="PROJECT DETAILS" subtitle="Detailed information of project" />
      <Paper elevation={3} sx={{ p: 3, mt: 3, backgroundColor: colors.primary[400] }}>
        <LocalizationProvider dateAdapter={AdapterDayjs} adapterLocale="en">
          <Grid
            container
            spacing={4} // 👉 tăng khoảng cách giữa các cột
            sx={{
              "& .MuiTextField-root": {
                width: "100%",
                mb: 2,
              },
              "& .MuiFormControl-root": {
                width: "100%",
              },
              "& .MuiInputBase-root": {
                borderRadius: "10px",
              },
            }}
          >
            {/* ===== COLUMN 1 ===== */}
            <Grid item xs={12} md={4}>
              <Typography variant="h5" fontWeight="700" color="secondary" gutterBottom>
                General Info
              </Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Project ID:
              </Typography>
              <Typography mb={2}>{formData.projectid}</Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Project Name:
              </Typography>
              {isEditing ? (
                <TextField
                  name="projectname"
                  value={formData.projectname}
                  onChange={handleChange}
                />
              ) : (
                <Typography mb={2} sx={{ wordBreak: "break-word", whiteSpace: "pre-wrap" }}>
                  {formData.projectname}
                </Typography>
              )}

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Description:
              </Typography>
              {isEditing ? (
                <TextField
                  multiline
                  rows={3}
                  name="shortdescription"
                  value={formData.shortdescription}
                  onChange={handleChange}
                />
              ) : (
                <Typography mb={2} sx={{ wordBreak: "break-word", whiteSpace: "pre-wrap" }}>
                  {formData.shortdescription || "—"}
                </Typography>
              )}
            </Grid>

            {/* ===== COLUMN 2 ===== */}
            <Grid item xs={12} md={4}>
              <Typography variant="h5" fontWeight="700" color="secondary" gutterBottom>
                Technical Info
              </Typography>

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
                  sx={{ width: "100%" }}
                />
              ) : (
                <Typography mb={2}>{formData.starteddate || "—"}</Typography>
              )}

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
                  sx={{ width: "100%" }}
                />
              ) : (
                <Typography mb={2}>{formData.enddate || "—"}</Typography>
              )}

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Measurement Method:
              </Typography>
              {isEditing ? (
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  name="measurementmethod"
                  value={formData.measurementmethod}
                  onChange={handleChange}
                  sx={{ mb: 2 }}
                />

              ) : (
                <Typography mb={2} sx={{ wordBreak: "break-word", whiteSpace: "pre-wrap" }}>
                  {formData.measurementmethod || "—"}
                </Typography>
              )}
            </Grid>

            {/* ===== COLUMN 3 ===== */}
            <Grid item xs={12} md={4}>
              <Typography variant="h5" fontWeight="700" color="secondary" gutterBottom>
                Documents & Status
              </Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Emission Factor (Kg/Kwh):
              </Typography>
              {isEditing ? (
                <TextField
                  name="emissionFactor"
                  value={formData.emissionFactor}
                  onChange={handleChange}
                />
              ) : (
                <Typography mb={2}>{formData.emissionFactor || "—"}</Typography>
              )}

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Logo:
              </Typography>
              {isEditing ? (
                <Box mb={2}>
                  <Button variant="contained" component="label" color="info" fullWidth>
                    Upload Logo
                    <input hidden type="file" accept="image/*" onChange={(e) => handleFileUpload(e, "logo")} />
                  </Button>
                </Box>
              ) : formData.logo ? (
                <Button variant="contained" color="info" size="small" onClick={() => window.open(formData.logo, "_blank")}>
                  View Logo
                </Button>
              ) : (
                <Typography mb={2}>—</Typography>
              )}

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Legal Docs:
              </Typography>
              {isEditing ? (
                <Box mb={2}>
                  <Button variant="contained" component="label" color="secondary" fullWidth>
                    Upload Document
                    <input hidden type="file" accept=".pdf,.doc,.docx" onChange={(e) => handleFileUpload(e, "legaldocurl")} />
                  </Button>
                </Box>
              ) : formData.legaldocurl ? (
                <Button variant="contained" color="secondary" size="small" onClick={() => window.open(formData.legaldocurl, "_blank")}>
                  View Document
                </Button>
              ) : (
                <Typography mb={2}>—</Typography>
              )}

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Status:
              </Typography>
              {isEditing ? (
                <TextField
                  select
                  name="status"
                  value={formData.status}
                  onChange={handleChange}
                >
                  <MenuItem value="OPEN">OPEN</MenuItem>
                  <MenuItem value="COMING_SOON">COMING SOON</MenuItem>
                  <MenuItem value="CLOSE">CLOSE</MenuItem>
                </TextField>
              ) : (
                <Typography mb={2}>{formData.status}</Typography>
              )}
            </Grid>
          </Grid>

        </LocalizationProvider>

        {/* ACTION BUTTONS */}
        <Box display="flex" justifyContent="flex-end" mt={4} gap={2}>
          {!isEditing ? (
            <Button variant="contained" color="secondary" onClick={handleEdit} sx={{ fontWeight: 600 }}>
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
          <Button variant="outlined" color="info" onClick={() => navigate("/admin/project_management")} sx={{ fontWeight: 600 }}>
            Back
          </Button>
        </Box>
      </Paper>

      <Snackbar open={openSnackbar} autoHideDuration={2500} onClose={handleCloseSnackbar} anchorOrigin={{ vertical: "bottom", horizontal: "right" }}>
        <Alert onClose={handleCloseSnackbar} severity={snackbarSeverity} sx={{ width: "100%" }}>
          {snackbarMessage}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ViewProject;
