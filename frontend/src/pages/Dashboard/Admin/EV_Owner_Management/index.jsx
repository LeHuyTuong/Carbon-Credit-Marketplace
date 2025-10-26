import { Box, Typography, useTheme } from "@mui/material";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { Link } from "react-router-dom";
import { useState, useEffect } from "react";
import "@/styles/actionadmin.scss";
import { getAllUsers } from "@/apiAdmin/userAdmin.js";
import AdminDataGrid from "@/components/DataGrid/AdminDataGrid.jsx";
const EvOwnerTeam = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const [data, setData] = useState([]);

  useEffect(() => {
    async function fetchUsers() {
      try {
        const response = await getAllUsers();
        if (response?.responseData) {
          const users = response.responseData
            .filter((u) => u.roles?.[0]?.name?.toUpperCase() === "EV_OWNER")
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
        console.error("Error fetching EV Owners:", err);
      }
    }

    fetchUsers();
  }, []);

  const columns = [
    { field: "id", headerName: "" },
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
          <Link to={`/admin/ev_owner_view/${params.row.email}`} style={{ textDecoration: "none" }}>
            <div className="viewButton">View</div>
          </Link>
        </div>
      ),
    },
  ];

  return (
    <Box m="20px" className="actionadmin">
      <Header title="EV OWNERS" subtitle="Managing EV Owner Accounts" />
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
         <AdminDataGrid rows={data} columns={columns} getRowId={(r) => r.id} />

      </Box>
    </Box>
  );
};

export default EvOwnerTeam;
