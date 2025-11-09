import { useState, useEffect } from "react";
import { Box,Typography } from "@mui/material";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { useTheme } from "@mui/material";
import { Link } from "react-router-dom";
import "@/styles/actionadmin.scss";
import { getProjectApplications } from "@/apiAdmin/companyAdmin.js";
import AdminDataGrid from "@/components/DataGrid/AdminDataGrid.jsx";
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
    { field: "projectId", headerName: "Project ID", flex: 0.5 },
    {
      field: "projectTitle",
      headerName: "Project Title",
      flex: 1.2,
      renderCell: (params) => (
        <Typography
          variant="body2"
          sx={{
            display: "flex",
            alignItems: "center", // căn giữa theo chiều dọc
            height: "100%",
            whiteSpace: "normal",
            wordWrap: "break-word",
            lineHeight: 1.4,
            textAlign: "left",
          }}
        >
          {params.value || "N/A"}
        </Typography>
      ),
    },
    { field: "companyId", headerName: "Company ID", flex: 0.6 },
    { field: "companyName", headerName: "Company Name", flex: 1 },
    {
      field: "status",
      headerName: "Status",
      flex: 1.2,
      renderCell: (params) => {
        const value = params?.row?.status || "unknown";
        const colorMap = {
          CVA_APPROVED: colors.blueAccent[500],
          ADMIN_APPROVED: colors.greenAccent[500],
          ADMIN_REJECTED: colors.redAccent[500],
          CVA_REJECTED: "#FFB300",
          UNDER_REVIEW: colors.grey[500],
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
      flex: 1.2,
      renderCell: (params) => {
        const date = params?.value ? new Date(params.value) : null;
        if (!date) return "N/A";

        const day = String(date.getDate()).padStart(2, "0");
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const year = date.getFullYear();

        // giữ nguyên phần giờ phút giây mặc định
        const time = date.toLocaleTimeString();

        return `${day}/${month}/${year}, ${time}`;
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
    <Box m="20px" sx={{ marginLeft: "290px" }} className="actionadmin">
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
        <AdminDataGrid rows={data} columns={columns} getRowId={(r) => r.id} />
      </Box>
    </Box>
  );
};

export default ApplicationList;
