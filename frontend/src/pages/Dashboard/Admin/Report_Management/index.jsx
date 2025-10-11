import { Box, Typography, useTheme } from "@mui/material";
import { DataGrid } from "@mui/x-data-grid";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";

const Invoices = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const columns = [
    { field: "id", headerName: "" },
    { field: "reportid", headerName: "Report ID", flex: 1 },
    {
      field: "aggregator",
      headerName: "Aggregator",
      flex: 1,
      cellClassName: "name-column--cell",
    },
    {
      field: "reportingperiod",
      headerName: "Reporting Period",
      flex: 1,
    },
    {
      field: "totalevowner", 
      headerName: "Total Ev_Owner",
      flex: 1,
    },
    {
      field: "submissiondate",
      headerName: "Submission Date",
      flex: 1,
    },
    
    {
      field: "status",// Trạng thái (Đã duyệt / Chờ duyệt / Bị từ chối)
      headerName: "Status",
      flex: 1,
      renderCell: ({ row: { status } }) => {
        const statusColorMap = {
          Pending: colors.blueAccent[500],
          Approved: colors.greenAccent[500],
          Rejected: colors.redAccent[500],
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
      <Header title="REPORTS" subtitle="List of reports" />
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
        <DataGrid checkboxSelection rows={mockDataReports} columns={columns} />
      </Box>
    </Box>
  );
};

export default Invoices;
