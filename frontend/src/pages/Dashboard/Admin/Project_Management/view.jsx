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
  const [preview, setPreview] = useState({
    logo: null,
    legalDocs: []
  });


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
            measurementMethod: project.measurementMethod || "",
            emissionFactor: project.emissionFactorKgPerKwh || "",
            logo: null, // file upload
            logoUrl: project.logo || "", // link để preview
            legalDocsUrl: project.legalDocsFile
              ? Array.isArray(project.legalDocsFile)
                ? project.legalDocsFile
                : [project.legalDocsFile]
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

      const form = new FormData();

      // Headers
      form.append("title", formData.projectname);
      form.append("description", formData.shortdescription);
      form.append("commitments", formData.commitments);
      form.append("technicalIndicators", formData.technicalIndicators);
      form.append("measurementMethod", formData.measurementMethod);
      form.append("emissionFactorKgPerKwh", formData.emissionFactor);
      form.append("status", formData.status);

      // Dates required by API
      if (formData.starteddate)
        form.append(
          "startedDate",
          dayjs(formData.starteddate, ["DD/MM/YYYY", "YYYY-MM-DD"]).format(
            "YYYY-MM-DD"
          )
        );

      if (formData.enddate)
        form.append(
          "endDate",
          dayjs(formData.enddate, ["DD/MM/YYYY", "YYYY-MM-DD"]).format(
            "YYYY-MM-DD"
          )
        );

      // Files
      if (formData.logo) form.append("logo", formData.logo);

      if (formData.legalDocsFile?.length) {
        formData.legalDocsFile.forEach((file) => {
          form.append("legalDocsFile", file);
        });
      }

      // Call API
      const res = await updateProjectById(formData.projectid, form);

      if (res?.responseStatus?.responseCode === "00000000") {
        showSnackbar("success", "Update successfully!");
        setIsEditing(false);
      } else {
        showSnackbar(
          "error",
          res?.responseStatus?.responseMessage || "Update failed!"
        );
      }
    } catch (err) {
      showSnackbar("error", err?.response?.data?.message || "Update error");
    } finally {
      setUpdateLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    if (name === "emissionFactor" && !/^\d*\.?\d*$/.test(value)) return;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleFileUpload = (e, type) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    if (type === "logo") {
      const file = files[0];
      setPreview((prev) => ({
        ...prev,
        logo: URL.createObjectURL(file)
      }));
      handleChange("logoFile", file);
    }

    if (type === "legalDocsFile") {
      const previews = Array.from(files).map((f) => ({
        name: f.name,
        url: URL.createObjectURL(f),
      }));

      setPreview((prev) => ({
        ...prev,
        legalDocs: previews
      }));
      handleChange("legalDocsFile", files);
    }
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
    <Box
      m="20px"
      sx={{ marginLeft: "290px", maxWidth: "1150px", width: "100%" }}
    >
      <Header
        title="PROJECT DETAILS"
        subtitle="Detailed information of project"
      />
      <Paper
        elevation={3}
        sx={{
          p: 3,
          mt: 3,
          width: "100%",
          backgroundColor: colors.primary[400],
        }}
      >
        <LocalizationProvider dateAdapter={AdapterDayjs} adapterLocale="en">
          <Grid
            container
            spacing={10} // khoảng cách giữa các cột
            sx={{
              "& .MuiTextField-root": {
                width: "100%",
                mb: 0.3,
              },
              "& .MuiFormControl-root": {
                width: "100%",
              },
              "& .MuiInputBase-root": {
                borderRadius: "10px",
              },
            }}
          >
            {/* COLUMN 1 */}
            <Grid
              item
              xs={12}
              md={4}
              sx={{
                flexBasis: "30%", // chiếm 30% chiều ngang container
                maxWidth: "30%", // giới hạn chiều rộng
              }}
            >
              <Typography
                variant="h5"
                fontWeight="700"
                color="secondary"
                gutterBottom
              >
                General Information
              </Typography>
              {/* Project ID */}
              <Box mb={0.5}>
                <Typography variant="h6" fontWeight="600" gutterBottom>
                  Project ID:
                </Typography>
                <Typography>{formData.projectid}</Typography>
              </Box>
              {/* Project Name */}
              <Box mb={0.5}>
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
                  <TextField
                    value={formData.projectname || "N/A"}
                    multiline
                    fullWidth
                    InputProps={{
                      readOnly: true,
                    }}
                    inputProps={{ style: { cursor: "pointer" } }}
                    variant="outlined"
                    size="small"
                    minRows={1}
                    sx={{
                      mb: 0.5,
                      backgroundColor: "rgba(255,255,255,0.08)",
                      borderRadius: "8px",
                      "& .MuiInputBase-input.Mui-disabled": {
                        WebkitTextFillColor: "#ccc", // màu chữ nếu theme tối
                      },
                    }}
                  />
                )}
              </Box>
              {/* Start Date */}
              <Box mb={0.5}>
                <Typography variant="h6" fontWeight="600" gutterBottom>
                  Started Date:
                </Typography>
                {isEditing ? (
                  <DatePicker
                    format="DD/MM/YYYY"
                    value={
                      formData.starteddate
                        ? dayjs(formData.starteddate, [
                          "YYYY-MM-DD",
                          "DD/MM/YYYY",
                        ])
                        : null
                    }
                    onChange={(date) =>
                      setFormData((prev) => ({
                        ...prev,
                        starteddate: date ? date.format("DD/MM/YYYY") : "",
                      }))
                    }
                    sx={{ width: "100%" }}
                  />
                ) : (
                  <Typography>
                    {formData.starteddate
                      ? dayjs(formData.starteddate, [
                        "YYYY-MM-DD",
                        "DD/MM/YYYY",
                      ]).format("DD/MM/YYYY")
                      : "—"}
                  </Typography>
                )}
              </Box>
              {/* End Date */}
              <Box mb={0.5}>
                <Typography variant="h6" fontWeight="600" gutterBottom>
                  End Date:
                </Typography>
                {isEditing ? (
                  <DatePicker
                    format="DD/MM/YYYY"
                    value={
                      formData.enddate
                        ? dayjs(formData.enddate, ["YYYY-MM-DD", "DD/MM/YYYY"])
                        : null
                    }
                    onChange={(date) =>
                      setFormData((prev) => ({
                        ...prev,
                        enddate: date ? date.format("DD/MM/YYYY") : "",
                      }))
                    }
                    sx={{ width: "100%" }}
                  />
                ) : (
                  <Typography>
                    {formData.enddate
                      ? dayjs(formData.enddate, [
                        "YYYY-MM-DD",
                        "DD/MM/YYYY",
                      ]).format("DD/MM/YYYY")
                      : "indefinitely"}
                  </Typography>
                )}
              </Box>
              {/* Description */}
              <Box mb={0.5}>
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
                  />
                ) : (
                  <TextField
                    value={formData.shortdescription || "N/A"}
                    multiline
                    fullWidth
                    InputProps={{
                      readOnly: true,
                    }}
                    inputProps={{ style: { cursor: "pointer" } }}
                    variant="outlined"
                    size="small"
                    minRows={2}
                    sx={{
                      mb: 0.5,
                      backgroundColor: "rgba(255,255,255,0.08)",
                      borderRadius: "8px",
                      "& .MuiInputBase-input.Mui-disabled": {
                        WebkitTextFillColor: "#ccc", // màu chữ nếu theme tối
                      },
                    }}
                  />
                )}
              </Box>
            </Grid>

            {/* COLUMN 2 */}
            <Grid
              item
              xs={12}
              md={4}
              sx={{
                flexBasis: "30%", // chiếm 30% chiều ngang container
                maxWidth: "30%", // giới hạn chiều rộng
              }}
            >
              <Typography
                variant="h5"
                fontWeight="700"
                color="secondary"
                gutterBottom
              >
                Technical Information
              </Typography>

              {/* Các field khác */}
              {["measurementMethod", "commitments", "technicalIndicators"].map(
                (field) => (
                  <Box key={field} mb={0.5}>
                    <Typography variant="h6" fontWeight="600" gutterBottom>
                      {field
                        .replace(/([A-Z])/g, " $1")
                        .replace(/^./, (str) => str.toUpperCase())}
                      :
                    </Typography>
                    {isEditing ? (
                      <TextField
                        fullWidth
                        multiline
                        rows={3}
                        name={field}
                        value={formData[field]}
                        onChange={handleChange}
                      />
                    ) : (
                      <TextField
                        value={formData[field] || "N/A"}
                        multiline
                        fullWidth
                        InputProps={{
                          readOnly: true,
                        }}
                        inputProps={{ style: { cursor: "pointer" } }}
                        variant="outlined"
                        size="small"
                        minRows={2}
                        sx={{
                          mb: 0.5,
                          backgroundColor: "rgba(255,255,255,0.08)",
                          borderRadius: "8px",
                          "& .MuiInputBase-input.Mui-disabled": {
                            WebkitTextFillColor: "#ccc", // màu chữ nếu theme tối
                          },
                        }}
                      />
                    )}
                  </Box>
                )
              )}
            </Grid>

            {/* COLUMN 3 */}
            <Grid item xs={12} md={4}>
              {/* Documents & Status */}
              <Typography
                variant="h5"
                fontWeight="700"
                color="secondary"
                gutterBottom
              >
                Documents & Status
              </Typography>
              {/* Emission Factor */}
              <Box mb={2}>
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
                  <Typography>{formData.emissionFactor || "—"}</Typography>
                )}
              </Box>
              {/* Logo */}
              {/* Logo */}
              <Box mb={2}>
                <Typography variant="h6" fontWeight="600" gutterBottom>
                  Logo:
                </Typography>

                {isEditing ? (
                  <>
                    {/* Nút Upload */}
                    <Button
                      variant="contained"
                      component="label"
                      color="info"
                      fullWidth
                    >
                      Upload Logo
                      <input
                        hidden
                        type="file"
                        accept="image/*"
                        onChange={(e) => handleFileUpload(e, "logo")}
                      />
                    </Button>

                    {/* Preview logo vừa chọn */}
                    {preview.logo && (
                      <Box mt={2}>
                        <img
                          src={preview.logo}
                          alt="preview"
                          style={{ width: 150, borderRadius: 8 }}
                        />
                      </Box>
                    )}
                  </>
                ) : formData.logoUrl ? (
                  <Button
                    variant="contained"
                    color="info"
                    size="small"
                    onClick={() => window.open(formData.logoUrl)}
                  >
                    View Logo
                  </Button>
                ) : (
                  <Typography>—</Typography>
                )}
              </Box>


              {/* Legal Docs */}
              <Box mb={2}>
                <Typography variant="h6" fontWeight="600" gutterBottom>
                  Legal Docs:
                </Typography>
                {isEditing ? (
                  <>
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
                        onChange={(e) => handleFileUpload(e, "legalDocsFile")}
                      />
                    </Button>

                    {/* Preview Document List */}
                    {preview.legalDocs.length > 0 && (
                      <Box mt={2}>
                        {preview.legalDocs.map((file, idx) => (
                          <Box key={idx} mb={1}>
                            <Button
                              variant="outlined"
                              size="small"
                              onClick={() => window.open(file.url)}
                            >
                              Preview: {file.name}
                            </Button>
                          </Box>
                        ))}
                      </Box>
                    )}
                  </>
                ) : formData.legalDocsUrl?.length ? (
                  formData.legalDocsUrl.map((url, idx) => (
                    // Hiển thị nút xem từng tài liệu
                    <Button
                      key={idx}
                      variant="contained"
                      color="secondary"
                      size="small"
                      onClick={() => window.open(url)} // Mở file trong tab mới
                      sx={{ mb: 1 }}
                    >
                      View Document
                    </Button>
                  ))
                ) : (
                  <Typography>—</Typography>
                )}
              </Box>

              <Box mb={2}>
                <Typography variant="h6" fontWeight="600" gutterBottom>
                  Status:
                </Typography>
                {isEditing ? (
                  <TextField
                    select
                    name="status"
                    value={formData.status}
                    onChange={handleChange}
                    fullWidth
                  >
                    <MenuItem value="OPEN">OPEN</MenuItem>
                    <MenuItem value="COMING_SOON">COMING SOON</MenuItem>
                    <MenuItem value="CLOSE">CLOSE</MenuItem>
                  </TextField>
                ) : (
                  <Typography>{formData.status}</Typography>
                )}
              </Box>
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
              startIcon={
                updateLoading && <CircularProgress size={20} color="inherit" />
              }
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
      {SnackbarComponent}
    </Box>
  );
};

export default ViewProject;
