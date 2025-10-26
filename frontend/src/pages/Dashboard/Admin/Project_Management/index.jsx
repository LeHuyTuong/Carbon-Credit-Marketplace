import { Box, Typography, useTheme, Button } from "@mui/material";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { Link, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import "@/styles/actionadmin.scss";
import { apiFetch } from "@/utils/apiFetch";
import AdminDataGrid from "@/components/DataGrid/AdminDataGrid.jsx";

const ListProjects = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();
  const [data, setData] = useState([]);

  // Gọi API khi load trang
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
          commitments: item.commitments || "-",
          measurementmethod: item.measurementMethod || "-",
          totalexpectedcredits: item.technicalIndicators || "-",
          status: item.status || "Coming_Soon",
        }));

        setData(projects);
      } catch (error) {
        console.error("Failed to fetch projects:", error);
      }
    };

    fetchProjects();
  }, []);

  // ======= Cấu hình cột hiển thị DataGrid =======
  const textCellStyle = {
    whiteSpace: "normal",
    wordWrap: "break-word",
    overflowWrap: "break-word",
    lineHeight: 1.4,
    fontSize: 14,
  };

  const columns = [
    { field: "id", headerName: "#", width: 70 },
    { field: "projectid", headerName: "Project ID", flex: 1 },
    {
      field: "projectname",
      headerName: "Project Name",
      flex: 1.2,
      renderCell: (params) => <Typography sx={textCellStyle}>{params.value}</Typography>,
    },
    {
      field: "shortdescription",
      headerName: "Description",
      flex: 1,
      renderCell: (params) => <Typography sx={textCellStyle}>{params.value}</Typography>,
    },
    {
      field: "commitments",
      headerName: "Commitments",
      flex: 1.2,
      renderCell: (params) => <Typography sx={textCellStyle}>{params.value}</Typography>,
    },
    {
      field: "measurementmethod",
      headerName: "Measurement Method",
      flex: 1.2,
      renderCell: (params) => <Typography sx={textCellStyle}>{params.value}</Typography>,
    },
    {
      field: "totalexpectedcredits",
      headerName: "Technical Indicators",
      flex: 1.2,
      renderCell: (params) => <Typography sx={textCellStyle}>{params.value}</Typography>,
    },
    {
      field: "status",
      headerName: "Status",
      flex: 0.5,
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
          <Box display="flex" alignItems="center" height="100%">
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
      flex: 0.5,
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

  // ======= Render Component =======
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
        sx={{
          "& .MuiDataGrid-root": {
            border: "none",
            overflowX: "hidden !important", //  Ẩn scroll ngang
          },
          "& .MuiDataGrid-cell": {
            borderBottom: "none",
            display: "flex",
            alignItems: "flex-start !important", //  Căn text lên trên
            whiteSpace: "normal",
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
        }}
      >
        <AdminDataGrid
          rows={data}
          columns={columns}
          getRowHeight={() => "auto"} //  Auto height
          autoHeight //  Tự giãn toàn bảng
          getRowId={(r) => r.id}
        />
      </Box>
    </Box>
  );
};

export default ListProjects;
