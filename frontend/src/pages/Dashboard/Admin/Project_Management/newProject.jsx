import {
  Box,
  Button,
  TextField,
  Paper,
  CircularProgress,
  Typography,
} from "@mui/material";
import { Formik } from "formik";
import * as yup from "yup";
import useMediaQuery from "@mui/material/useMediaQuery";
import { useState } from "react";
import { Link } from "react-router-dom";
import Header from "@/components/Chart/Header.jsx";
import { createProject } from "@/apiAdmin/projectAdmin.js";
import { useSnackbar } from "@/hooks/useSnackbar.jsx";
import dayjs from "dayjs";

const NewProjectForm = () => {
  const isNonMobile = useMediaQuery("(min-width:900px)");
  const [loading, setLoading] = useState(false);
  const { showSnackbar, SnackbarComponent } = useSnackbar();
  const [previewUrl, setPreviewUrl] = useState(null);
  const [s3Url, setS3Url] = useState(null);

  const handleFormSubmit = async (values, { resetForm }) => {
    try {
      setLoading(true);
      const formDataToSend = new FormData();

      formDataToSend.append("requestTrace", `trace_${Date.now()}`);
      formDataToSend.append("requestDateTime", new Date().toISOString());
      formDataToSend.append("title", values.title);
      formDataToSend.append("description", values.description);
      formDataToSend.append("commitments", values.commitments);
      formDataToSend.append("technicalIndicators", values.technicalIndicators);
      formDataToSend.append("measurementMethod", values.measurementMethod);
      formDataToSend.append(
        "emissionFactorKgPerKwh",
        parseFloat(values.emissionFactorKgPerKwh) || 0
      );

      if (values.status) formDataToSend.append("status", values.status);
      if (values.endDate) {
        // Chuẩn hóa thủ công nếu nhập dd/MM/yyyy
        const [day, month, year] = values.endDate.split("/");
        const formattedEndDate = `${year}-${month}-${day}`;
        formDataToSend.append("endDate", formattedEndDate);
      }

      if (values.startedDate) {
        const [day, month, year] = values.startedDate.split("/");
        const formattedStartDate = `${year}-${month}-${day}`;
        formDataToSend.append("startedDate", formattedStartDate);
      }


      if (values.logo) formDataToSend.append("logo", values.logo);
      if (values.legalDocsFile)
        formDataToSend.append("legalDocsFile", values.legalDocsFile);

      const response = await createProject(formDataToSend);

      if (response?.responseStatus?.responseCode === "00000000") {
        const logoUrl = response?.responseData?.logo || null;
        setS3Url(logoUrl);
        setPreviewUrl(null);
        showSnackbar("success", "Project created successfully!");
        setTimeout(() => resetForm(), 600);
      } else {
        throw new Error(
          response?.responseStatus?.responseMessage ||
          "Failed to create project"
        );
      }
    } catch (error) {
      console.error("Error creating project:", error);

      const message =
        error?.response?.data?.responseStatus?.responseDesc ||
        error?.response?.data?.message ||
        error?.message ||
        "Failed to create project. Please try again.";

      showSnackbar("error", message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box m={2} sx={{ marginLeft: "290px" }}>
      {/* Header */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Header title="CREATE PROJECT" subtitle="Create a new carbon project" />
        <Button
          component={Link}
          to="/admin/project_management"
          variant="outlined"
          color="secondary"
          sx={{ textTransform: "none", fontWeight: 600 }}
        >
          ← Back
        </Button>
      </Box>

      {/* Main Paper */}
      <Paper
        elevation={3}
        sx={{
          p: 3,
          borderRadius: 2,
          backgroundColor: "background.paper",
          maxWidth: "1100px",
          mx: "auto",
          overflow: "hidden",
          maxHeight: "calc(100vh - 150px)",
        }}
      >
        <Formik
          onSubmit={handleFormSubmit}
          initialValues={initialValues}
          validationSchema={checkoutSchema}
        >
          {({
            values,
            errors,
            touched,
            handleBlur,
            handleChange,
            handleSubmit,
            setFieldValue,
          }) => (
            <form onSubmit={handleSubmit}>
              <Box
                display="grid"
                gridTemplateColumns={isNonMobile ? "repeat(3, 1fr)" : "1fr"}
                gap="16px"
              >
                {/* Title */}
                <TextField
                  fullWidth
                  size="small"
                  variant="outlined"
                  label="Title"
                  name="title"
                  value={values.title}
                  onBlur={handleBlur}
                  onChange={handleChange}
                  error={!!touched.title && !!errors.title}
                  helperText={touched.title && errors.title}
                  multiline
                  minRows={2}
                  maxRows={8}

                />

                {/* Commitments */}
                <TextField
                  fullWidth
                  size="small"
                  variant="outlined"
                  label="Commitments"
                  name="commitments"
                  value={values.commitments}
                  onBlur={handleBlur}
                  onChange={handleChange}
                  error={!!touched.commitments && !!errors.commitments}
                  helperText={touched.commitments && errors.commitments}
                  multiline
                  minRows={2}
                  maxRows={8}

                />

                {/* Technical Indicators */}
                <TextField
                  fullWidth
                  size="small"
                  variant="outlined"
                  label="Technical Indicators"
                  name="technicalIndicators"
                  value={values.technicalIndicators}
                  onBlur={handleBlur}
                  onChange={handleChange}
                  error={
                    !!touched.technicalIndicators && !!errors.technicalIndicators
                  }
                  helperText={
                    touched.technicalIndicators && errors.technicalIndicators
                  }
                  multiline
                  minRows={2}
                  maxRows={8}

                />

                {/* Description */}
                <TextField
                  fullWidth
                  variant="outlined"
                  multiline
                  minRows={2}
                  size="small"
                  label="Description"
                  name="description"
                  value={values.description}
                  onBlur={handleBlur}
                  onChange={handleChange}
                  error={!!touched.description && !!errors.description}
                  helperText={touched.description && errors.description}
                  sx={{ gridColumn: "span 3" }}
                />

                {/* Measurement Method */}
                <TextField
                  fullWidth
                  variant="outlined"
                  multiline
                  minRows={2}
                  size="small"
                  label="Measurement Method"
                  name="measurementMethod"
                  value={values.measurementMethod}
                  onBlur={handleBlur}
                  onChange={handleChange}
                  error={
                    !!touched.measurementMethod && !!errors.measurementMethod
                  }
                  helperText={touched.measurementMethod && errors.measurementMethod}
                  sx={{ gridColumn: "span 3" }}
                />

                {/* Emission Factor */}
                <TextField
                  fullWidth
                  size="small"
                  variant="outlined"
                  label="Emission Factor (kg/kWh)"
                  name="emissionFactorKgPerKwh"
                  type="number"
                  inputProps={{ step: "0.01", min: "0" }}
                  value={values.emissionFactorKgPerKwh}
                  onBlur={handleBlur}
                  onChange={(e) =>
                    setFieldValue(
                      "emissionFactorKgPerKwh",
                      e.target.value === "" ? "" : parseFloat(e.target.value)
                    )
                  }
                  error={
                    !!touched.emissionFactorKgPerKwh &&
                    !!errors.emissionFactorKgPerKwh
                  }
                  helperText={
                    touched.emissionFactorKgPerKwh &&
                    errors.emissionFactorKgPerKwh
                  }
                />

                {/* Start & End Date */}
                <TextField
                  fullWidth
                  size="small"
                  variant="outlined"
                  type="text"
                  label="Start Date (dd/mm/yyyy)"
                  name="startedDate"
                  placeholder="dd/mm/yyyy"
                  value={values.startedDate}
                  onBlur={handleBlur}
                  onChange={handleChange}
                  error={!!touched.startedDate && !!errors.startedDate}
                  helperText={touched.startedDate && errors.startedDate}
                />

                <TextField
                  fullWidth
                  size="small"
                  variant="outlined"
                  type="text"
                  label="End Date (dd/mm/yyyy)"
                  name="endDate"
                  placeholder="dd/mm/yyyy"
                  value={values.endDate}
                  onBlur={handleBlur}
                  onChange={handleChange}
                  error={!!touched.endDate && !!errors.endDate}
                  helperText={touched.endDate && errors.endDate}
                />


                {/* Logo Upload */}
                <Box>
                  <Typography fontSize="0.85rem" mb={0.5}>
                    Project Logo
                  </Typography>
                  <Button
                    size="small"
                    variant="outlined"
                    color="secondary"
                    onClick={() => document.getElementById("logo-upload").click()}
                  >
                    Upload
                  </Button>
                  <input
                    id="logo-upload"
                    type="file"
                    accept="image/*"
                    hidden
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) {
                        setFieldValue("logo", file);
                        setPreviewUrl(URL.createObjectURL(file));
                        setS3Url(null);
                      }
                    }}
                  />
                  {(previewUrl || s3Url) && (
                    <Box mt={1} textAlign="center">
                      <img
                        src={s3Url ? `${s3Url}?t=${Date.now()}` : previewUrl}
                        alt="Project Logo"
                        style={{
                          width: 60,
                          height: 60,
                          borderRadius: "50%",
                          objectFit: "cover",
                          border: "1px solid #ccc",
                        }}
                      />
                    </Box>
                  )}
                </Box>

                {/* Legal Docs */}
                <Box>
                  <Typography fontSize="0.85rem" mb={0.5}>
                    Legal Document
                  </Typography>
                  <Button
                    size="small"
                    variant="outlined"
                    color="secondary"
                    onClick={() => document.getElementById("legal-upload").click()}
                  >
                    Upload
                  </Button>
                  <input
                    id="legal-upload"
                    type="file"
                    accept=".pdf,.doc,.docx"
                    hidden
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) setFieldValue("legalDocsFile", file);
                    }}
                  />
                  {values.legalDocsFile && (
                    <Typography mt={0.5} fontSize="0.8rem" noWrap>
                      {values.legalDocsFile.name}
                    </Typography>
                  )}
                </Box>

                {/* Status */}
                <TextField
                  fullWidth
                  size="small"
                  variant="outlined"
                  label="Status"
                  name="status"
                  value="OPEN"
                  InputProps={{ readOnly: true }}
                />
              </Box>

              {/* Submit */}
              <Box display="flex" justifyContent="flex-end" mt={3}>
                <Button
                  type="submit"
                  color="secondary"
                  variant="contained"
                  disabled={loading}
                  startIcon={
                    loading ? <CircularProgress size={18} color="inherit" /> : null
                  }
                  sx={{ textTransform: "none", px: 3, fontWeight: 600 }}
                >
                  {loading ? "Creating..." : "Create Project"}
                </Button>
              </Box>
            </form>
          )}
        </Formik>
      </Paper>
      {SnackbarComponent}
    </Box>
  );
};
const dateRegex = /^(0[1-9]|[12][0-9]|3[01])[\/](0[1-9]|1[0-2])[\/]\d{4}$/;
const checkoutSchema = yup.object().shape({
  title: yup.string().required("Title is required"),
  description: yup.string().required("Description is required"),
  commitments: yup.string().required("Commitments are required"),
  technicalIndicators: yup.string().required("Technical indicators are required"),
  measurementMethod: yup.string().required("Measurement method is required"),
  emissionFactorKgPerKwh: yup
    .number()
    .typeError("Emission factor must be a number")
    .required("Emission factor is required"),

  // Bắt buộc nhập và đúng format dd/mm/yyyy
  startedDate: yup
    .string()
    .required("Start date is required")
    .matches(dateRegex, "Start date must be in dd/mm/yyyy format"),
  endDate: yup
    .string()
    .nullable()
    .notRequired()
    .test(
      "is-valid-date-format",
      "End date must be in dd/mm/yyyy format",
      (value) => !value || dateRegex.test(value)
    ),

});

const initialValues = {
  title: "",
  description: "",
  logo: null,
  commitments: "",
  technicalIndicators: "",
  measurementMethod: "",
  emissionFactorKgPerKwh: "",
  status: "OPEN",
  startedDate: "",
  endDate: "",
  legalDocsFile: null,
};

export default NewProjectForm;
