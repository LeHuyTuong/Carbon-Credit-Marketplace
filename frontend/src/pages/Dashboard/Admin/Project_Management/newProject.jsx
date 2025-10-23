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
  try {
    setLoading(true);

    //  Tạo FormData
    const formDataToSend = new FormData();
    formDataToSend.append("requestTrace", `trace_${Date.now()}`);
    formDataToSend.append("requestDateTime", new Date().toISOString());
    formDataToSend.append("title", values.title);
    formDataToSend.append("description", values.description);
    formDataToSend.append("commitments", values.commitments);
    formDataToSend.append("technicalIndicators", values.technicalIndicators);
    formDataToSend.append("measurementMethod", values.measurementMethod);

    //  logo & legal docs (nếu có file)
    if (values.logo) formDataToSend.append("logo", values.logo);
    if (values.legalDocsUrl)
      formDataToSend.append("legalDocsUrl", values.legalDocsUrl);

    //  Gửi form data
    const response = await createProject(formDataToSend);
    console.log("Create Project Response:", response);

    if (response?.responseStatus?.responseCode === "00000000") {
      setSnackbarMessage("Project created successfully!");
      setSnackbarSeverity("success");
      setOpenSnackbar(true);
      resetForm();
    } else {
      throw new Error(
        response?.responseStatus?.responseMessage || "Failed to create project"
      );
    }
  } catch (error) {
    console.error("Error creating project:", error);
    setSnackbarMessage(
      error.message || "Failed to create project. Please try again."
    );
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
          sx={{ height: "fit-content", textTransform: "none", fontWeight: 600 }}
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
            setFieldValue,
          }) => (
            <form onSubmit={handleSubmit}>
              <Box
                display="grid"
                gridTemplateColumns={
                  isNonMobile ? "repeat(2, 1fr)" : "repeat(1, 1fr)"
                }
                gap="30px"
              >
                {/* TITLE */}
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

                {/* UPLOAD LOGO */}
                <Box>
                  <TextField
                    fullWidth
                    variant="filled"
                    label="Logo File"
                    name="logo"
                    value={values.logo ? values.logo.name : ""}
                    InputProps={{ readOnly: true }}
                    sx={{
                      "& .MuiFilledInput-root": {
                        backgroundColor: (theme) =>
                          theme.palette.mode === "dark"
                            ? "rgba(255,255,255,0.08)"
                            : "#f9f9f9",
                        borderRadius: 2,
                        "&:hover": {
                          backgroundColor: (theme) =>
                            theme.palette.mode === "dark"
                              ? "rgba(255,255,255,0.12)"
                              : "#fff",
                        },
                      },
                    }}
                    onClick={() => document.getElementById("logo-upload").click()}
                  />
                  <input
                    id="logo-upload"
                    name="logo"
                    type="file"
                    accept="image/*"
                    style={{ display: "none" }}
                    onChange={(e) => {
                      const file = e.target.files[0];
                      if (file) setFieldValue("logo", file);
                    }}
                  />
                  <Button
                    variant="outlined"
                    color="secondary"
                    onClick={() => document.getElementById("logo-upload").click()}
                    sx={{
                      mt: 1,
                      borderRadius: 2,
                      textTransform: "none",
                      fontWeight: 600,
                    }}
                  >
                    Upload Logo
                  </Button>
                </Box>

                {/* DESCRIPTION */}
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

                {/* COMMITMENTS */}
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

                {/* TECHNICAL INDICATORS */}
                <TextField
                  fullWidth
                  variant="filled"
                  label="Technical Indicators"
                  name="technicalIndicators"
                  value={values.technicalIndicators}
                  onBlur={handleBlur}
                  onChange={handleChange}
                  error={
                    !!touched.technicalIndicators &&
                    !!errors.technicalIndicators
                  }
                  helperText={
                    touched.technicalIndicators && errors.technicalIndicators
                  }
                />

                {/* MEASUREMENT METHOD */}
                <TextField
                  fullWidth
                  variant="filled"
                  label="Measurement Method"
                  name="measurementMethod"
                  value={values.measurementMethod}
                  onBlur={handleBlur}
                  onChange={handleChange}
                  error={
                    !!touched.measurementMethod && !!errors.measurementMethod
                  }
                  helperText={
                    touched.measurementMethod && errors.measurementMethod
                  }
                />

                {/* UPLOAD LEGAL DOC */}
                <Box>
                  <TextField
                    fullWidth
                    variant="filled"
                    label="Legal Document"
                    name="legalDocsUrl"
                    value={values.legalDocsUrl ? values.legalDocsUrl.name : ""}
                    InputProps={{ readOnly: true }}
                    sx={{
                      "& .MuiFilledInput-root": {
                        backgroundColor: (theme) =>
                          theme.palette.mode === "dark"
                            ? "rgba(255,255,255,0.08)"
                            : "#f9f9f9",
                        borderRadius: 2,
                        "&:hover": {
                          backgroundColor: (theme) =>
                            theme.palette.mode === "dark"
                              ? "rgba(255,255,255,0.12)"
                              : "#fff",
                        },
                      },
                    }}
                    onClick={() =>
                      document.getElementById("legal-upload").click()
                    }
                  />
                  <input
                    id="legal-upload"
                    name="legalDocsUrl"
                    type="file"
                    accept=".pdf,.doc,.docx"
                    style={{ display: "none" }}
                    onChange={(e) => {
                      const file = e.target.files[0];
                      if (file) setFieldValue("legalDocsUrl", file);
                    }}
                  />
                  <Button
                    variant="outlined"
                    color="secondary"
                    onClick={() =>
                      document.getElementById("legal-upload").click()
                    }
                    sx={{
                      mt: 1,
                      borderRadius: 2,
                      textTransform: "none",
                      fontWeight: 600,
                    }}
                    
                  >
                    Upload Legal Document
                  </Button>
                </Box>
              </Box>

              <Box display="flex" justifyContent="flex-end" mt="30px">
                <Button
                  type="submit"
                  color="secondary"
                  variant="contained"
                  disabled={loading}
                  startIcon={
                    loading && <CircularProgress size={20} color="inherit" />
                  }
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
  commitments: yup.string().required("Commitments are required"),
  technicalIndicators: yup
    .string()
    .required("Technical indicators are required"),
  measurementMethod: yup.string().required("Measurement method is required"),
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
