import React, { useState, useRef } from "react";
import {
    Box,
    Button,
    TextField,
    Typography,
    useTheme,
    Paper,
    CircularProgress,
} from "@mui/material";
import { tokens } from "@/theme";
import SupervisorAccount from "@mui/icons-material/SupervisorAccount";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import { apiRegister, verifyOTP } from "@/apiCVA/apiAuthor.js";
import { useAuth } from "@/context/AuthContext.jsx";
import { useNavigate } from "react-router-dom"; // ✅ thêm để điều hướng

const CVARegisterWithOTP = () => {
    const theme = useTheme();
    const colors = tokens(theme.palette.mode);
    const { login } = useAuth();
    const navigate = useNavigate(); // ✅ khởi tạo hook điều hướng

    const [form, setForm] = useState({ email: "", password: "", rePassword: "" });
    const [otp, setOtp] = useState(["", "", "", "", "", ""]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [step, setStep] = useState("register"); // "register" | "otp"

    const inputRefs = useRef([]);

    // Cập nhật form input
    const handleChange = (e) =>
        setForm({ ...form, [e.target.name]: e.target.value });

    // Cập nhật OTP input
    const handleOtpChange = (index, value) => {
        if (!/^\d*$/.test(value)) return;
        const newOtp = [...otp];
        newOtp[index] = value;
        setOtp(newOtp);

        if (value && index < 5) inputRefs.current[index + 1]?.focus();
        if (!value && index > 0) inputRefs.current[index - 1]?.focus();
    };

    // Submit đăng ký tài khoản
    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");

        if (!form.email || !form.password || !form.rePassword) {
            return setError("Please fill all fields");
        }
        if (form.password !== form.rePassword) {
            return setError("Passwords do not match");
        }

        try {
            setLoading(true);
            const res = await apiRegister(
                form.email,
                form.password,
                form.rePassword,
                "CVA"
            );
            console.log("Register success:", res);
            setStep("otp");
        } catch (err) {
            console.error("Register API Error:", err);
            setError(
                err?.responseStatus?.responseMessage ||
                err.message ||
                "Register failed"
            );
        } finally {
            setLoading(false);
        }
    };

    // Xác thực OTP
    const handleVerifyOTP = async () => {
        setError("");
        const otpStr = otp.join("");

        if (!form.email) return setError("Email is missing for OTP verification");
        if (otpStr.length !== 6) return setError("Enter complete 6-digit OTP");

        try {
            setLoading(true);
            console.log("Sending OTP verify request:", {
                email: form.email,
                otpCode: otpStr,
            });

            // ✅ gọi đúng API verify OTP
            const res = await verifyOTP({ email: form.email, otpCode: otpStr });

            if (!res?.jwt) throw new Error("Invalid OTP response");

            // ✅ Đăng nhập tạm hoặc lưu session
            login({ ...res.user, role: "CVA" }, res.jwt, true);

            // ✅ Điều hướng sang trang login
            navigate("/cva/carbonX/mkp/login");

            // Nếu muốn reload trang cứng, dùng dòng này thay vì navigate():
            // window.location.href = "/cva/carbonX/mkp/login";
        } catch (err) {
            console.error("Verify OTP API Error:", err);
            const apiMessage =
                err?.responseStatus?.responseMessage ||
                err?.message ||
                "OTP verification failed";
            setError(apiMessage);
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box
            height="100vh"
            display="flex"
            justifyContent="center"
            alignItems="center"
            sx={{
                background: `linear-gradient(135deg, ${colors.primary[400]}, ${colors.greenAccent[700]})`,
            }}
        >
            <Paper
                elevation={8}
                sx={{
                    p: 5,
                    width: "380px",
                    borderRadius: "18px",
                    textAlign: "center",
                    backgroundColor: colors.primary[500],
                }}
            >
                {/* STEP 1️⃣: Đăng ký tài khoản */}
                {step === "register" && (
                    <>
                        <Box
                            display="flex"
                            flexDirection="column"
                            alignItems="center"
                            mb={3}
                        >
                            <SupervisorAccount
                                sx={{ fontSize: 48, color: colors.greenAccent[500], mb: 1 }}
                            />
                            <Typography
                                variant="h4"
                                fontWeight="bold"
                                color={colors.greenAccent[400]}
                            >
                                CVA Register
                            </Typography>
                            <Typography variant="body2" color={colors.grey[300]}>
                                Create your CVA account
                            </Typography>
                        </Box>

                        <form onSubmit={handleSubmit}>
                            <TextField
                                label="Email"
                                name="email"
                                type="email"
                                value={form.email}
                                onChange={handleChange}
                                fullWidth
                                variant="filled"
                                sx={{
                                    mb: 2,
                                    backgroundColor: colors.primary[400],
                                    borderRadius: "6px",
                                }}
                            />
                            <TextField
                                label="Password"
                                name="password"
                                type="password"
                                value={form.password}
                                onChange={handleChange}
                                fullWidth
                                variant="filled"
                                sx={{
                                    mb: 2,
                                    backgroundColor: colors.primary[400],
                                    borderRadius: "6px",
                                }}
                            />
                            <TextField
                                label="Re-enter Password"
                                name="rePassword"
                                type="password"
                                value={form.rePassword}
                                onChange={handleChange}
                                fullWidth
                                variant="filled"
                                sx={{
                                    mb: 2,
                                    backgroundColor: colors.primary[400],
                                    borderRadius: "6px",
                                }}
                            />
                            {error && (
                                <Typography
                                    color="error"
                                    variant="body2"
                                    sx={{ mb: 2, fontWeight: "bold" }}
                                >
                                    {error}
                                </Typography>
                            )}
                            <Button
                                type="submit"
                                fullWidth
                                variant="contained"
                                disabled={loading}
                                sx={{
                                    mt: 1,
                                    py: 1.2,
                                    backgroundColor: colors.greenAccent[600],
                                    color: colors.grey[900],
                                    fontWeight: "bold",
                                    "&:hover": { backgroundColor: colors.greenAccent[700] },
                                }}
                            >
                                {loading ? (
                                    <CircularProgress size={24} color="inherit" />
                                ) : (
                                    "Sign Up"
                                )}
                            </Button>
                        </form>
                    </>
                )}

                {/* STEP 2️⃣: Nhập OTP */}
                {step === "otp" && (
                    <>
                        <Box
                            display="flex"
                            flexDirection="column"
                            alignItems="center"
                            mb={3}
                        >
                            <LockOutlinedIcon
                                sx={{ fontSize: 48, color: colors.greenAccent[500], mb: 1 }}
                            />
                            <Typography
                                variant="h4"
                                fontWeight="bold"
                                color={colors.greenAccent[400]}
                            >
                                Enter OTP
                            </Typography>
                            <Typography variant="body2" color={colors.grey[300]}>
                                Enter your 6-digit OTP
                            </Typography>
                        </Box>

                        <Box display="flex" justifyContent="space-between" mb={2}>
                            {otp.map((digit, idx) => (
                                <TextField
                                    key={idx}
                                    inputRef={(el) => (inputRefs.current[idx] = el)}
                                    value={digit}
                                    onChange={(e) => handleOtpChange(idx, e.target.value)}
                                    inputProps={{ maxLength: 1, style: { textAlign: "center" } }}
                                    sx={{
                                        width: "45px",
                                        backgroundColor: colors.primary[400],
                                        borderRadius: "6px",
                                    }}
                                    variant="filled"
                                />
                            ))}
                        </Box>

                        {error && (
                            <Typography
                                color="error"
                                variant="body2"
                                sx={{ mb: 2, fontWeight: "bold" }}
                            >
                                {error}
                            </Typography>
                        )}

                        <Button
                            fullWidth
                            variant="contained"
                            onClick={handleVerifyOTP}
                            disabled={loading}
                            sx={{
                                mt: 1,
                                py: 1.2,
                                backgroundColor: colors.greenAccent[600],
                                color: colors.grey[900],
                                fontWeight: "bold",
                                "&:hover": { backgroundColor: colors.greenAccent[700] },
                            }}
                        >
                            {loading ? (
                                <CircularProgress size={24} color="inherit" />
                            ) : (
                                "Verify OTP"
                            )}
                        </Button>
                    </>
                )}
            </Paper>
        </Box>
    );
};

export default CVARegisterWithOTP;