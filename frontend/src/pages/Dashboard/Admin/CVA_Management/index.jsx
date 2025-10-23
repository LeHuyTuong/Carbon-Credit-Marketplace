import { Box, Typography, useTheme } from "@mui/material";
import { DataGrid, GridToolbar } from "@mui/x-data-grid";
import { tokens } from "@/theme";
import SecurityOutlinedIcon from "@mui/icons-material/SecurityOutlined";
import Header from "@/components/Chart/Header.jsx";
import { Link } from "react-router-dom";
import { useState, useEffect } from "react";
import "@/styles/actionadmin.scss";
import { getAllUsers } from "@/apiAdmin/userAdmin.js";

const CvaTeam = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const [data, setData] = useState([]);

  useEffect(() => {
    async function fetchUsers() {
      try {
        const response = await getAllUsers();
        if (response?.responseData) {
          const users = response.responseData
            .filter((u) => u.roles?.[0]?.name?.toUpperCase() === "CVA")
            .map((u, index) => ({
              id: index + 1,
              userid: u.id,
              email: u.email,
              status: u.status?.toLowerCase() === "active" ? "active" : "inactive",
              access: u.roles?.[0]?.name || "Unknown",
              balance: u.wallet?.balance ?? 0,
            }));
          setData(users);
        }
      } catch (err) {
        console.error("Error fetching CVA users:", err);
      }
    }

    fetchUsers();
  }, []);

  const columns = [
    { field: "id", headerName: "#" },
    { field: "userid", headerName: "User ID" },
    { field: "email", headerName: "Email", flex: 1 },
    {
      field: "status",
      headerName: "Status",
      flex: 1,
      renderCell: ({ row: { status } }) => (
        <Typography color={status === "active" ? "green" : "red"} fontWeight="600">
          {status}
        </Typography>
      ),
    },
    {
      field: "balance",
      headerName: "Wallet Balance",
      flex: 1,
      renderCell: ({ row }) => <Typography>{row.balance} ₫</Typography>,
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
      <Header title="CVA USERS" subtitle="Managing CVA Accounts" />
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
        }}
      >
        <DataGrid rows={data} columns={columns} components={{ Toolbar: GridToolbar }} />
      </Box>
    </Box>
  );
};

export default CvaTeam;
