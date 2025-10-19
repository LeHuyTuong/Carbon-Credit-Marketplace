import { Box, Typography, useTheme, Button } from "@mui/material";
import { DataGrid } from "@mui/x-data-grid";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { Link, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import "@/styles/actionadmin.scss";
import { apiFetch } from "@/utils/apiFetch";

const ListProjects = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();
  const [data, setData] = useState([]);

  //  Gọi API khi load trang
  useEffect(() => {
    const fetchProjects = async () => {
      try {
        const res = await apiFetch("/api/v1/projects/all", { method: "GET" });
        console.log("API Project List:", res);

        const projects = (res?.response || []).map((item, idx) => ({
          id: item.id || idx,
          projectid: item.id,
          projectname: item.title || "-",
          shortdescription: item.description || "-",
          companyname: item.companyName || "-",
          commitments: item.commitments || "-",
          measurementmethod: item.measurementMethod || "-",
          totalexpectedcredits: item.technicalIndicators || "-",
          reviewer: item.reviewer || "-",
          reviewnote: item.reviewNote || "-",
          finalreviewer: item.finalReviewer || "-",
          starteddate: item.createdAt
            ? new Date(item.createdAt).toLocaleDateString()
            : "-",
          status: item.status || "Coming_Soon",
        }));

        setData(projects);
      } catch (error) {
        console.error(" Failed to fetch projects:", error);
      }
    };

    fetchProjects();
  }, []);


  //  Cấu hình cột hiển thị DataGrid
  const columns = [
    { field: "id", headerName: "#", width: 70 },
    { field: "projectid", headerName: "Project ID", flex: 1 },
    {
      field: "projectname",
      headerName: "Project Name",
      flex: 1.5,
      renderCell: (params) => (
        <Typography sx={{ whiteSpace: "normal", wordWrap: "break-word", lineHeight: 1.4 }}>
          {params.value}
        </Typography>
      ),
    },
    {
      field: "shortdescription",
      headerName: "Description",
      flex: 1.5,
      renderCell: (params) => (
        <Typography sx={{ whiteSpace: "normal", wordWrap: "break-word", lineHeight: 1.4 }}>
          {params.value}
        </Typography>
      ),
    },
    {
      field: "companyname",
      headerName: "Company",
      flex: 1,
      renderCell: (params) => <Typography>{params.value}</Typography>,
    },
    {
      field: "commitments",
      headerName: "Commitments",
      flex: 1,
      renderCell: (params) => <Typography>{params.value}</Typography>,
    },
    {
      field: "measurementmethod",
      headerName: "Measurement Method",
      flex: 1,
      renderCell: (params) => <Typography>{params.value}</Typography>,
    },
    {
      field: "totalexpectedcredits",
      headerName: "Technical Indicators",
      flex: 1,

      renderCell: (params) => <Typography>{params.value}</Typography>,
    },
    {
      field: "reviewer",
      headerName: "Reviewer",
      flex: 1,
      renderCell: (params) => <Typography>{params.value}</Typography>,
    },
    {
      field: "finalreviewer",
      headerName: "Final Reviewer",
      flex: 1,
      renderCell: (params) => <Typography>{params.value}</Typography>,
    },
    {
      field: "starteddate",
      headerName: "Created Date",
      flex: 1,
    },
    {
      field: "status",
      headerName: "Status",
      flex: 1,
      renderCell: ({ row: { status } }) => {
        const statusColorMap = {
          SUBMITTED: colors.blueAccent[400],
          APPROVED: colors.greenAccent[500],
          REJECTED: colors.redAccent[500],
          Coming_Soon: colors.grey[500],
          Is_Open: colors.greenAccent[500],
          Ended: colors.redAccent[500],
        };
        return (
          <Box display="flex" alignItems="center" justifyContent="left" height="100%">
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
          <Link
            to={`/admin/view_project/${params.row.projectid}`}
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
      <Box display="flex" justifyContent="space-between" alignItems="center">
        <Header title="PROJECTS" subtitle="List of all registered projects" />
        <Button
          variant="contained"
          color="success"
          onClick={() => navigate("/admin/new_project")}
          sx={{
            height: "40px",
            borderRadius: "8px",
            textTransform: "none",
            fontWeight: 600,
          }}
        >
          + Add New
        </Button>
      </Box>

      <Box
        m="40px 0 0 0"
        height="75vh"
        sx={{
          "& .MuiDataGrid-root": { border: "none" },
          "& .MuiDataGrid-cell": { borderBottom: "none" },
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
        <DataGrid checkboxSelection rows={data} columns={columns} />
      </Box>
    </Box>
  );
};

export default ListProjects;