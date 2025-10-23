import { useState, useEffect } from "react";
import { Box } from "@mui/material";
import { DataGrid, GridToolbar } from "@mui/x-data-grid";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { useTheme } from "@mui/material";
import { Link } from "react-router-dom";
import "@/styles/actionadmin.scss";
import { getProjectApplications } from "@/apiAdmin/companyAdmin.js";

const ApplicationList = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const applications = await getProjectApplications();
        setData(applications || []);
      } catch (error) {
        console.error("Error fetching applications:", error);
      } finally {
        setLoading(false); // đảm bảo loading dừng kể cả khi lỗi
      }
    };

    fetchData();
  }, []);

  const columns = [
    { field: "id", headerName: "ID", flex: 0.3 },
    { field: "projectId", headerName: "Project ID", flex: 0.5 },
    {
      field: "projectTitle",
      headerName: "Project Title",
      flex: 1,
      cellClassName: "name-column--cell",
    },
    { field: "companyId", headerName: "Company ID", flex: 0.6 },
    { field: "companyName", headerName: "Company Name", flex: 1 },
    {
      field: "status",
      headerName: "Status",
      flex: 0.8,
      renderCell: (params) => {
        const value = params?.row?.status || "unknown";
        const colorMap = {
          SUBMITTED: "#42A5F5",
          APPROVED: "#4CAF50",
          REJECTED: "#E53935",
          REVIEWING: "#FFB300",
        };
        const color = colorMap[value.toUpperCase()] || "#E0E0E0";

        return (
          <span
            style={{
              color,
              fontWeight: 600,
              textTransform: "capitalize",
              padding: "4px 10px",
              borderRadius: "6px",
              fontSize: "0.9rem",
            }}
          >
            {value}
          </span>
        );
      },
    },
    { field: "reviewNote", headerName: "Review Note", flex: 1 },
    { field: "finalReviewNote", headerName: "Final Review Note", flex: 1 },
    {
      field: "applicationDocsUrl",
      headerName: "Docs Link",
      flex: 1,
      renderCell: (params) =>
        params.value ? (
          <a
            href={params.value}
            target="_blank"
            rel="noopener noreferrer"
            style={{ color: "#64B5F6", textDecoration: "underline" }}
          >
            View Doc
          </a>
        ) : (
          <span style={{ color: "#aaa" }}>No file</span>
        ),
    },
    {
      field: "submittedAt",
      headerName: "Submitted At",
      flex: 0.8,
      renderCell: (params) => {
        const date = params?.value ? new Date(params.value) : null;
        return date ? date.toLocaleString() : "N/A";
      },
    },
    {
      field: "action",
      headerName: "Action",
      flex: 0.6,
      renderCell: (params) => (
        <div className="cellAction">
          <Link
            to={`/admin/view_company/${params.row.id || params.row.applicationId}`}
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
      <Header
        title="APPLICATIONS"
        subtitle="List of Submitted Project Applications"
      />
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
          "& .MuiDataGrid-toolbarContainer .MuiButton-text": {
            color: `${colors.grey[100]} !important`,
          },
        }}
      >
        <DataGrid
          rows={data}
          columns={columns}
          slots={{ toolbar: GridToolbar }}
          getRowId={(row) => row.id}
          loading={loading}
        />
      </Box>
    </Box>
  );
};

export default ApplicationList;
