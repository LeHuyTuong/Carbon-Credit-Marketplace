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
        // ✅ Giả lập dữ liệu mẫu (mock data)
        const mockApplications = [
          {
            id: 1,
            projectId: 101,
            projectTitle: "AI-Powered Chatbot",
            companyId: 201,
            companyName: "TechNova Co., Ltd.",
            status: "SUBMITTED",
            reviewNote: "Awaiting first review.",
            finalReviewNote: "",
            applicationDocsUrl: "https://example.com/docs/ai-chatbot.pdf",
            submittedAt: "2025-10-17T18:50:29.847Z",
          },
          {
            id: 2,
            projectId: 102,
            projectTitle: "Smart Farming Drone",
            companyId: 202,
            companyName: "AgriTech Vietnam",
            status: "REVIEWING",
            reviewNote: "Under review by committee.",
            finalReviewNote: "",
            applicationDocsUrl: "",
            submittedAt: "2025-10-16T14:32:12.000Z",
          },
          {
            id: 3,
            projectId: 103,
            projectTitle: "Blockchain Logistics System",
            companyId: 203,
            companyName: "LogiChain Solutions",
            status: "APPROVED",
            reviewNote: "Approved with minor revisions.",
            finalReviewNote: "Project approved officially.",
            applicationDocsUrl: "https://example.com/docs/logichain.pdf",
            submittedAt: "2025-10-15T09:00:00.000Z",
          },
          {
            id: 4,
            projectId: 104,
            projectTitle: "Renewable Energy Management",
            companyId: 204,
            companyName: "GreenFuture Energy",
            status: "REJECTED",
            reviewNote: "Insufficient technical details.",
            finalReviewNote: "Rejected due to incomplete documentation.",
            applicationDocsUrl: "",
            submittedAt: "2025-10-14T12:15:45.000Z",
          },
        ];

        // ✅ Nếu API chưa có, dùng mock data thay thế
        let applications;
        try {
          applications = await getProjectApplications();
        } catch {
          applications = mockApplications;
        }

        setData(applications || mockApplications);
      } catch (error) {
        console.error("Error fetching applications:", error);
        setData([]); // tránh crash
      } finally {
        setLoading(false);
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
            View Docs
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
            to={`/cva/view_registration_project/${params.row.id}`}
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
