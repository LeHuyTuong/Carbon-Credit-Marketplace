import {
  Box,
  Button,
  TextField,
  MenuItem,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Typography,
  Stack,
} from "@mui/material";
import { Formik } from "formik";
import * as yup from "yup";
import useMediaQuery from "@mui/material/useMediaQuery";
import { useState, useEffect, useRef } from "react";
import Header from "@/components/Chart/Header.jsx";
import { registerUser, verifyOtp } from "@/apiAdmin/apiLogin.js";
import { useSnackbar } from "@/hooks/useSnackbar.jsx";

const RegisterForm = () => {
  const isNonMobile = useMediaQuery("(min-width:600px)");
  const { showSnackbar, SnackbarComponent } = useSnackbar();
  const [openOtpDialog, setOpenOtpDialog] = useState(false);
  const [otpValues, setOtpValues] = useState(["", "", "", "", "", ""]);
  const [timer, setTimer] = useState(300); // 5 phút = 300s
  const [userEmail, setUserEmail] = useState("");
  const otpRefs = useRef([]); //  refs cho auto focus

  // Timer countdown
  useEffect(() => {
    if (!openOtpDialog) return;
    if (timer <= 0) return;

    const interval = setInterval(() => {
      setTimer((prev) => prev - 1);
    }, 1000);

    return () => clearInterval(interval);
  }, [openOtpDialog, timer]);

  const handleOtpChange = (index, value) => {
    if (!/^[0-9]?$/.test(value)) return;
    const newOtp = [...otpValues];
    newOtp[index] = value;
    setOtpValues(newOtp);

    //  tự động chuyển sang ô tiếp theo
    if (value && index < 5) {
      otpRefs.current[index + 1].focus();
    }
    //  nếu xóa thì lùi lại
    if (!value && index > 0) {
      otpRefs.current[index - 1].focus();
    }
  };

  const handleFormSubmit = async (values, { resetForm }) => {
    try {
      const payload = {
        email: values.email,
        password: values.password,
        confirmPassword: values.confirmPassword,
        roleName: values.roleName,
      };

      const res = await registerUser(payload);
      console.log(" Register success:", res);

      setUserEmail(values.email);
      setOpenOtpDialog(true);
      setTimer(300); // reset timer 5 phút
      resetForm();
      showSnackbar("info", "Registration successful! Please verify OTP.");
    } catch (error) {
      console.error(" Register failed:", error);
      showSnackbar("error", "Failed to register account!");
    }
  };

  const handleOtpSubmit = async () => {
    const otpCode = otpValues.join("");
    if (otpCode.length !== 6) {
      showSnackbar("warning", "Please enter all 6 digits of the OTP!");
      return;
    }

    try {
      const res = await verifyOtp({
        email: userEmail,
        otpCode: otpCode,
      });

      console.log(" OTP verified:", res);
      if (res?.responseStatus?.responseCode === "200") {
        setOpenOtpDialog(false);
        showSnackbar("success", "Account verified successfully!");
      } else {
        showSnackbar("error", "Invalid OTP or verification failed!");
      }
    } catch (error) {
      console.error(" OTP verify failed:", error);
      showSnackbar("error", "Error verifying OTP!");
    }
  };

  const minutes = Math.floor(timer / 60);
  const seconds = timer % 60;

  return (
    <Box m="20px" sx={{ marginLeft: "290px" }}>
      <Header title="REGISTER USER" subtitle="Create a new account" />

      <Formik
        onSubmit={handleFormSubmit}
        initialValues={initialValues}
        validationSchema={registerSchema}
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
              gap="30px"
              gridTemplateColumns="repeat(4, minmax(0, 1fr))"
              sx={{
                "& > div": { gridColumn: isNonMobile ? undefined : "span 4" },
              }}
            >
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
                sx={{ gridColumn: "span 4" }}
              />

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

              <TextField
                fullWidth
                variant="filled"
                type="password"
                label="Confirm Password"
                onBlur={handleBlur}
                onChange={handleChange}
                value={values.confirmPassword}
                name="confirmPassword"
                error={!!touched.confirmPassword && !!errors.confirmPassword}
                helperText={touched.confirmPassword && errors.confirmPassword}
                sx={{ gridColumn: "span 2" }}
              />

              <TextField
                select
                fullWidth
                variant="filled"
                label="Role"
                onBlur={handleBlur}
                onChange={handleChange}
                value={values.roleName}
                name="roleName"
                error={!!touched.roleName && !!errors.roleName}
                helperText={touched.roleName && errors.roleName}
                sx={{ gridColumn: "span 4" }}
              >
                <MenuItem value="admin">Admin</MenuItem>
                <MenuItem value="cva">CVA</MenuItem>
              </TextField>
            </Box>

            <Box display="flex" justifyContent="end" mt="20px">
              <Button type="submit" color="secondary" variant="contained">
                Register
              </Button>
            </Box>
          </form>
        )}
      </Formik>

      {/* OTP Popup */}
      <Dialog open={openOtpDialog} onClose={() => setOpenOtpDialog(false)}>
        <DialogTitle textAlign="center" fontWeight="bold">
          Enter OTP
        </DialogTitle>
        <DialogContent>
          <Typography textAlign="center" sx={{ mb: 2 }}>
            Please enter the 6-digit OTP sent to your email.
          </Typography>

          <Stack direction="row" justifyContent="center" spacing={1}>
            {otpValues.map((val, index) => (
              <TextField
                key={index}
                inputRef={(el) => (otpRefs.current[index] = el)} //  gán ref
                value={val}
                onChange={(e) => handleOtpChange(index, e.target.value)}
                inputProps={{
                  maxLength: 1,
                  style: {
                    textAlign: "center",
                    fontSize: "22px",
                    width: "45px",
                    height: "45px",
                  },
                }}
              />
            ))}
          </Stack>

          <Typography textAlign="center" color="text.secondary" sx={{ mt: 2 }}>
            Time left: {minutes}:{seconds.toString().padStart(2, "0")}
          </Typography>
        </DialogContent>

        <DialogActions sx={{ justifyContent: "center", pb: 2 }}>
          <Button
            onClick={handleOtpSubmit}
            variant="contained"
            color="secondary"
          >
            Verify OTP
          </Button>
          <Button onClick={() => setOpenOtpDialog(false)} color="error">
            Cancel
          </Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar component */}
      {SnackbarComponent}
    </Box>
  );
};

// Validation schema
const registerSchema = yup.object().shape({
  email: yup.string().email("Invalid email").required("Email is required"),
  password: yup
    .string()
    .min(6, "Password must be at least 6 characters")
    .required("Required"),
  confirmPassword: yup
    .string()
    .oneOf([yup.ref("password"), null], "Passwords must match")
    .required("Confirm password is required"),
  roleName: yup
    .string()
    .oneOf(["admin", "cva"], "Invalid role")
    .required("Role is required"),
});

// Default values
const initialValues = {
  email: "",
  password: "",
  confirmPassword: "",
  roleName: "",
};

export default RegisterForm;
