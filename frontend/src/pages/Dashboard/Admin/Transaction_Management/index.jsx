// src/pages/admin/TransactionAdmin.js
import { useEffect, useState } from "react";
import { Box, Typography, useTheme } from "@mui/material";
import { DataGrid } from "@mui/x-data-grid";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { Link } from "react-router-dom";
import { getWithdrawalsAdmin } from "@/apiAdmin/transactionAdmin.js";
import AdminDataGrid from "@/components/DataGrid/AdminDataGrid.jsx";
const TransactionAdmin = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const [data, setData] = useState([]);

  //  Gọi API khi load trang
  useEffect(() => {
    const fetchData = async () => {
      try {
        const list = await getWithdrawalsAdmin();
        const mapped = list.map((item) => ({
          id: item.id,
          transactionId: item.id,
          trader: item.paymentDetails?.accountHolderName || "N/A",
          email: item.user?.email || item.paymentDetails?.user?.email || "N/A",
          transactionType: "withdraw",
          date: new Date(item.requestedAt).toLocaleString(),
          cost: item.amount,
          status: item.status?.toLowerCase(),
        }));
        setData(mapped);
      } catch (error) {
        console.error("Error loading withdrawal data:", error);
      }
    };

    fetchData();
  }, []);

  //  Cột hiển thị
  const columns = [
    { field: "transactionId", headerName: "Transaction ID", flex: 0.8 },
    { field: "email", headerName: "Email", flex: 1 },
    {
      field: "transactionType",
      headerName: "Transaction Type",
      flex: 0.8,
      renderCell: ({ row }) => (

        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "left",
            width: "100%",
            height: "100%",
          }}
        >
          <Typography color={colors.blueAccent[500]} fontWeight="600">
            {row.transactionType}
          </Typography>
        </Box>
      ),
    },
    { field: "date", headerName: "Date", flex: 1 },
    {
      field: "cost",
      headerName: "Amount",
      flex: 0.6,
      renderCell: ({ row }) => (
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "left",
            width: "100%",
            height: "100%",
          }}
        >
          <Typography color={colors.greenAccent[500]}>
            ${row.cost?.toLocaleString()}
          </Typography>
        </Box>
      ),
    },
    {
      field: "status",
      headerName: "Status",
      flex: 0.6,
      renderCell: ({ row }) => {
        const colorMap = {
          pending: colors.grey[300],
          succeeded: colors.greenAccent[400],
          rejected: colors.redAccent[400],
        };
        return (
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              justifyContent: "left",
              width: "100%",
              height: "100%",
            }}
          >
            <Typography
              color={colorMap[row.status] || colors.grey[100]}
              fontWeight="600"
              sx={{ textTransform: "capitalize" }}
            >
              {row.status}
            </Typography>
          </Box>
        );
      },
    },
    {
      field: "action",
      headerName: "Action",
      flex: 0.8,
      renderCell: (params) => (
        <Link
          to={`/admin/transaction/${params.row.id}`}
          style={{ textDecoration: "none" }}
        >
          <div className="viewButton">View</div>
        </Link>
      ),
    },
  ];

  return (
    <Box m="20px" sx={{ marginLeft: "290px" }}>
      <Header title="WITHDRAWAL TRANSACTIONS" subtitle="List of Withdrawal Requests" />
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

export default TransactionAdmin;
