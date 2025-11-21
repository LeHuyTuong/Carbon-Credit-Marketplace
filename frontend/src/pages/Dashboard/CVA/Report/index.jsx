import { useEffect, useState } from "react";
import { Box, Typography, useTheme } from "@mui/material";
import { tokens } from "@/themeCVA";
import Header from "@/components/Chart/Header.jsx";
import { Link } from "react-router-dom";
import { getReportCVAList } from "@/apiCVA/reportCVA.js";
import "@/styles/actionadmin.scss";
import CVADataGrid from "@/components/DataGrid/CVADataGrid.jsx";

const ReportListCVA = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  // DataGrid state
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  // Total rows from server
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);

  // Fetch API mỗi khi đổi page / size
  useEffect(() => {
    const fetchReports = async () => {
      try {
        setLoading(true);
        const res = await getReportCVAList({ page, size: pageSize });
        console.log("API CVA list:", res);

        if (res?.response) {
          const list = Array.isArray(res.response)
            ? res.response
            : res.response.content || [];
          setRows(list);
          if (res.totalElements !== undefined) setRowCount(res.totalElements);
        }
      } catch (err) {
        console.error("Error when getting CVA report list:", err);
      } finally {
        setLoading(false);
      }
    };
    fetchReports();
  }, [page, pageSize]);
  // DataGrid columns
  const columns = [
    { field: "id", headerName: "Report ID", width: 80 },
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
          SUBMITTED: colors.blueAccent[500],
          CVA_APPROVED: colors.greenAccent[500],
          CVA_REJECTED: colors.redAccent[500],
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
              sx={{ textTransform: "capitalize" }}
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
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "left",
            width: "100%",
            height: "100%",
          }}
          className="cellAction">
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
    // Main container
    <Box m="20px" sx={{ marginLeft: "280px" }} className="actionadmin">
      <Header title="CVA REPORTS" subtitle="Managing CVA Reports" />
      <Box
        m="40px 0 0 0"
        height="70vh"
        sx={{
          "& .MuiDataGrid-root": { border: "none", },
          "& .MuiDataGrid-columnHeaders": {
            backgroundColor: colors.blueAccent[700],
            borderBottom: "none",
          },
          "& .MuiDataGrid-virtualScroller": {
            backgroundColor: colors.primary[400],
          },
          "& .MuiDataGrid-footerContainer": {
            borderTop: "none",
            backgroundColor: colors.greenAccent[400],
          },
          "& .MuiDataGrid-cell": {
            whiteSpace: "normal !important",
            wordWrap: "break-word !important",
            lineHeight: "1.4em",
            display: "flex",
            alignItems: "flex-start",
          },
        }}
      > 
      {/* DataGrid Component */}
        <CVADataGrid
          rows={rows}
          columns={columns}
          getRowId={(r) => r.id}
          page={page}
          onPageChange={(newPage) => setPage(newPage)}
          pageSize={pageSize}
          onPageSizeChange={(newSize) => setPageSize(newSize)}
          rowCount={rowCount}
          loading={loading}
          getRowHeight={() => "auto"}
        />
      </Box>
    </Box>
  );
};

export default ReportListCVA;
