// src/scenes/admin/EVList.jsx
import { Box, Typography, useTheme, CircularProgress, Snackbar, Alert } from "@mui/material";
import { DataGrid } from "@mui/x-data-grid";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { Link } from "react-router-dom";
import { useState, useEffect } from "react";
import "@/styles/actionadmin.scss";
import { getVehicles } from "@/apiAdmin/EVAdmin.js";
import AdminDataGrid from "@/components/DataGrid/AdminDataGrid.jsx";

const EVList = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  // states
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [pageNo, setPageNo] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalCount, setTotalCount] = useState(0);
  const [error, setError] = useState("");

  // Fetch data
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        setError("");

        const res = await getVehicles(pageNo, pageSize);

        //  đọc đúng key `data` từ API
        const items = res?.data || [];

        setData(
          items.map((v, index) => ({
            ...v,
            id: v.id ?? `${pageNo}-${index}`, // fallback id
          }))
        );

        // Tổng số rows = totalPages * pageSize nếu backend trả totalPages
        const totalRows = res?.totalPages > 0 ? res.totalPages * res.pageSize : items.length;
        setTotalCount(totalRows);

      } catch (err) {
        console.error(" Failed to fetch vehicles:", err);
        setError("Failed to load vehicles");
        setData([]);
        setTotalCount(0);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [pageNo, pageSize]);

  // table columns
  const columns = [
    { field: "id", headerName: "", width: 90 },
    { field: "plateNumber", headerName: "Number Plate", flex: 1 },
    { field: "brand", headerName: "Brand", flex: 1 },
    { field: "model", headerName: "Model", flex: 1 },
    { field: "companyId", headerName: "Company ID", flex: 1 },
    {
      field: "action",
      headerName: "Action",
      flex: 1,
      sortable: false,
      renderCell: (params) => (
        <div className="cellAction">
          <Link
            to={`/admin/view_EV/${params.row.id}`}
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
      <Header title="ELECTRIC VEHICLES" subtitle="List of electric vehicles" />

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
        }}
      >
        {loading ? (
          <Box
            height="100%"
            display="flex"
            alignItems="center"
            justifyContent="center"
          >
            <CircularProgress />
          </Box>
        ) : (
          <AdminDataGrid rows={data} columns={columns} getRowId={(r) => r.id} />
        )}
      </Box>

      {/* Snackbar báo lỗi */}
      <Snackbar
        open={!!error}
        autoHideDuration={4000}
        onClose={() => setError("")}
        anchorOrigin={{ vertical: "top", horizontal: "center" }}
      >
        <Alert severity="error" variant="filled" onClose={() => setError("")}>
          {error}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default EVList;
