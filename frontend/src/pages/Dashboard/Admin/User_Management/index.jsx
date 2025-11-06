import { Box, Typography, useTheme } from "@mui/material";
import { DataGrid, GridToolbar } from "@mui/x-data-grid";
import { tokens } from "@/theme";
import AdminPanelSettingsOutlinedIcon from "@mui/icons-material/AdminPanelSettingsOutlined";
import LockOpenOutlinedIcon from "@mui/icons-material/LockOpenOutlined";
import SecurityOutlinedIcon from "@mui/icons-material/SecurityOutlined";
import Header from "@/components/Chart/Header.jsx";
import { Link } from "react-router-dom";
import { useState, useEffect } from "react";
import "@/styles/actionadmin.scss";
import { getAllUsers } from "@/apiAdmin/userAdmin.js";
import AdminDataGrid from "@/components/DataGrid/AdminDataGrid.jsx";
const accessConfig = {
  ADMIN: {
    label: "Admin",
    icon: <AdminPanelSettingsOutlinedIcon />,
    bg: "greenAccent.600",
  },
  CVA: {
    label: "CVA",
    icon: <SecurityOutlinedIcon />,
    bg: "greenAccent.700",
  },
  EV_OWNER: {
    label: "EV Owner",
    icon: <LockOpenOutlinedIcon />,
    bg: "greenAccent.700",
  },
  COMPANY: {
    label: "Company",
    icon: <LockOpenOutlinedIcon />,
    bg: "greenAccent.700",
  },
};

const Team = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const [data, setData] = useState([]);

  useEffect(() => {
    async function fetchUsers() {
      try {
        const response = await getAllUsers();

        if (response?.responseData) {
          const users = response.responseData.map((u, index) => ({
            id: index + 1,
            userid: u.id,
            email: u.email,
            status: u.status?.toLowerCase() === "active" ? "active" : "inactive",
            access: u.roles?.[0]?.name || "Unknown",
            balance: u.wallet?.balance ?? 0,
          }));
          setData(users);
          localStorage.setItem("userData", JSON.stringify(users));
        } else {
          console.error("No user data found:", response);
        }
      } catch (err) {
        console.error("Error fetching users:", err);
      }
    }

    fetchUsers();
  }, []);

  const columns = [
    { field: "userid", headerName: "User ID" },
    { field: "email", headerName: "Email", flex: 1 },
    {
      field: "status",
      headerName: "Account Status",
      flex: 1,
      renderCell: ({ row: { status } }) => (
        <Box
        sx={{
              display: "flex",
              alignItems: "center",
              justifyContent: "left",
              width: "100%",
              height: "100%",
            }}
        >
        <Typography color={status === "active" ? "green" : "red"} fontWeight="600">
          {status === "active" ? "Active" : "Inactive"}
        </Typography>
        </Box>
      ),
    },
    {
      field: "access", // trùng với row.access để filter/sort được
      headerName: "Access Level",
      flex: 1.2,
      renderCell: ({ row: { access } }) => {
        const config = accessConfig[access?.toUpperCase()] || {};
        return (
          <Box display="flex" alignItems="center" justifyContent="center" width="100%" height="100%">
            <Box
              display="flex"
              alignItems="center"
              justifyContent="center"
              width="110px"
              py="6px"
              borderRadius="6px"
              sx={{
                backgroundColor: config.bg
                  ? colors[config.bg.split(".")[0]][config.bg.split(".")[1]]
                  : colors.grey[700],
              }}
            >
              {config.icon}
              <Typography color={colors.grey[100]} sx={{ ml: "6px" }}>
                {config.label || access}
              </Typography>
            </Box>
          </Box>
        );
      },
    },
    {
      field: "balance",
      headerName: "Wallet Balance",
      flex: 1,
      renderCell: ({ row }) => <Box
       sx={{
              display: "flex",
              alignItems: "center",
              justifyContent: "left",
              width: "100%",
              height: "100%",
            }}
      ><Typography>{row.balance} ₫</Typography></Box>,
    },
    {
      field: "action",
      headerName: "Action",
      flex: 0.8,
      renderCell: (params) => (
        <div className="cellAction">
          <Link to={`/admin/view_user/${params.row.email}`} style={{ textDecoration: "none" }}>
            <div className="viewButton">View</div>
          </Link>
        </div>
      ),
    },
  ];

  return (
    <Box m="20px" className="actionadmin">
      <Header title="USERS" subtitle="Managing the Users" />
      <Box
        m="40px 0 0 0"
        height="75vh"
        sx={{
          "& .MuiDataGrid-root": { border: "none" },
          "& .MuiDataGrid-columnHeaders": {
            backgroundColor: colors.blueAccent[700],
            borderBottom: "none",
          },
          "& .MuiDataGrid-virtualScroller": {
            backgroundColor: colors.primary[400],
          },
          "& .MuiDataGrid-footerContainer": {
            borderTop: "none",
            backgroundColor: colors.blueAccent[700],
          },
          "& .MuiCheckbox-root": {
            color: `${colors.greenAccent[200]} !important`,
          },
        }}
      >
         <AdminDataGrid rows={data} columns={columns} getRowId={(r) => r.id} />

      </Box>
    </Box>
  );
};

export default Team;
