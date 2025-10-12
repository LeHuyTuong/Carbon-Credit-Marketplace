import { Box, Typography, useTheme } from "@mui/material";
import { DataGrid } from "@mui/x-data-grid";
import { tokens } from "@/theme";
import AdminPanelSettingsOutlinedIcon from "@mui/icons-material/AdminPanelSettingsOutlined";
import LockOpenOutlinedIcon from "@mui/icons-material/LockOpenOutlined";
import SecurityOutlinedIcon from "@mui/icons-material/SecurityOutlined";
import Header from "@/components/Chart/Header.jsx";
import { Link } from "react-router-dom";
import { useState } from "react"; //thêm để quản lý dữ liệu
import "@/styles/actionadmin.scss"; // dùng style đã copy từ template

const accessConfig = {
  admin: {
    label: "Admin",
    icon: <AdminPanelSettingsOutlinedIcon />,
    bg: "greenAccent.600",
  },
  cva: {
    label: "CVA", 
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
localStorage.removeItem("userData");
const Team = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);



  // lưu dữ liệu (để có thể xóa hàng)
  const [data, setData] = useState(() => {
  const saved = localStorage.getItem("userData");
  return saved ? JSON.parse(saved) : mockDataTeam;
});

  // thêm hàm xóa
  const handleDelete = (id) => {
    setData(data.filter((item) => item.id !== id));
  };
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
      flex: 1.3,
      renderCell: ({ row: { access } }) => {
        const config = accessConfig[access] || {};
        return (
          <Box
            display="flex"
            alignItems="center"
            justifyContent="center"
            width="100%"
            height="100%"
          >
            <Box
              display="flex"
              alignItems="center"
              justifyContent="center"
              width="100px"
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
      field: "action",
      headerName: "Action",
      flex: 1,
      renderCell: (params) => {
        return (
          <div className="cellAction">
            <Link to={`/admin/view_user/${params.row.id}`} style={{ textDecoration: "none" }}>
              <div className="viewButton">View</div>
            </Link>
            <div
              className="deleteButton"
              onClick={() => handleDelete(params.row.id)}
            >
              Delete
            </div>
          </div>
        );
      },
    },

  ];

  return (
    <Box m="20px" className="actionadmin">
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
          "& .MuiTablePagination-root": {
            display: "flex",
            alignItems: "center",
            justifyContent: "flex-end",
          },
          "& .MuiTablePagination-selectLabel, & .MuiTablePagination-displayedRows": {
            marginTop: 0,
            marginBottom: 0,
            lineHeight: "normal",
          },
          "& .MuiTablePagination-select": {
            marginTop: "0 !important",
            marginBottom: "0 !important",
            paddingTop: "0 !important",
            paddingBottom: "0 !important",
          },
        }}
      >
        {/* Dùng state `data` thay vì mockDataTeam để có thể xóa */}
        <DataGrid checkboxSelection rows={data} columns={columns} />
      </Box>
    </Box>
  );
};

export default Team;
