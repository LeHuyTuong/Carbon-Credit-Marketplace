import React, { useEffect, useState } from "react";
import {
    Box,
    Typography,
    CircularProgress,
    Paper,
    Button,
    Snackbar,
    Alert,
    Grid,
    useTheme,
    TextField
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import Header from "@/components/Chart/Header";
import { tokens } from "@/theme";
import { getReportByIdAdmin } from "@/apiAdmin/reportAdmin.js";
import { getCompanyKYCProfile } from "@/apiAdmin/companyAdmin.js";

const ReportView = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const theme = useTheme();
    const colors = tokens(theme.palette.mode);
    const [report, setReport] = useState(null);
    const [loading, setLoading] = useState(true);
    const [snackbar, setSnackbar] = useState({
        open: false,
        message: "",
        severity: "success",
    });
    const [kyc, setKyc] = useState(null);

    useEffect(() => {
        const fetchDetail = async () => {
            try {
                console.log("Fetching report ID:", id);
                const res = await getReportByIdAdmin(id);
                const data = res?.responseData || res;
                console.log("Raw report API response:", data);

                if (data && data.id) setReport(data);
                else throw new Error("No valid data received");
            } catch (error) {
                console.error("Error fetching report detail:", error);
                setSnackbar({
                    open: true,
                    message: "Failed to fetch report.",
                    severity: "error",
                });
            } finally {
                setLoading(false);
            }
        };
        fetchDetail();
    }, [id]);

    // Load KYC Info của công ty
    useEffect(() => {
        if (report?.sellerId) {
            (async () => {
                try {
                    const res = await getCompanyKYCProfile(report.sellerId);
                    console.log("Raw KYC API response:", res);

                    // thử lấy đúng field (responseData hoặc response)
                    const data =
                        res?.responseData || res?.response || res?.data || res;

                    console.log("Parsed KYC data:", data);
                    if (data && Object.keys(data).length > 0) {
                        setKyc(data);
                    } else {
                        console.warn("Empty KYC data received");
                    }
                } catch (error) {
                    console.error("Failed to fetch KYC info:", error);
                    setSnackbar({
                        open: true,
                        message: "Failed to fetch company KYC profile.",
                        severity: "error",
                    });
                }
            })();
        }
    }, [report?.sellerId]);

    const formatVNDate = (dateString) => {
        if (!dateString) return "N/A";

        const d = new Date(dateString);

        // Chuyển UTC → VN (UTC+7)
        const vnTime = new Date(d.getTime() + 7 * 60 * 60 * 1000);

        const day = String(vnTime.getDate()).padStart(2, "0");
        const month = String(vnTime.getMonth() + 1).padStart(2, "0");
        const year = vnTime.getFullYear();

        const time = vnTime.toLocaleTimeString("vi-VN", {
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit",
        });

        return `${day}/${month}/${year}, ${time}`;
    };



    if (loading)
        return (
            <Box
                display="flex"
                justifyContent="center"
                alignItems="center"
                height="70vh"
                sx={{ marginLeft: "290px" }}
            >
                <CircularProgress />
            </Box>
        );

    if (!report)
        return (
            <Box textAlign="center" sx={{ marginLeft: "290px" }} mt={5}>
                <Typography variant="h6" color="error">
                    Report not found.
                </Typography>
                <Button
                    onClick={() => navigate("/admin/report_management")}
                    variant="contained"
                    sx={{ mt: 2 }}
                >
                    Back
                </Button>
            </Box>
        );

    return (
        <Box
            m="20px"
            sx={{ marginLeft: "290px", maxWidth: "1140px", width: "100%" }}
        >
            <Header
                title="REPORT DETAIL"
                subtitle={`Detailed information of report ID: ${report.id}`}
            />

            <Paper
                elevation={3}
                sx={{
                    p: 3,
                    mt: 3,
                    backgroundColor: colors.primary[400],
                }}
            >
                <Grid container spacing={13}>
                    {/* COLUMN 1: General Info */}
                    <Grid item xs={12} md={4}>
                        <Typography variant="h5" fontWeight="700" color="secondary" gutterBottom>
                            General Information
                        </Typography>

                        <Typography variant="h6" fontWeight="600" gutterBottom>
                            Project Name:
                        </Typography>
                        <TextField
                            value={report.projectName || "N/A"}
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
                                mb: 2,
                                backgroundColor: "rgba(255,255,255,0.08)",
                                borderRadius: "8px",
                                "& .MuiInputBase-input.Mui-disabled": {
                                    WebkitTextFillColor: "#ccc", // màu chữ nếu theme tối
                                },
                            }}
                        />

                        <Typography variant="h6" fontWeight="600" gutterBottom>
                            Seller Company Name:
                        </Typography>
                        <Typography mb={2}>{report.sellerName || "N/A"}</Typography>

                        <Typography variant="h6" fontWeight="600" gutterBottom>
                            Period:
                        </Typography>
                        <Typography mb={2}>{report.period || "N/A"}</Typography>

                        <Typography variant="h6" fontWeight="600" gutterBottom>
                            Status:
                        </Typography>
                        <Typography mb={2}>{report.status || "N/A"}</Typography>

                        <Typography variant="h6" fontWeight="600" gutterBottom>
                            Source:
                        </Typography>
                        <Typography mb={2}>{report.source || "N/A"}</Typography>

                        {/* Document Section */}
                        <Box mt={2}>
                            <Typography variant="h6" fontWeight="600" gutterBottom>
                                Uploaded File:
                            </Typography>
                            {report.uploadStorageUrl ? (
                                <Button
                                    variant="contained"
                                    color="info"
                                    size="small"
                                    onClick={() => window.open(report.uploadStorageUrl, "_blank")}
                                >
                                    View CSV File
                                </Button>
                            ) : (
                                <Typography>No file uploaded</Typography>
                            )}
                        </Box>
                    </Grid>

                    {/* COLUMN 2: Company KYC Info */}
                    {kyc && (
                        <Grid item xs={12} md={4}>
                            <Typography variant="h5" fontWeight="700" color="secondary" gutterBottom>
                                Company KYC
                            </Typography>

                            <Typography variant="h6" fontWeight="600" gutterBottom>
                                Company Name:
                            </Typography>
                            <Typography mb={2}>{kyc.companyName || "N/A"}</Typography>

                            <Typography variant="h6" fontWeight="600" gutterBottom>
                                Tax Code:
                            </Typography>
                            <Typography mb={2}>{kyc.taxCode || "N/A"}</Typography>

                            <Typography variant="h6" fontWeight="600" gutterBottom>
                                Business License:
                            </Typography>
                            <Typography mb={2}>{kyc.businessLicense || "N/A"}</Typography>

                            <Typography variant="h6" fontWeight="600" gutterBottom>
                                Address:
                            </Typography>
                            <Typography mb={2}>{kyc.address || "N/A"}</Typography>

                            <Typography variant="h6" fontWeight="600" gutterBottom>
                                Created At:
                            </Typography>
                            <Typography mb={2}>{ formatVNDate(kyc.createAt) }</Typography>

                            <Typography variant="h6" fontWeight="600" gutterBottom>
                                Update At:
                            </Typography>
                            <Typography mb={2}>{ formatVNDate(kyc.updatedAt) }</Typography>
                        </Grid>
                    )}

                    {/* COLUMN 3: Energy & CO2 Info */}
                    <Grid item xs={12} md={4}>
                        <Typography variant="h5" fontWeight="700" color="secondary" gutterBottom>
                            Energy & CO₂ Info
                        </Typography>

                        <Typography variant="h6" fontWeight="600" gutterBottom>
                            Total Energy:
                        </Typography>
                        <Typography mb={2}>{report.totalEnergy?.toLocaleString() || 0}</Typography>

                        <Typography variant="h6" fontWeight="600" gutterBottom>
                            Total CO₂:
                        </Typography>
                        <Typography mb={2}>{report.totalCo2?.toLocaleString() || 0}</Typography>

                        <Typography variant="h6" fontWeight="600" gutterBottom>
                            Vehicle Count:
                        </Typography>
                        <Typography mb={2}>{report.vehicleCount || 0}</Typography>


                    </Grid>

                    {/* COLUMN 4: Verification & Admin Info */}
                    <Grid item xs={12} md={4} sx={{
                        flexBasis: "30%",     // chiếm 40% chiều ngang container
                        maxWidth: "30%",      // giới hạn chiều rộng
                    }}>
                        <Typography variant="h5" fontWeight="700" color="secondary" gutterBottom>
                            Verification & Admin
                        </Typography>


                        <Typography variant="h6" fontWeight="600" gutterBottom>
                            Verified By:
                        </Typography>
                        <Typography mb={2}>{report.verifiedByCvaName || "N/A"}</Typography>

                        <Typography variant="h6" fontWeight="600" gutterBottom>
                            Verified At:
                        </Typography>
                        <Typography mb={2}>{ formatVNDate(report.verifiedAt) }</Typography>

                        <Typography variant="h6" fontWeight="600" gutterBottom>
                            Admin Comment:
                        </Typography>
                        <TextField
                            value={report.adminComment || "N/A"}
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
                                width: "80%",
                                mb: 2,
                                backgroundColor: "rgba(255,255,255,0.08)",
                                borderRadius: "8px",
                                "& .MuiInputBase-input.Mui-disabled": {
                                    WebkitTextFillColor: "#ccc", // màu chữ nếu theme tối
                                },
                            }}
                        />

                        <Typography variant="h6" fontWeight="600" gutterBottom>
                            Approved By:
                        </Typography>
                        <Typography mb={2}>{report.adminApprovedByName || "N/A"}</Typography>

                        <Typography variant="h6" fontWeight="600" gutterBottom>
                            Approved At:
                        </Typography>
                        <Typography mb={2}>
                            { formatVNDate(report.approvedAt) }
                        </Typography>
                    </Grid>
                </Grid>



                {/* Action Buttons */}
                <Box display="flex" justifyContent="flex-end" gap={2} mt={4}>
                    <Button
                        variant="outlined"
                        color="info"
                        onClick={() => navigate("/admin/report_management")}
                        sx={{ fontWeight: 600 }}
                    >
                        Back
                    </Button>
                    <Button
                        variant="contained"
                        color="secondary"
                        onClick={() => navigate(`/admin/edit_report/${report.id}`)}
                        sx={{ fontWeight: 600 }}
                    >
                        Edit
                    </Button>
                </Box>
            </Paper>

            {/* Snackbar */}
            <Snackbar
                open={snackbar.open}
                autoHideDuration={3000}
                onClose={() => setSnackbar({ ...snackbar, open: false })}
                anchorOrigin={{ vertical: "top", horizontal: "center" }}
            >
                <Alert severity={snackbar.severity} variant="filled">
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default ReportView;
