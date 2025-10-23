import { useEffect, useState } from "react";
import { Box, Typography, useTheme } from "@mui/material";
import { DataGrid } from "@mui/x-data-grid";
import { tokens } from "@/themeCVA";
import Header from "@/components/Chart/Header.jsx";
import { Link } from "react-router-dom";
import { getReportCVAList } from "@/apiCVA/reportCVA.js";
import "@/styles/actionadmin.scss";

const ReportListCVA = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);

  // Fetch API mỗi khi đổi page / size
  useEffect(() => {
    const fetchReports = async () => {
      try {
        setLoading(true);
        const res = await getReportCVAList({ page, size: pageSize });
        if (res?.response) {
          setRows(res.response);
          // 👇 Dùng totalElements từ backend để biết tổng record
          if (res.totalElements !== undefined) setRowCount(res.totalElements);
        }
      } catch (err) {
        console.error("Lỗi khi lấy danh sách báo cáo CVA:", err);
      } finally {
        setLoading(false);
      }
    };
    fetchReports();
  }, [page, pageSize]);

  const columns = [
    { field: "id", headerName: "ID", width: 80 },
    { field: "sellerName", headerName: "Seller Name", flex: 1 },
    { field: "projectName", headerName: "Project Name", flex: 1 },
    { field: "period", headerName: "Period", flex: 1 },
    { field: "totalEnergy", headerName: "Total Energy", flex: 1 },
    { field: "totalCo2", headerName: "Total CO₂", flex: 1 },
    { field: "vehicleCount", headerName: "Vehicle Count", flex: 1 },
    {
      field: "status",
      headerName: "Status",
      flex: 1,
      renderCell: ({ row: { status } }) => {
        const statusColorMap = {
          Pending: colors.blueAccent[500],
          Approved: colors.greenAccent[500],
          Rejected: colors.redAccent[500],
        };
        return (
          <Typography
            color={statusColorMap[status] || colors.grey[100]}
            fontWeight="600"
            sx={{ textTransform: "capitalize" }}
          >
            {status}
          </Typography>
        );
      },
    },
    {
      field: "submittedAt",
      headerName: "Submitted At",
      flex: 1,
      valueGetter: (params) =>
        params?.value ? new Date(params.value).toLocaleString() : "-",
    },
    {
      field: "action",
      headerName: "Action",
      flex: 1,
      renderCell: (params) => (
        <div className="cellAction">
          <Link
            to={`/cva/view_report/${params.row.id}`}
            style={{ textDecoration: "none" }}
          >
            <div className="viewButton">View</div>
          </Link>
        </div>
      ),
    },
  ];

  return (
    <Box m="20px" className="actionadmin">
      <Header title="CVA REPORTS" subtitle="Managing CVA Reports" />
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
        <DataGrid
          rows={rows}
          columns={columns}
          getRowId={(r) => r.id}
          loading={loading}
          pagination
          paginationMode="server"
          page={page}
          pageSize={pageSize}
          rowCount={rowCount}
          onPageChange={(newPage) => setPage(newPage)}
          onPageSizeChange={(newSize) => {
            setPageSize(newSize);
            setPage(0); // quay lại trang đầu khi đổi size
          }}
          rowsPerPageOptions={[10, 20, 50]}
        />
      </Box>
    </Box>
  );
};

export default ReportListCVA;
