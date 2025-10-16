import {Box,Typography,Avatar,useMediaQuery,Paper,Divider,useTheme,ListItemButton,ListItemIcon,ListItemText,} from "@mui/material";
import { EditOutlined } from "@mui/icons-material";
import Header from "@/components/Chart/Header.jsx";
import { adminData } from "@/data/mockData";
import { tokens } from "@/themeCVA";
import { Link } from "react-router-dom";

const AdminProfile = ({ onClose }) => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const isNonMobile = useMediaQuery("(min-width:600px)");

  // Lấy dữ liệu từ localStorage (nếu có)
  const storedData = JSON.parse(localStorage.getItem("adminData"));
  const admin = storedData || adminData[0]; // fallback về mockdata nếu chưa chỉnh sửa gì

  const handleEdit = (section) => {
    console.log(`Edit ${section} clicked`);
    if (onClose) onClose();
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
          src={admin.avatar?.startsWith("data:") ? admin.avatar : admin.avatar?.replace("@/", "/")}
          alt="Admin Avatar"
          sx={{
            width: 120,
            height: 120,
            border: `3px solid ${colors.greenAccent[400]}`,
          }}
        />
        <Box>
          <Typography variant="h4" fontWeight="bold" color={colors.grey[100]}>
            {admin.firstName} {admin.lastName}
          </Typography>
          <Typography variant="h6" color={colors.greenAccent[400]} mb={1}>
            {admin.role}
          </Typography>
          <Typography variant="body1" color={colors.grey[200]}>
            {admin.address}
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
          <InfoItem label="First Name" value={admin.firstName} />
          <InfoItem label="Last Name" value={admin.lastName} />
          <InfoItem label="Email" value={admin.email} />
          <InfoItem label="Phone Number" value={admin.contact} />
          <InfoItem label="Date of Birth" value={admin.dob || "Not provided"} />
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

//  Component hiển thị từng dòng thông tin
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

//  Nút Edit dẫn tới trang chỉnh sửa
const EditListButton = ({ section, handleEdit }) => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  return (
    <Box
      sx={{
        position: "absolute",
        top: 8,
        right: 8,
      }}
    >
      <ListItemButton
  onClick={() => handleEdit(section)}
  sx={{
    p: "6px 12px",
    borderRadius: "8px",
    bgcolor:
      theme.palette.mode === "dark"
        ? colors.primary[600]
        : colors.grey[200], // sáng hơn cho light mode
    "&:hover": {
      bgcolor:
        theme.palette.mode === "dark"
          ? colors.greenAccent[600]
          : colors.greenAccent[200],
    },
    transition: "all 0.2s ease-in-out",
  }}
  component={Link}
  to="/admin/edit_profile_admin"
>
  <ListItemIcon
    sx={{
      color:
        theme.palette.mode === "dark"
          ? colors.grey[100]
          : colors.grey[900], // icon tối hơn khi light mode
      minWidth: 32,
    }}
  >
    <EditOutlined fontSize="small" />
  </ListItemIcon>
  <ListItemText
    primary="Edit Profile"
    primaryTypographyProps={{
      fontSize: "0.85rem",
      color:
        theme.palette.mode === "dark"
          ? colors.grey[100]
          : colors.grey[900], // chữ tối hơn khi sáng
      fontWeight: 500,
    }}
  />
</ListItemButton>

    </Box>
  );
};

export default AdminProfile;
