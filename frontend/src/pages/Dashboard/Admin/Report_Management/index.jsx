import { Box, Typography, useTheme, CircularProgress } from "@mui/material";
import { DataGrid } from "@mui/x-data-grid";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { Link } from "react-router-dom";
import { useState, useEffect } from "react";
import "@/styles/actionadmin.scss";
import { getAllReportsAdmin } from "@/apiAdmin/reportAdmin.js";
import AdminDataGrid from "@/components/DataGrid/AdminDataGrid.jsx";
 const ReportsList = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  // State
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);

  // Fetch API
  useEffect(() => {
    const fetchReports = async () => {
      try {
        const res = await getAllReportsAdmin({ page: 0, size: 20 });
        if (res?.response) {
          // API trả về res.response là mảng report
          const formatted = res.response.map((item) => ({
            id: item.id,
            sellerName: item.sellerName,
            projectName: item.projectName,
            period: item.period,
            totalEnergy: item.totalEnergy,
            totalCo2: item.totalCo2,
            vehicleCount: item.vehicleCount,
            status: item.status,
            submittedAt: item.submittedAt,
          }));
          setData(formatted);
        }
      } catch (error) {
        console.error("Failed to fetch reports:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchReports();
  }, []);

  // Cột hiển thị đúng field của BE
  const columns = [
    { field: "id", headerName: "ID", flex: 0.5 },
    { field: "sellerName", headerName: "Company", flex: 1 },
    { field: "projectName", headerName: "Project", flex: 1 },
    { field: "period", headerName: "Reporting Period", flex: 1 },
    { field: "vehicleCount", headerName: "Vehicle Count", flex: 0.8 },
    { field: "totalEnergy", headerName: "Total Energy", flex: 1 },
    { field: "totalCo2", headerName: "Total CO₂", flex: 1 },
    {
      field: "submittedAt",
      headerName: "Submitted At",
      flex: 1,
      renderCell: ({ value }) =>
        value ? new Date(value).toLocaleString() : "—",
    },
    {
      field: "status",
      headerName: "Status",
      flex: 1,
      renderCell: ({ row: { status } }) => {
        const statusColorMap = {
          SUBMITTED: colors.blueAccent[500],
          APPROVED: colors.greenAccent[500],
          REJECTED: colors.redAccent[500],
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
            color={statusColorMap[status] || colors.grey[100]}
            fontWeight="600"
            sx={{ textTransform: "capitalize", lineHeight: 1 }}
          >
            {status || "—"}
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
          <Link
            to={`/admin/view_report/${params.row.id}`}
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
      <Header title="REPORTS" subtitle="List of all submitted reports" />
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
        {loading ? (
          <Box
            display="flex"
            alignItems="center"
            justifyContent="center"
            height="100%"
          >
            <CircularProgress />
          </Box>
        ) : (
          <AdminDataGrid rows={data} columns={columns} getRowId={(r) => r.id} />
        )}
      </Box>
    </Box>
  );
};

export default ReportsList;
