import React, { useState, useEffect } from "react";
import {
  Box,
  Typography,
  Button,
  Grid,
  TextField,
  Paper,
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
import { useSnackbar } from "@/hooks/useSnackbar.jsx";


const ViewProject = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const { id } = useParams();
  const navigate = useNavigate();

  const [formData, setFormData] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [updateLoading, setUpdateLoading] = useState(false);
  const { showSnackbar, SnackbarComponent } = useSnackbar();

  //API FETCH
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
            legaldocurl: Array.isArray(project.legalDocsFile)
              ? project.legalDocsFile
              : project.legalDocsFile
                ? [project.legalDocsFile]
                : [],
            status: project.status || "OPEN",
            commitments: project.commitments || "",
            technicalIndicators: project.technicalIndicators || "",

          });
        }
      } catch (err) {
        console.error("Error fetching project:", err);

        const message =
          err?.response?.data?.responseStatus?.responseDesc ||
          err?.response?.data?.message ||
          err?.message ||
          "Failed to fetch project.";
        showSnackbar("error", message);
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
        legaldocurl: Array.isArray(formData.legalDocsFile)
          ? formData.legalDocsFile
          : formData.legalDocsFile
            ? [formData.legalDocsFile]
            : [],
        status: formData.status,
        commitments: formData.commitments,
        technicalIndicators: formData.technicalIndicators,

      };

      const res = await updateProjectById(formData.projectid, payload);
      if (res?.responseStatus?.responseCode === "00000000") {
        setIsEditing(false);
        showSnackbar("success", "Update successfully!");
      } else {
        showSnackbar("error", res?.responseStatus?.responseMessage || "Update failed!");
      }
    } catch (err) {
      err?.response?.data?.responseStatus?.responseDesc ||
        err?.response?.data?.responseStatus?.responseMessage ||
        err?.response?.data?.message ||
        err?.message ||
        "Error updating project!";
    } finally {
      setUpdateLoading(false);
    }
  };


  const handleChange = (e) => {
    const { name, value } = e.target;
    if (name === "emissionFactor" && !/^\d*\.?\d*$/.test(value)) return;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleFileUpload = (e, field) => {
    const files = Array.from(e.target.files);
    if (files.length > 0) {
      const newFiles = files.map((file) => URL.createObjectURL(file));
      setFormData((prev) => ({
        ...prev,
        [field]: [...(prev[field] || []), ...newFiles],
      }));
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
    <Box m="20px" sx={{ marginLeft: "290px" }}>
      <Header title="PROJECT DETAILS" subtitle="Detailed information of project" />
      <Paper elevation={3} sx={{ p: 3, mt: 3, width: "100%", backgroundColor: colors.primary[400] }}>
        <LocalizationProvider dateAdapter={AdapterDayjs} adapterLocale="en">
          <Grid
            container
            spacing={3} // tăng khoảng cách giữa các cột
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
            {/* COLUMN 1  */}
            <Grid item xs={12} md={4} sx={{ minWidth: 0 }}>
              <Typography variant="h5" fontWeight="700" color="secondary" gutterBottom>
                General Info
              </Typography>

              <Box mb={2} display="flex" flexDirection="column" justifyContent="space-between" sx={{ minHeight: 65 }}>
                <Typography variant="h6" fontWeight="600" gutterBottom>
                  Project ID:
                </Typography>
                <Typography mb={2}>{formData.projectid}</Typography>
              </Box>

              <Box mb={2} display="flex" flexDirection="column" justifyContent="space-between" sx={{ minHeight: 100 }}>
                <Typography variant="h6" fontWeight="600" gutterBottom>
                  Project Name:
                </Typography>
                {isEditing ? (
                  <TextField
                    fullWidth
                    multiline
                    rows={3}
                    name="projectname"
                    value={formData.projectname}
                    onChange={handleChange}
                  />
                ) : (
                  <Typography mb={2} sx={{ wordBreak: "break-word", whiteSpace: "pre-wrap" }}>
                    {formData.projectname}
                  </Typography>
                )}
              </Box>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Description:
              </Typography>
              {isEditing ? (
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  name="shortdescription"
                  value={formData.shortdescription}
                  onChange={handleChange}
                  sx={{ mb: 2 }}
                />
              ) : (
                <Typography mb={2} sx={{ wordBreak: "break-word", whiteSpace: "pre-wrap",overflowWrap: "break-word", }}>
                  {formData.shortdescription || "—"}
                </Typography>
              )}
            </Grid>

            {/* COLUMN 2  */}
            <Grid item xs={12} md={4}>
              <Typography variant="h5" fontWeight="700" color="secondary" gutterBottom>
                Technical Info
              </Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Started Date:
              </Typography>
              {isEditing ? (
                <DatePicker
                  format="DD/MM/YYYY"
                  value={formData.starteddate ? dayjs(formData.starteddate, ["YYYY-MM-DD", "DD/MM/YYYY"]) : null}
                  onChange={(date) =>
                    setFormData((prev) => ({
                      ...prev,
                      starteddate: date ? date.format("DD/MM/YYYY") : "",
                    }))
                  }
                  sx={{ width: "100%" }}
                />

              ) : (
                <Typography mb={2}>
                  {formData.starteddate
                    ? dayjs(formData.starteddate, ["YYYY-MM-DD", "DD/MM/YYYY"]).format("DD/MM/YYYY")
                    : "—"}
                </Typography>
              )}

              <Box mb={2} display="flex" flexDirection="column" justifyContent="space-between" sx={{ minHeight: 60 }}>
                <Typography variant="h6" fontWeight="600" gutterBottom>
                  End Date:
                </Typography>
                {isEditing ? (
                  <DatePicker
                    format="DD/MM/YYYY"
                    value={formData.enddate ? dayjs(formData.enddate, ["YYYY-MM-DD", "DD/MM/YYYY"]) : null}
                    onChange={(date) =>
                      setFormData((prev) => ({
                        ...prev,
                        enddate: date ? date.format("DD/MM/YYYY") : "",
                      }))
                    }
                    sx={{ width: "100%" }}
                  />

                ) : (
                  <Typography mb={2}>
                    {formData.enddate
                      ? dayjs(formData.enddate, ["YYYY-MM-DD", "DD/MM/YYYY"]).format("DD/MM/YYYY")
                      : "—"}
                  </Typography>
                )}
              </Box>


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

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Commitments:
              </Typography>
              {isEditing ? (
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  name="commitments"
                  value={formData.commitments}
                  onChange={handleChange}
                  sx={{ mb: 2 }}
                />
              ) : (
                <Typography mb={2} sx={{ wordBreak: "break-word", whiteSpace: "pre-wrap" }}>
                  {formData.commitments || "—"}
                </Typography>
              )}

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Technical Indicators:
              </Typography>
              {isEditing ? (
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  name="technicalIndicators"
                  value={formData.technicalIndicators}
                  onChange={handleChange}
                  sx={{ mb: 2 }}
                />
              ) : (
                <Typography mb={2} sx={{ wordBreak: "break-word", whiteSpace: "pre-wrap" }}>
                  {formData.technicalIndicators || "—"}
                </Typography>
              )}

            </Grid>

            {/*  COLUMN 3  */}
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

                  {/* Hiển thị preview nhỏ */}
                  {formData.logo && (
                    <Box mt={1} display="flex" justifyContent="center">
                      <img
                        src={Array.isArray(formData.logo) ? formData.logo[0] : formData.logo}
                        alt="Logo Preview"
                        style={{
                          width: 80,
                          height: 80,
                          objectFit: "cover",
                          borderRadius: "8px",
                          border: "1px solid #ccc",
                        }}
                      />
                    </Box>
                  )}
                </Box>
              ) : (
                formData.logo ? (
                  <Button
                    variant="contained"
                    color="info"
                    size="small"
                    onClick={() => window.open(Array.isArray(formData.logo) ? formData.logo[0] : formData.logo, "_blank")}
                  >
                    View Logo
                  </Button>
                ) : (
                  <Typography mb={2}>—</Typography>
                )
              )}


              <Typography variant="h6" fontWeight="600" gutterBottom>
                Legal Docs:
              </Typography>

              {isEditing ? (
                <Box mb={2}>
                  <Button
                    variant="contained"
                    component="label"
                    color="secondary"
                    fullWidth
                  >
                    Upload Document(s)
                    <input
                      hidden
                      type="file"
                      multiple
                      accept=".pdf,.doc,.docx"
                      onChange={(e) => handleFileUpload(e, "legaldocurl")}
                    />
                  </Button>

                  {/* Hiển thị danh sách file vừa upload */}
                  {formData.legaldocurl?.length > 0 && (
                    <Box mt={1}>
                      {formData.legaldocurl.map((url, idx) => (
                        <Typography key={idx} variant="body2" sx={{ color: "#aaa" }}>
                          Document {idx + 1} ready to upload
                        </Typography>
                      ))}
                    </Box>
                  )}
                </Box>
              ) : formData.legaldocurl?.length > 0 ? (
                <Box mb={2} display="flex" flexDirection="column" gap={1}>
                  {formData.legaldocurl.map((url, idx) => (
                    <Button
                      key={idx}
                      variant="contained"
                      color="secondary"
                      size="small"
                      onClick={() => window.open(url, "_blank")}
                    >
                      View Document {idx + 1}
                    </Button>
                  ))}
                </Box>
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
      {SnackbarComponent}
    </Box>
  );
};

export default ViewProject;
