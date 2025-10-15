import {
  Box,
  Button,
  TextField,
  Snackbar,
  Alert,
  Paper,
  CircularProgress,
} from "@mui/material";
import { Formik } from "formik";
import * as yup from "yup";
import useMediaQuery from "@mui/material/useMediaQuery";
import { useState } from "react";
import { Link } from "react-router-dom";
import Header from "@/components/Chart/Header.jsx";
import { createProject } from "@/apiAdmin/projectAdmin.js";

const NewProjectForm = () => {
  const isNonMobile = useMediaQuery("(min-width:800px)");
  const [openSnackbar, setOpenSnackbar] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState("");
  const [snackbarSeverity, setSnackbarSeverity] = useState("success");
  const [loading, setLoading] = useState(false);

  const handleFormSubmit = async (values, { resetForm }) => {
    const payload = {
      title: values.title,
      description: values.description,
      logo: values.logo,
      commitments: values.commitments,
      technicalIndicators: values.technicalIndicators,
      measurementMethod: values.measurementMethod,
      legalDocsUrl: values.legalDocsUrl,
    };

    console.log("📦 Payload to API:", payload);

    try {
      setLoading(true);
      const response = await createProject(payload);
      console.log("✅ API Response:", response);

      setSnackbarMessage("🎉 Project created successfully!");
      setSnackbarSeverity("success");
      setOpenSnackbar(true);
      resetForm();
    } catch (error) {
      console.error("❌ Error creating project:", error);
      setSnackbarMessage(error.message || "Failed to create project. Please try again.");
      setSnackbarSeverity("error");
      setOpenSnackbar(true);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box m="20px">
      <Box display="flex" justifyContent="space-between" alignItems="center">
        <Header title="CREATE PROJECT" subtitle="Create a new carbon project" />
        <Button
          component={Link}
          to="/admin/project_management"
          variant="outlined"
          color="secondary"
          sx={{
            height: "fit-content",
            textTransform: "none",
            fontWeight: 600,
          }}
        >
          ← Back to Project List
        </Button>
      </Box>

      <Paper
        elevation={3}
        sx={{
          p: 4,
          maxWidth: "1000px",
          mx: "auto",
          borderRadius: 3,
          backgroundColor: "background.paper",
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
          }) => (
            <form onSubmit={handleSubmit}>
              <Box
                display="grid"
                gridTemplateColumns={isNonMobile ? "repeat(2, 1fr)" : "repeat(1, 1fr)"}
                gap="30px"
              >
                <TextField
                  fullWidth
                  variant="filled"
                  label="Title"
                  name="title"
                  value={values.title}
                  onBlur={handleBlur}
                  onChange={handleChange}
                  error={!!touched.title && !!errors.title}
                  helperText={touched.title && errors.title}
                />
                <TextField
                  fullWidth
                  variant="filled"
                  label="Logo URL"
                  name="logo"
                  value={values.logo}
                  onBlur={handleBlur}
                  onChange={handleChange}
                  error={!!touched.logo && !!errors.logo}
                  helperText={touched.logo && errors.logo}
                />
                <TextField
                  fullWidth
                  variant="filled"
                  multiline
                  minRows={3}
                  label="Description"
                  name="description"
                  value={values.description}
                  onBlur={handleBlur}
                  onChange={handleChange}
                  error={!!touched.description && !!errors.description}
                  helperText={touched.description && errors.description}
                  sx={{ gridColumn: "span 2" }}
                />
                <TextField
                  fullWidth
                  variant="filled"
                  label="Commitments"
                  name="commitments"
                  value={values.commitments}
                  onBlur={handleBlur}
                  onChange={handleChange}
                  error={!!touched.commitments && !!errors.commitments}
                  helperText={touched.commitments && errors.commitments}
                />
                <TextField
                  fullWidth
                  variant="filled"
                  label="Technical Indicators"
                  name="technicalIndicators"
                  value={values.technicalIndicators}
                  onBlur={handleBlur}
                  onChange={handleChange}
                  error={!!touched.technicalIndicators && !!errors.technicalIndicators}
                  helperText={touched.technicalIndicators && errors.technicalIndicators}
                />
                <TextField
                  fullWidth
                  variant="filled"
                  label="Measurement Method"
                  name="measurementMethod"
                  value={values.measurementMethod}
                  onBlur={handleBlur}
                  onChange={handleChange}
                  error={!!touched.measurementMethod && !!errors.measurementMethod}
                  helperText={touched.measurementMethod && errors.measurementMethod}
                />
                <TextField
                  fullWidth
                  variant="filled"
                  label="Legal Docs URL"
                  name="legalDocsUrl"
                  value={values.legalDocsUrl}
                  onBlur={handleBlur}
                  onChange={handleChange}
                  error={!!touched.legalDocsUrl && !!errors.legalDocsUrl}
                  helperText={touched.legalDocsUrl && errors.legalDocsUrl}
                />
              </Box>

              <Box display="flex" justifyContent="flex-end" mt="30px">
                <Button
                  type="submit"
                  color="secondary"
                  variant="contained"
                  disabled={loading}
                  startIcon={loading && <CircularProgress size={20} color="inherit" />}
                >
                  {loading ? "Creating..." : "Create Project"}
                </Button>
              </Box>
            </form>
          )}
        </Formik>
      </Paper>

      <Snackbar
        open={openSnackbar}
        autoHideDuration={4000}
        onClose={() => setOpenSnackbar(false)}
        anchorOrigin={{ vertical: "top", horizontal: "center" }}
      >
        <Alert
          onClose={() => setOpenSnackbar(false)}
          severity={snackbarSeverity}
          variant="filled"
          sx={{ width: "100%" }}
        >
          {snackbarMessage}
        </Alert>
      </Snackbar>
    </Box>
  );
};

const checkoutSchema = yup.object().shape({
  title: yup.string().required("Title is required"),
  description: yup.string().required("Description is required"),
  logo: yup.string().url("Invalid URL").required("Logo URL is required"),
  commitments: yup.string().required("Commitments are required"),
  technicalIndicators: yup.string().required("Technical indicators are required"),
  measurementMethod: yup.string().required("Measurement method is required"),
  legalDocsUrl: yup.string().url("Invalid URL").required("Legal docs URL is required"),
});

const initialValues = {
  title: "",
  description: "",
  logo: "",
  commitments: "",
  technicalIndicators: "",
  measurementMethod: "",
  legalDocsUrl: "",
};

export default NewProjectForm;
