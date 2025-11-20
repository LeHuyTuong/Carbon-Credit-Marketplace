import { useState, useEffect } from "react";
import { ProSidebar, Menu, MenuItem } from "react-pro-sidebar";
import { Box, Typography, useTheme } from "@mui/material";
import { Link, useLocation } from "react-router-dom";
import "react-pro-sidebar/dist/css/styles.css";
import { tokens } from "@/theme";
import AdminPanelSettings from "@mui/icons-material/AdminPanelSettings";
import HomeOutlinedIcon from "@mui/icons-material/HomeOutlined";
import PeopleOutlinedIcon from "@mui/icons-material/PeopleOutlined";
import CreditCardIcon from "@mui/icons-material/CreditCard";
import WorkOutlineIcon from "@mui/icons-material/WorkOutline";
import DescriptionOutlinedIcon from "@mui/icons-material/DescriptionOutlined";
import AccountBalanceWalletOutlinedIcon from "@mui/icons-material/AccountBalanceWalletOutlined";
import CorporateFareOutlinedIcon from "@mui/icons-material/CorporateFareOutlined";
import ElectricCarOutlinedIcon from "@mui/icons-material/ElectricCarOutlined";
import PersonOutlinedIcon from "@mui/icons-material/PersonOutlined";
import BarChartOutlinedIcon from "@mui/icons-material/BarChartOutlined";
import PieChartOutlineOutlinedIcon from "@mui/icons-material/PieChartOutlineOutlined";
import TimelineOutlinedIcon from "@mui/icons-material/TimelineOutlined";
import { checkKYCAdmin } from "@/apiAdmin/apiLogin.js";

const Item = ({ title, to, icon, selected, setSelected }) => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  return (
    <MenuItem
      active={selected === to}
      style={{
        color: colors.grey[100],
      }}
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
  const [selected, setSelected] = useState(location.pathname);
  const [adminInfo, setAdminInfo] = useState({ name: "", role: "" });

  useEffect(() => {
    const fetchAdminData = async () => {
      try {
        const token = localStorage.getItem("admin_token");
        if (!token) throw new Error("No admin token found!");

        const kycData = await checkKYCAdmin();
        const role = localStorage.getItem("admin_role") || "Admin";
        const name = kycData?.name || "Unknown";

        setAdminInfo({ name, role });
      } catch (err) {
        console.error("Failed to fetch admin data:", err.message);
        setAdminInfo({ name: "Unknown", role: "Admin" });
      }
    };

    fetchAdminData();
  }, []);

  useEffect(() => {
    setSelected(location.pathname);
  }, [location]);

  return (
    <Box
      sx={{
        position: "fixed",
        left: 0,
        top: 0,
        height: "100vh",
        zIndex: 100,
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
          color: "#868dfb !important",
        },
        "& .pro-menu-item.active": {
          color: "#6870fa !important",
        },
      }}
    >
      <ProSidebar collapsed={false}>
        <Menu iconShape="square">
          {/* LOGO AND HEADER */}
          <MenuItem
            style={{
              margin: "10px 0 20px 0",
              color: colors.grey[100],
            }}
          >
            <Box
              display="flex"
              justifyContent="center"
              alignItems="center"
              ml="15px"
            >
              <Typography variant="h3" color={colors.grey[100]}>
                ADMIN PAGE
              </Typography>
            </Box>
          </MenuItem>

          <Box mb="25px">
            <Box display="flex" justifyContent="center" alignItems="center">
              <AdminPanelSettings
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
            <Box textAlign="center">
              <Typography
                variant="h2"
                color={colors.grey[100]}
                fontWeight="bold"
                sx={{ m: "10px 0 0 0" }}
              >
                {adminInfo.name || "Loading..."}
              </Typography>
              <Typography variant="h5" color={colors.greenAccent[500]}>
                {adminInfo.role || "Admin"}
              </Typography>
            </Box>
          </Box>

          {/* MENU ITEMS */}
          <Box paddingLeft="10%">
            <Item
              title="Dashboard"
              to="/admin/dashboard"
              icon={<HomeOutlinedIcon />}
              selected={selected}
              setSelected={setSelected}
            />

            <Typography
              variant="h6"
              color={colors.grey[300]}
              sx={{ m: "15px 0 5px 20px" }}
            >
              Data
            </Typography>
            <Item
              title="Manage Users"
              to="/admin/user_management"
              icon={<PeopleOutlinedIcon />}
              selected={selected}
              setSelected={setSelected}
            />
            <Item
              title="Manage EV Owners"
              to="/admin/ev_owner_management"
              icon={<PeopleOutlinedIcon />}
              selected={selected}
              setSelected={setSelected}
            />
            <Item
              title="Manage Companies"
              to="/admin/companies_management"
              icon={<PeopleOutlinedIcon />}
              selected={selected}
              setSelected={setSelected}
            />
            <Item
              title="Manage CVA"
              to="/admin/cva_management"
              icon={<PeopleOutlinedIcon />}
              selected={selected}
              setSelected={setSelected}
            />
            <Item
              title="Manage Certificates"
              to="/admin/credit_management"
              icon={<CreditCardIcon />}
              selected={selected}
              setSelected={setSelected}
            />
            <Item
              title="Manage Withdrawl"
              to="/admin/transaction_management"
              icon={<AccountBalanceWalletOutlinedIcon />}
              selected={selected}
              setSelected={setSelected}
            />
            <Item
              title="Manage Reports"
              to="/admin/report_management"
              icon={<DescriptionOutlinedIcon />}
              selected={selected}
              setSelected={setSelected}
            />
            <Item
              title="Manage E-Vehicles"
              to="/admin/ev_management"
              icon={<ElectricCarOutlinedIcon />}
              selected={selected}
              setSelected={setSelected}
            />
            <Item
              title="Manage Projects"
              to="/admin/project_management"
              icon={<WorkOutlineIcon />}
              selected={selected}
              setSelected={setSelected}
            />
            <Item
              title="Manage Application"
              to="/admin/company_management"
              icon={<CorporateFareOutlinedIcon />}
              selected={selected}
              setSelected={setSelected}
            />

            {/* <Typography
              variant="h6"
              color={colors.grey[300]}
              sx={{ m: "15px 0 5px 20px" }}
            >
              Pages
            </Typography>
            <Item
              title="Profile Form"
              to="/admin/form"
              icon={<PersonOutlinedIcon />}
              selected={selected}
              setSelected={setSelected}
            />

            <Typography
              variant="h6"
              color={colors.grey[300]}
              sx={{ m: "15px 0 5px 20px" }}
            >
              Charts
            </Typography>
            <Item
              title="Bar Chart"
              to="/admin/bar"
              icon={<BarChartOutlinedIcon />}
              selected={selected}
              setSelected={setSelected}
            />
            <Item
              title="Pie Chart"
              to="/admin/pie"
              icon={<PieChartOutlineOutlinedIcon />}
              selected={selected}
              setSelected={setSelected}
            />
            <Item
              title="Line Chart"
              to="/admin/line"
              icon={<TimelineOutlinedIcon />}
              selected={selected}
              setSelected={setSelected}
            /> */}
          </Box>
        </Menu>
      </ProSidebar>
    </Box>
  );
};

export default Sidebar;
