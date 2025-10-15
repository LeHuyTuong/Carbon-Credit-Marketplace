import { Box, Button, TextField, MenuItem, Snackbar, Alert, Paper } from "@mui/material";
import { Formik } from "formik";
import * as yup from "yup";
import useMediaQuery from "@mui/material/useMediaQuery";
import { useState } from "react";
import { Link } from "react-router-dom";
import Header from "@/components/Chart/Header.jsx";

const NewProjectForm = () => {
  const isNonMobile = useMediaQuery("(min-width:800px)");
  const [openSnackbar, setOpenSnackbar] = useState(false);

  const handleFormSubmit = (values, { resetForm }) => {
    console.log("Form values:", values);
    // Gửi API ở đây nếu cần
    setOpenSnackbar(true);
    resetForm();
  };

  return (
    <Box m="20px">
      {/* Header + nút Back */}
      <Box display="flex" justifyContent="space-between" alignItems="center">
        <Header title="CREATE PROJECT" subtitle="Create a new carbon project" />
        <Button
          component={Link}
          to="/admin/project_management"
          variant="outlined"
          color="secondary"
          sx={{
            height: "fit-content",
            alignSelf: "flex-start",
            textTransform: "none",
            fontWeight: 600,
          }}
        >
          ← Back to Project List
        </Button>
      </Box>

      {/* Form Container */}
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
                {/* Title */}
                <TextField
                  fullWidth
                  variant="filled"
                  type="text"
                  label="Title"
                  onBlur={handleBlur}
                  onChange={handleChange}
                  value={values.title}
                  name="title"
                  error={!!touched.title && !!errors.title}
                  helperText={touched.title && errors.title}
                />

                {/* Logo */}
                <TextField
                  fullWidth
                  variant="filled"
                  type="text"
                  label="Logo URL"
                  onBlur={handleBlur}
                  onChange={handleChange}
                  value={values.logo}
                  name="logo"
                  error={!!touched.logo && !!errors.logo}
                  helperText={touched.logo && errors.logo}
                />

                {/* Description */}
                <TextField
                  fullWidth
                  variant="filled"
                  multiline
                  minRows={3}
                  label="Description"
                  onBlur={handleBlur}
                  onChange={handleChange}
                  value={values.description}
                  name="description"
                  error={!!touched.description && !!errors.description}
                  helperText={touched.description && errors.description}
                  sx={{ gridColumn: "span 2" }}
                />

                {/* Commitments */}
                <TextField
                  fullWidth
                  variant="filled"
                  type="text"
                  label="Commitments"
                  onBlur={handleBlur}
                  onChange={handleChange}
                  value={values.commitments}
                  name="commitments"
                  error={!!touched.commitments && !!errors.commitments}
                  helperText={touched.commitments && errors.commitments}
                />

                {/* Technical Indicator */}
                <TextField
                  fullWidth
                  variant="filled"
                  type="text"
                  label="Technical Indicator"
                  onBlur={handleBlur}
                  onChange={handleChange}
                  value={values.technicalIndicator}
                  name="technicalIndicator"
                  error={!!touched.technicalIndicator && !!errors.technicalIndicator}
                  helperText={touched.technicalIndicator && errors.technicalIndicator}
                />

                {/* Measurement Method */}
                <TextField
                  fullWidth
                  variant="filled"
                  type="text"
                  label="Measurement Method"
                  onBlur={handleBlur}
                  onChange={handleChange}
                  value={values.measurementMethod}
                  name="measurementMethod"
                  error={!!touched.measurementMethod && !!errors.measurementMethod}
                  helperText={touched.measurementMethod && errors.measurementMethod}
                />

                {/* Status */}
                <TextField
                  select
                  fullWidth
                  variant="filled"
                  label="Status"
                  onBlur={handleBlur}
                  onChange={handleChange}
                  value={values.status}
                  name="status"
                  error={!!touched.status && !!errors.status}
                  helperText={touched.status && errors.status}
                >
                  <MenuItem value="Is_Open">Is Open</MenuItem>
                  <MenuItem value="Coming_Soon">Coming Soon</MenuItem>
                  <MenuItem value="Ended">Ended</MenuItem>
                </TextField>
              </Box>

              {/* Submit Button */}
              <Box display="flex" justifyContent="flex-end" mt="30px">
                <Button type="submit" color="secondary" variant="contained">
                  Create Project
                </Button>
              </Box>
            </form>
          )}
        </Formik>
      </Paper>

      {/* Snackbar Success */}
      <Snackbar
        open={openSnackbar}
        autoHideDuration={3000}
        onClose={() => setOpenSnackbar(false)}
        anchorOrigin={{ vertical: "top", horizontal: "center" }}
      >
        <Alert
          onClose={() => setOpenSnackbar(false)}
          severity="success"
          variant="filled"
          sx={{ width: "100%" }}
        >
          🎉 Project created successfully!
        </Alert>
      </Snackbar>
    </Box>
  );
};

// Validation schema
const checkoutSchema = yup.object().shape({
  title: yup.string().required("Title is required"),
  description: yup.string().required("Description is required"),
  logo: yup.string().url("Invalid URL").required("Logo URL is required"),
  commitments: yup.string().required("Commitments are required"),
  technicalIndicator: yup.string().required("Technical indicator is required"),
  measurementMethod: yup.string().required("Measurement method is required"),
  status: yup
    .string()
    .oneOf(["Is_Open", "Coming_Soon", "Ended"], "Invalid status")
    .required("Status is required"),
});

const initialValues = {
  title: "",
  description: "",
  logo: "",
  commitments: "",
  technicalIndicator: "",
  measurementMethod: "",
  status: "",
};

export default NewProjectForm;
