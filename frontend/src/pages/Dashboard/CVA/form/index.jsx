import { Box, Button, TextField, MenuItem, Snackbar, Alert } from "@mui/material";
import { Formik } from "formik";
import * as yup from "yup";
import useMediaQuery from "@mui/material/useMediaQuery";
import { useState } from "react";
import Header from "@/components/Chart/Header.jsx";

const Form = () => {
  const isNonMobile = useMediaQuery("(min-width:600px)");
  const [openSnackbar, setOpenSnackbar] = useState(false);

  const handleFormSubmit = (values, { resetForm }) => {
    const formData = new FormData();
    Object.keys(values).forEach((key) => {
      formData.append(key, values[key]);
    });

    console.log("Uploaded avatar:", values.avatar);
    console.log("All form values:", values);

    // Gửi formData qua API ở đây nếu cần
    // axios.post("/api/users", formData, { headers: { "Content-Type": "multipart/form-data" } });

    setOpenSnackbar(true);
    resetForm();
  };

  return (
    <Box m="20px">
      <Header title="CREATE USER" subtitle="Create a New User Profile" />

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
              gap="30px"
              gridTemplateColumns="repeat(4, minmax(0, 1fr))"
              sx={{
                "& > div": { gridColumn: isNonMobile ? undefined : "span 4" },
              }}
            >
              {/* Upload Avatar */}
              <Box
                gridColumn="span 4"
                display="flex"
                flexDirection="column"
                alignItems="center"
                gap="10px"
              >
                <input
                  id="avatar"
                  name="avatar"
                  type="file"
                  accept="image/*"
                  style={{ display: "none" }}
                  onChange={(event) => {
                    const file = event.currentTarget.files[0];
                    setFieldValue("avatar", file);
                  }}
                />
                <label htmlFor="avatar">
                  <Button variant="outlined" component="span" color="secondary">
                    Upload Avatar
                  </Button>
                </label>

                {values.avatar && (
                  <Box mt={2}>
                    <img
                      src={URL.createObjectURL(values.avatar)}
                      alt="Avatar Preview"
                      width="120"
                      height="120"
                      style={{
                        borderRadius: "50%",
                        objectFit: "cover",
                        border: "2px solid #888",
                      }}
                    />
                  </Box>
                )}
                {touched.avatar && errors.avatar && (
                  <div style={{ color: "red", fontSize: "14px" }}>{errors.avatar}</div>
                )}
              </Box>

              {/* Name */}
              <TextField
                fullWidth
                variant="filled"
                type="text"
                label="Full Name"
                onBlur={handleBlur}
                onChange={handleChange}
                value={values.name}
                name="name"
                error={!!touched.name && !!errors.name}
                helperText={touched.name && errors.name}
                sx={{ gridColumn: "span 4" }}
              />

              {/* Email */}
              <TextField
                fullWidth
                variant="filled"
                type="email"
                label="Email"
                onBlur={handleBlur}
                onChange={handleChange}
                value={values.email}
                name="email"
                error={!!touched.email && !!errors.email}
                helperText={touched.email && errors.email}
                sx={{ gridColumn: "span 2" }}
              />

              {/* Password */}
              <TextField
                fullWidth
                variant="filled"
                type="password"
                label="Password"
                onBlur={handleBlur}
                onChange={handleChange}
                value={values.password}
                name="password"
                error={!!touched.password && !!errors.password}
                helperText={touched.password && errors.password}
                sx={{ gridColumn: "span 2" }}
              />

              {/* Phone */}
              <TextField
                fullWidth
                variant="filled"
                type="text"
                label="Phone"
                onBlur={handleBlur}
                onChange={handleChange}
                value={values.phone}
                name="phone"
                error={!!touched.phone && !!errors.phone}
                helperText={touched.phone && errors.phone}
                sx={{ gridColumn: "span 2" }}
              />

              {/* Role */}
              <TextField
                select
                fullWidth
                variant="filled"
                label="Role"
                onBlur={handleBlur}
                onChange={handleChange}
                value={values.role}
                name="role"
                error={!!touched.role && !!errors.role}
                helperText={touched.role && errors.role}
                sx={{ gridColumn: "span 1" }}
              >
                <MenuItem value="admin">Admin</MenuItem>
                <MenuItem value="cva">CVA</MenuItem>
              </TextField>

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
                sx={{ gridColumn: "span 1" }}
              >
                <MenuItem value="active">Active</MenuItem>
                <MenuItem value="inactive">Inactive</MenuItem>
              </TextField>
            </Box>

            {/* Submit Button */}
            <Box display="flex" justifyContent="end" mt="20px">
              <Button type="submit" color="secondary" variant="contained">
                Create New User
              </Button>
            </Box>
          </form>
        )}
      </Formik>

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
          🎉 Tạo tài khoản thành công!
        </Alert>
      </Snackbar>
    </Box>
  );
};

// Validation schema
const phoneRegExp =
  /^((\+[1-9]{1,4}[ -]?)|(\([0-9]{2,3}\)[ -]?)|([0-9]{2,4})[ -]?)*?[0-9]{3,4}[ -]?[0-9]{3,4}$/;

const checkoutSchema = yup.object().shape({
  name: yup.string().required("Name is required"),
  email: yup.string().email("Invalid email").required("Email is required"),
  password: yup.string().min(6, "Password must be at least 6 characters").required("Required"),
  phone: yup
    .string()
    .matches(phoneRegExp, "Phone number is not valid")
    .required("Phone is required"),
  role: yup.string().oneOf(["admin", "cva"], "Invalid role").required("Role is required"),
  status: yup.string().oneOf(["active", "inactive"], "Invalid status").required("Status is required"),
  avatar: yup.mixed().required("Avatar is required"),
});

// Default values
const initialValues = {
  name: "",
  email: "",
  password: "",
  phone: "",
  role: "",
  status: "",
  avatar: null,
};

export default Form;
