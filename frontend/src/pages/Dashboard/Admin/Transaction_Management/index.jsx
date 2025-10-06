import { Box, Typography, useTheme } from "@mui/material";
import { DataGrid } from "@mui/x-data-grid";
import { tokens } from "@/theme";
import { mockDataInvoices } from "@/data/mockData";
import Header from "@/components/Chart/Header.jsx";

const Invoices = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const columns = [
    { field: "id", headerName: "" },
    { field: "transactionid", headerName: "Transaction ID", flex: 1 },
    {
      field: "trader",
      headerName: "Trader",
      flex: 1,
      cellClassName: "name-column--cell",
    },
    {
      field: "email",
      headerName: "Email",
      flex: 1,
    },
    {
      field: "transactiontype", // chú ý tên field này phải trùng với tên trong mockData,3 loại là mua/bán/rút tiền
      headerName: "Transaction Type",
      flex: 1,
      renderCell: ({ row: { transactionType } }) => {
        const colorMap = {
          buy: colors.greenAccent[500],
          sell: colors.redAccent[500],
          withdraw: colors.blueAccent[500],
        };
        return (
          <Box
            display="flex"
            alignItems="center"
            justifyContent="left"
            height="100%"
          >
            <Typography
              color={colorMap[transactionType] || colors.grey[100]}
              fontWeight="600"
              sx={{ textTransform: "capitalize", lineHeight: 1 }}
            >
              {transactionType}
            </Typography>
          </Box>
        );
      },
    },
    {
      field: "date",
      headerName: "Date",
      flex: 1,
    },
    {
      field: "cost",
      headerName: "Cost",
      flex: 1,
      renderCell: (params) => (
        <Box
          display="flex"
          alignItems="center"
          justifyContent="left"
          height="100%"
        >
          <Typography color={colors.greenAccent[500]}>
            ${params.row.cost}
          </Typography>
        </Box>
      ),
    },
    {
      field: "status",// chú ý tên field này phải trùng với tên trong mockData, 3 loại là pending/paid/failed
      headerName: "Status",
      flex: 1,
      renderCell: ({ row: { status } }) => {
        const statusColorMap = {
          pending: colors.grey[300],
          paid: colors.greenAccent[400],
          failed: colors.redAccent[400],
        };
        return (
          <Box
            display="flex"
            alignItems="center"
            justifyContent="left"
            height="100%"
          >
            <Typography
              color={statusColorMap[status] || colors.grey[100]}
              fontWeight="600"
              sx={{ textTransform: "capitalize", lineHeight: 1 }}
            >
              {status}
            </Typography>
          </Box>
        );
      },
    },
    
  ];

  return (
    <Box m="20px">
      <Header title="TRANSACTIONS" subtitle="List of Transactions" />
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
        <DataGrid checkboxSelection rows={mockDataInvoices} columns={columns} />
      </Box>
    </Box>
  );
};

export default Invoices;
