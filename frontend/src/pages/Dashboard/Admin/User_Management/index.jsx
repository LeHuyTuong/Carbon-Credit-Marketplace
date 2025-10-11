import { Box, Typography, useTheme } from "@mui/material";
import { DataGrid } from "@mui/x-data-grid";
import { tokens } from "@/theme";
import AdminPanelSettingsOutlinedIcon from "@mui/icons-material/AdminPanelSettingsOutlined";
import LockOpenOutlinedIcon from "@mui/icons-material/LockOpenOutlined";
import SecurityOutlinedIcon from "@mui/icons-material/SecurityOutlined";
import Header from "@/components/Chart/Header.jsx";

const accessConfig = {
  admin: {
    label: "Admin",
    icon: <AdminPanelSettingsOutlinedIcon />,
    bg: "greenAccent.600",
  },
  cva: {
    label: "CVA", // đổi tên hiển thị mà ko cần đổi data BE
    icon: <SecurityOutlinedIcon />,
    bg: "greenAccent.700",
  },
  ev_owner: {
    label: "Ev-Owner",
    icon: <LockOpenOutlinedIcon />,
    bg: "greenAccent.700",
  },
  cc_buyer: {
    label: "CC-Buyer",
    icon: <LockOpenOutlinedIcon />,
    bg: "greenAccent.700",
  },

};

const Team = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const columns = [
    { field: "id", headerName: "" },
    { field: "userid", headerName: "User ID" },

    {
      field: "name",
      headerName: "Name",
      flex: 1,
      cellClassName: "name-column--cell",
    },
    {
      field: "status",
      headerName: "Account Status",
      flex: 1,
      renderCell: ({ row: { status } }) => {
        return (
          <Box
            display="flex"
            alignItems="center"
            justifyContent="left"
            height="100%"
          >
            <Typography
              color={status === "active" ? "green" : "red"}
              fontWeight="600"
              sx={{ lineHeight: 1 }}
            >
              {status === "active" ? "Active" : "Inactive"}
            </Typography>
          </Box>
        );
      },
    },
    {
      field: "phone",
      headerName: "Phone Number",
      flex: 1,
    },
    {
      field: "email",
      headerName: "Email",
      flex: 1,
    },
    {
      field: "date",
      headerName: "Created Date",
      flex: 1,
    },
    {
      field: "accesslevel",
      headerName: "Access Level",
      flex: 1,
      renderCell: ({ row: { access } }) => {
        const config = accessConfig[access] || {};
        return (
          <Box
            width="60%"
            m="0 auto"
            p="5px"
            display="flex"
            justifyContent="center"
            backgroundColor={config.bg ? colors[config.bg.split(".")[0]][config.bg.split(".")[1]] : colors.grey[700]}
            borderRadius="4px"
          >
            {config.icon}
            <Typography color={colors.grey[100]} sx={{ ml: "5px" }}>
              {config.label || access}
            </Typography>
          </Box>
        );
      },
    },

  ];

  return (
    <Box m="20px">
      <Header title="USERS" subtitle="Managing the Users" />
      <Box
        m="40px 0 0 0"
        height="75vh"
        sx={{
          "& .MuiDataGrid-root": {
            border: "none",
          },
          "& .MuiDataGrid-cell": {
            borderBottom: "none",
          },
          "& .name-column--cell": {
            color: colors.greenAccent[300],
          },
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
        <DataGrid checkboxSelection rows={mockDataTeam} columns={columns} />
      </Box>
    </Box>
  );
};

export default Team;
