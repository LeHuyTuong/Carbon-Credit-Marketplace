import { Box, Typography, Avatar, useMediaQuery, Paper, Divider, useTheme } from "@mui/material";
import { EditOutlined } from "@mui/icons-material";
import Header from "@/components/Chart/Header.jsx";
import { tokens } from "@/theme";
import { useEffect, useState } from "react";
import { checkKYCAdmin } from "@/apiAdmin/apiLogin.js";
import { useNavigate } from "react-router-dom";

const AdminProfile = ({ onClose }) => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const isNonMobile = useMediaQuery("(min-width:600px)");
  const navigate=useNavigate();

  const [admin, setAdmin] = useState({
    name: "Loading...",
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    role: "Admin",
    country: "",
    city: "",
    birthday: "",
    avatarUrl: null,
  });

  useEffect(() => {
    const fetchAdminData = async () => {
      try {
        const res = await checkKYCAdmin();
        console.log("AdminProfile fetched res:", res);

        const data = res; // chỉ lấy res.response, không thêm .response nữa
        if (!data) throw new Error("KYC data not found");

        setAdmin({
          name: data.name || "Unknown",
          firstName: data.firstName || data.name || "",
          lastName: data.lastName || "",
          email: data.email || "",
          phone: data.phone || "",
          role: "Admin",
          country: data.country || "Viet Nam",
          city: data.city || "Ho Chi Minh",
          birthday: data.birthday || "Not provided",
          avatarUrl: data.avatarUrl || null,
        });
      } catch (error) {
        console.error("Failed to fetch KYC admin data:", error.message);
      }
    };

    fetchAdminData();
  }, []);


   // Chuyển sang trang edit profile
  const handleEdit = () => {
    // Lưu dữ liệu hiện tại vào sessionStorage để edit profile dùng
    sessionStorage.setItem("editAdminData", JSON.stringify(admin));
    navigate("/admin/edit_profile_admin"); // route EditProfile
  };

  return (
    <Box m="20px">
      <Header title="ADMIN PROFILE" subtitle="View Administrator Information" />

      {/* ===== 1. PROFILE OVERVIEW ===== */}
      <Paper
        elevation={3}
        sx={{
          position: "relative",
          p: 4,
          mb: 4,
          display: "flex",
          flexDirection: isNonMobile ? "row" : "column",
          alignItems: "center",
          gap: 4,
          borderRadius: "16px",
          backgroundColor: colors.primary[400],
        }}
      >
        <EditListButton section="Profile Overview" handleEdit={handleEdit} />

        <Avatar
          src={admin.avatarUrl || undefined}
          alt="Admin Avatar"
          sx={{
            width: 120,
            height: 120,
            border: `3px solid ${colors.greenAccent[400]}`,
            bgcolor: colors.grey[700], // dùng icon nền khi chưa có avatar
          }}
        >
          {!admin.avatarUrl && admin.name[0]} {/* Hiển thị chữ đầu tên */}
        </Avatar>

        <Box>
          <Typography variant="h4" fontWeight="bold" color={colors.grey[100]}>
            {admin.firstName} {admin.lastName}
          </Typography>
          <Typography variant="h6" color={colors.greenAccent[400]} mb={1}>
            {admin.role}
          </Typography>
          <Typography variant="body1" color={colors.grey[200]}>
            {admin.country}, {admin.city}
          </Typography>
        </Box>
      </Paper>

      {/* ===== 2. PERSONAL INFORMATION ===== */}
      <Paper
        elevation={3}
        sx={{
          position: "relative",
          p: 4,
          mb: 4,
          borderRadius: "16px",
          backgroundColor: colors.primary[400],
        }}
      >
        <Typography variant="h5" fontWeight="bold" mb={2} color={colors.greenAccent[400]}>
          Personal Information
        </Typography>
        <Divider sx={{ mb: 3, backgroundColor: colors.grey[700] }} />

        <Box display="grid" gridTemplateColumns="repeat(auto-fit, minmax(250px, 1fr))" gap={2}>
          <InfoItem label="First Name" value={admin.firstName || "Not provided"} />
          <InfoItem label="Last Name" value={admin.lastName || "Not provided"} />
          <InfoItem label="Email" value={admin.email || "Not provided"} />
          <InfoItem label="Phone Number" value={admin.phone || "Not provided"} />
          <InfoItem label="Date of Birth" value={admin.birthday} />
          <InfoItem label="Role" value={admin.role} />
        </Box>
      </Paper>

      {/* ===== 3. ADDRESS ===== */}
      <Paper
        elevation={3}
        sx={{
          position: "relative",
          p: 4,
          borderRadius: "16px",
          backgroundColor: colors.primary[400],
        }}
      >
        <Typography variant="h5" fontWeight="bold" mb={2} color={colors.greenAccent[400]}>
          Address
        </Typography>
        <Divider sx={{ mb: 3, backgroundColor: colors.grey[700] }} />

        <Box display="grid" gridTemplateColumns="repeat(auto-fit, minmax(250px, 1fr))" gap={2}>
          <InfoItem label="Country" value={admin.country || "Viet Nam"} />
          <InfoItem label="City" value={admin.city || "Ho Chi Minh"} />
        </Box>
      </Paper>
    </Box>
  );
};

// Component hiển thị từng dòng thông tin
const InfoItem = ({ label, value }) => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  return (
    <Box>
      <Typography variant="subtitle2" color={colors.grey[300]}>
        {label}
      </Typography>
      <Typography variant="body1" color={colors.grey[100]} fontWeight="500">
        {value}
      </Typography>
    </Box>
  );
};

// Nút Edit dẫn tới trang chỉnh sửa
const EditListButton = ({ section, handleEdit }) => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  return (
    <Box sx={{ position: "absolute", top: 8, right: 8 }}>
      <Box
        onClick={() => handleEdit(section)}
        sx={{
          display: "flex",
          alignItems: "center",
          gap: 1,
          p: "6px 12px",
          borderRadius: "8px",
          bgcolor: theme.palette.mode === "dark" ? colors.primary[600] : colors.grey[200],
          cursor: "pointer",
          "&:hover": {
            bgcolor: theme.palette.mode === "dark" ? colors.greenAccent[600] : colors.greenAccent[200],
          },
        }}
      >
        <EditOutlined fontSize="small" sx={{ color: theme.palette.mode === "dark" ? colors.grey[100] : colors.grey[900] }} />
        <Typography fontSize="0.85rem" fontWeight={500} color={theme.palette.mode === "dark" ? colors.grey[100] : colors.grey[900]}>
          Edit Profile
        </Typography>
      </Box>
    </Box>
  );
};

export default AdminProfile;
