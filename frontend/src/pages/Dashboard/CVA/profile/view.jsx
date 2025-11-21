import { useState, useEffect } from "react";
import {
  Box,
  Typography,
  Avatar,
  Paper,
  Divider,
  useMediaQuery,
  useTheme,
  ListItemButton,
  ListItemIcon,
  ListItemText,
} from "@mui/material";
import { EditOutlined } from "@mui/icons-material";
import Header from "@/components/Chart/Header.jsx";
import { tokens } from "@/themeCVA";
import { checkKYCCVA } from "@/apiCVA/apiAuthor.js";
import { Link } from "react-router-dom";

const CVAProfile = ({ onClose }) => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const isNonMobile = useMediaQuery("(min-width:600px)");

  // State lưu dữ liệu CVA
  const [cvaData, setCvaData] = useState(
    JSON.parse(localStorage.getItem("cvaData")) || {}
  );

  useEffect(() => {
    const fetchCVA = async () => {
      const data = await checkKYCCVA();
      if (data) {
        setCvaData(data); // data là object "response" từ API
      }
    };
    fetchCVA();
  }, []);

  return (
    <Box m="20px" sx={{ marginLeft: "290px" }}>
      <Header title="CVA PROFILE" subtitle="View CVA Information" />

      {/* PROFILE OVERVIEW  */}
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
  <EditListButton section="Profile Overview" handleEdit={() => onClose?.()} />
 {/* Avatar và Name + positionTitle + organization */}
  <Avatar
    src={cvaData.avatar?.startsWith("data:") ? cvaData.avatarUrl : cvaData.avatarUrl?.replace("@/", "/")}
    alt="CVA Avatar"
    sx={{
      width: 120,
      height: 120,
      border: `3px solid ${colors.greenAccent[400]}`,
    }}
  />

  {/* Box chứa Name + positionTitle + organization */}
  <Box display="flex" flexDirection="column" justifyContent="center">
    <Typography variant="h4" fontWeight="bold" color={colors.grey[100]}>
      {cvaData.name || "-"}
    </Typography>

    {/* Căn giữa positionTitle so với Avatar */}
    <Box display="flex" alignItems="center" height={40} mt={1}>
      <Typography variant="h5" color={colors.greenAccent[400]}>
        {cvaData.positionTitle || "CVA"}
      </Typography>
    </Box>

    <Typography variant="body1" color={colors.grey[200]} mt={0.5}>
      {cvaData.organization || "-"}
    </Typography>
  </Box>
</Paper>


      {/* PERSONAL INFORMATION */}
      <Paper
        elevation={3}
        sx={{
          p: 4,
          borderRadius: "16px",
          backgroundColor: colors.primary[400],
        }}
      >
        <Typography variant="h5" fontWeight="bold" mb={2} color={colors.greenAccent[400]}>
          Personal Information
        </Typography>
        <Divider sx={{ mb: 3, backgroundColor: colors.grey[700] }} />
        {/* Info Items */}
        <Box display="grid" gridTemplateColumns="repeat(auto-fit, minmax(250px, 1fr))" gap={2}>
          <InfoItem label="Full Name" value={cvaData.name} />
          <InfoItem label="Email" value={cvaData.email} />
          <InfoItem label="Organization" value={cvaData.organization} />
          <InfoItem label="Position Title" value={cvaData.positionTitle} />
          <InfoItem label="KYC Status" value={cvaData.status} />
          <InfoItem label="Created At" value={new Date(cvaData.createdAt).toLocaleString()} />
          <InfoItem label="Updated At" value={new Date(cvaData.updatedAt).toLocaleString()} />
        </Box>
      </Paper>
    </Box>
  );
};

const InfoItem = ({ label, value }) => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  return (
    <Box>
      <Typography variant="subtitle2" color={colors.grey[300]}>
        {label}
      </Typography>
      <Typography variant="body1" color={colors.grey[100]} fontWeight="500">
        {value ?? "-"}
      </Typography>
    </Box>
  );
};
// Button chỉnh sửa ở góc trên bên phải
const EditListButton = ({ section, handleEdit }) => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  return (
    <Box sx={{ position: "absolute", top: 8, right: 8 }}>
      <ListItemButton
        onClick={handleEdit}
        sx={{
          p: "6px 12px",
          borderRadius: "8px",
          bgcolor: theme.palette.mode === "dark" ? colors.primary[600] : colors.grey[200],
          "&:hover": {
            bgcolor: theme.palette.mode === "dark" ? colors.greenAccent[600] : colors.greenAccent[200],
          },
          transition: "all 0.2s ease-in-out",
        }}
        component={Link}
        to="/cva/edit_profile_cva"
      >
        <ListItemIcon
          sx={{
            color: theme.palette.mode === "dark" ? colors.grey[100] : colors.grey[900],
            minWidth: 32,
          }}
        >
          <EditOutlined fontSize="small" />
        </ListItemIcon>
        <ListItemText
          primary="Edit Profile"
          primaryTypographyProps={{
            fontSize: "0.85rem",
            color: theme.palette.mode === "dark" ? colors.grey[100] : colors.grey[900],
            fontWeight: 500,
          }}
        />
      </ListItemButton>
    </Box>
  );
};

export default CVAProfile;
