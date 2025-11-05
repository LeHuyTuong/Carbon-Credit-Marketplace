import { Box } from "@mui/material";
import { DataGrid, GridToolbar } from "@mui/x-data-grid";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { useTheme } from "@mui/material";
import { Link } from "react-router-dom";
import { useState, useEffect } from "react";
import "@/styles/actionadmin.scss"; // style cũ
import { getCredits } from "@/apiAdmin/creditAdmin.js"; // API mới

const CreditsList = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  const [data, setData] = useState([]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const res = await getCredits(0, 20); // fetch 20 bản ghi
        // Lấy array response.content
        const credits = res.response?.content || [];
        // Map thành format DataGrid rows
        const mapped = credits.map((c) => ({
          id: c.id,
          creditid: c.batchCode,
          aggregator: c.companyName,
          projectname: c.projectTitle,
          numbercredit: c.creditsCount,
          estimatedvalue: c.totalTco2e,
          issuedday: new Date(c.issuedAt).toLocaleDateString(),
          status: c.status,
          expiredday: c.vintageYear,
          serial: `${c.serialFrom}-${c.serialTo}`,
        }));
        setData(mapped);
      } catch (err) {
        console.error("Failed to fetch credits:", err);
      }
    };
    fetchData();
  }, []);

  const columns = [
    { field: "id", headerName: "", flex: 0.5 },
    { field: "creditid", headerName: "Credit ID" },
    {
      field: "aggregator",
      headerName: "Company",
      flex: 1,
      cellClassName: "name-column--cell",
    },
    { field: "projectname", headerName: "Project Name", flex: 1 },
    {
      field: "numbercredit",
      headerName: "Number of Credits",
      type: "number",
      align: "left",
      headerAlign: "left",
      flex: 1,
    },
    { field: "estimatedvalue", headerName: "Estimated value", flex: 1 },
    { field: "issuedday", headerName: "Issued Day", flex: 1 },
    {
      field: "status",
      headerName: "Status",
      flex: 1,
      renderCell: (params) => {
        const value = params?.row?.status || "unknown";
        const lower = value.toLowerCase();

        // Mặc định ISSUED là xanh lá, các trạng thái khác fallback sang xám
        const color = lower === "issued" ? "#2E7D32" : "#9E9E9E";
        

        return (
          <div
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "flex-start",
              width: "100%",
            }}
          >
            <span
              style={{
                color,
                fontWeight: 600,
                textTransform: "capitalize",
                padding: "4px 12px",
                borderRadius: "8px",
                fontSize: "0.95rem",
                minWidth: "90px",
                textAlign: "center",
              }}
            >
              {value}
            </span>
          </div>
        );
      },
    },

    { field: "expiredday", headerName: "Expired Year", flex: 1 },
    { field: "serial", headerName: "Serial", flex: 1 },
    { field: "creditcertificateurl", headerName: "Link Certificate", flex: 1 },
    {
      field: "action",
      headerName: "Action",
      flex: 1,
      renderCell: (params) => {
        return (
          <div className="cellAction">
            <Link
              to={`/admin/view_credit/${params.row.id}`}
              style={{ textDecoration: "none" }}
            >
              <div className="viewButton">View</div>
            </Link>
          </div>
        );
      },
    },
  ];

  return (
    <Box m="20px" className="actionadmin">
      <Header title="CREDITS" subtitle="List of Carbon Credits in the System" />
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
          "& .MuiDataGrid-virtualScroller": { backgroundColor: colors.primary[400] },
          "& .MuiDataGrid-footerContainer": {
            borderTop: "none",
            backgroundColor: colors.blueAccent[700],
          },
          "& .MuiCheckbox-root": { color: `${colors.greenAccent[200]} !important` },
          "& .MuiDataGrid-toolbarContainer .MuiButton-text": {
            color: `${colors.grey[100]} !important`,
          },
        }}
      >
        <DataGrid
          checkboxSelection
          rows={data}
          columns={columns}
          getRowId={(row) => row.id}
          pagination
          initialState={{
            pagination: { paginationModel: { pageSize: 10, page: 0 } },
          }}
          pageSizeOptions={[10, 20, 50]}
          sx={{
            "& .MuiDataGrid-footerContainer": {
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              height: "56px", // chuẩn chiều cao theo Material Design
              padding: "0 16px",
            },
            "& .MuiTablePagination-displayedRows, & .MuiTablePagination-selectLabel": {
              margin: 0,
              lineHeight: "1.5rem",
            },
            "& .MuiTablePagination-actions": {
              marginRight: "4px",
            },
          }}
        />
      </Box>
    </Box>
  );
};

export default CreditsList;
