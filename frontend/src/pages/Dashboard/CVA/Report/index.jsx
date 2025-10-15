import { Box, Typography, useTheme } from "@mui/material";
import { DataGrid } from "@mui/x-data-grid";
import { tokens } from "@/themeCVA";
import Header from "@/components/Chart/Header.jsx";
import { Link } from "react-router-dom";
import "@/styles/actionadmin.scss";
import { mockDataReportsCVA } from "@/data/mockData";

const Team = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  // chỉ cần hiển thị dữ liệu gốc, không cần state / localStorage
  const data = mockDataReportsCVA;

  const columns = [
    { field: "id", headerName: "" },
    { field: "reportid", headerName: "Report ID" },
    {
      field: "company",
      headerName: "Company",
      flex: 1,
      cellClassName: "name-column--cell",
    },
    { field: "reportingperiod", headerName: "Reporting Period", flex: 1 },
    { field: "totalEV", headerName: "Total EV", flex: 1 },
    { field: "totalcredits", headerName: "Total Credits", flex: 1 },
    { field: "submissiondate", headerName: "Submission Date", flex: 1},
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
    {
      field: "action",
      headerName: "Action",
      flex: 1,
      renderCell: (params) => (
        <div className="cellAction">
          <Link to={`/cva/view_report/${params.row.id}`} style={{ textDecoration: "none" }}>
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
          "& .MuiDataGrid-cell": { borderBottom: "none" },
          "& .name-column--cell": { color: colors.greenAccent[300] },
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
        <DataGrid checkboxSelection rows={data} columns={columns} />
      </Box>
    </Box>
  );
};

export default Team;
