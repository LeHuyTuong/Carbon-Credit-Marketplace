import { useState, useEffect } from "react";
import { ProSidebar, Menu, MenuItem } from "react-pro-sidebar";
import { Box, IconButton, Typography, useTheme, CircularProgress } from "@mui/material";
import { Link, useLocation } from "react-router-dom";
import "react-pro-sidebar/dist/css/styles.css";
import { tokens } from "@/themeCVA";
import SecurityIcon from "@mui/icons-material/Security";
import HomeOutlinedIcon from "@mui/icons-material/HomeOutlined";
import PeopleOutlinedIcon from "@mui/icons-material/PeopleOutlined";
import ReceiptOutlinedIcon from "@mui/icons-material/ReceiptOutlined";
import MenuOutlinedIcon from "@mui/icons-material/MenuOutlined";
import { checkKYCCVA } from "@/apiCVA/apiAuthor.js";

// Component menu item
const Item = ({ title, to, icon, selected, setSelected }) => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  return (
    <MenuItem
      active={selected === to} //so sánh theo đường dẫn
      style={{ color: colors.grey[100] }}
      onClick={() => setSelected(to)}
      icon={icon}
    >
      <Typography>{title}</Typography>
      <Link to={to} />
    </MenuItem>
  );
};

const Sidebar = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const location = useLocation();

  const [isCollapsed, setIsCollapsed] = useState(false);
  const [selected, setSelected] = useState(location.pathname); // lưu trực tiếp đường dẫn

  // Khi đổi route (ví dụ reload/hoặc chuyển trang) cập nhật selected
  useEffect(() => {
    setSelected(location.pathname);
  }, [location.pathname]);

  // KYC API
  const [kycInfo, setKycInfo] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchKYC = async () => {
      try {
        const data = await checkKYCCVA();
        if (data) setKycInfo(data);
      } catch (err) {
        console.error("Failed to fetch KYC:", err);
      } finally {
        setLoading(false);
      }
    };
    fetchKYC();
  }, []);

  // Định nghĩa danh sách menu 1 chỗ
  const menuItems = [
    { title: "Dashboard", to: "/cva/dashboard", icon: <HomeOutlinedIcon /> },
    { section: "Data" },
    { title: "Manage Reports", to: "/cva/report_management", icon: <PeopleOutlinedIcon /> },
    { title: "Manage Applications_Projects", to: "/cva/registration_project_management", icon: <ReceiptOutlinedIcon /> },
  ];

  return (
    <Box
      sx={{
        "& .pro-sidebar-inner": {
          background: `${colors.primary[400]} !important`,
        },
        "& .pro-icon-wrapper": {
          backgroundColor: "transparent !important",
        },
        "& .pro-inner-item": {
          padding: "5px 35px 5px 20px !important",
        },
        "& .pro-inner-item:hover": {
          color: "#86fbdcff !important",
        },
        "& .pro-menu-item.active": {
          color: "#68fa80ff !important",
        },
      }}
    >
      <ProSidebar collapsed={isCollapsed}>
        <Menu iconShape="square">
          {/* Header */}
          <MenuItem
            onClick={() => setIsCollapsed(!isCollapsed)}
            icon={isCollapsed ? <MenuOutlinedIcon /> : undefined}
            style={{
              margin: "10px 0 20px 0",
              color: colors.grey[100],
            }}
          >
            {!isCollapsed && (
              <Box display="flex" justifyContent="space-between" alignItems="center" ml="15px">
                <Typography variant="h3" color={colors.grey[100]}>
                  Verification
                </Typography>
                <IconButton onClick={() => setIsCollapsed(!isCollapsed)}>
                  <MenuOutlinedIcon />
                </IconButton>
              </Box>
            )}
          </MenuItem>

          {/* Avatar */}
          {!isCollapsed && (
            <Box mb="25px">
              <Box display="flex" justifyContent="center" alignItems="center">
                <SecurityIcon
                  sx={{
                    fontSize: "100px",
                    color: colors.greenAccent[500],
                    cursor: "pointer",
                    backgroundColor: colors.primary[400],
                    borderRadius: "50%",
                    padding: "10px",
                  }}
                />
              </Box>

              <Box textAlign="center" sx={{ mt: 2 }}>
                {loading ? (
                  <CircularProgress size={28} color="inherit" />
                ) : (
                  <>
                    <Typography
                      variant="h2"
                      color={colors.grey[100]}
                      fontWeight="bold"
                      sx={{ m: "10px 0 0 0" }}
                    >
                      {kycInfo?.name || "Unknown User"}
                    </Typography>
                    <Typography variant="h5" color={colors.greenAccent[500]}>
                      {kycInfo?.positionTitle || kycInfo?.organization || "CVA"}
                    </Typography>
                  </>
                )}
              </Box>
            </Box>
          )}

          {/* Menu items */}
          <Box paddingLeft={isCollapsed ? undefined : "10%"}>
            {menuItems.map((item, idx) =>
              item.section ? (
                <Typography
                  key={`section-${idx}`}
                  variant="h6"
                  color={colors.grey[300]}
                  sx={{ m: "15px 0 5px 20px" }}
                >
                  {item.section}
                </Typography>
              ) : (
                <Item
                  key={item.to}
                  title={item.title}
                  to={item.to}
                  icon={item.icon}
                  selected={selected}
                  setSelected={setSelected}
                />
              )
            )}
          </Box>
        </Menu>
      </ProSidebar>
    </Box>
  );
};

export default Sidebar;
