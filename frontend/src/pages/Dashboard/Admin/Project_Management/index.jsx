import { Box, Typography, useTheme, Button, Tooltip } from "@mui/material";
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
        // Gọi API lấy danh sách project
        const res = await apiFetch("/api/v1/projects/all", { method: "GET" });
        console.log("API Project List:", res);
        // Chuyển đổi dữ liệu để phù hợp với DataGrid
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


  const columns = [
    { field: "projectid", headerName: "Project ID", flex: 0.5 },
    {
      field: "projectname",
      headerName: "Project Name",
      flex: 1.2,
      renderCell: (params) => (
        <Typography
          variant="body2"
          // sx styles
          sx={{
            display: "flex", // sử dụng flexbox
            alignItems: "center", // căn giữa theo chiều dọc nếu 1 dòng
            height: "100%", // chiếm toàn bộ chiều cao ô
            whiteSpace: "normal", // cho phép xuống dòng
            wordWrap: "break-word", // ngắt từ nếu quá dài
            lineHeight: 1.4, // chiều cao dòng
            textAlign: "left", // căn trái
          }}
        >
          {params.value}
        </Typography>
      ),
    },
    {
      field: "shortdescription",
      headerName: "Description",
      flex: 1.5,
      renderCell: (params) => (
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            height: "100%",
            width: "100%",
            overflow: "hidden",// Ẩn tràn văn bản
          }}
        >
          <Tooltip title={params.value || ""}>
            <Typography
              variant="body2"
              sx={{
                display: "-webkit-box",
                WebkitLineClamp: 2,
                WebkitBoxOrient: "vertical",
                overflow: "hidden",
                textOverflow: "ellipsis",
                whiteSpace: "normal",
                wordBreak: "break-word",
                lineHeight: 1.4,
                textAlign: "left",
                width: "100%",
              }}
            >
              {params.value}
            </Typography>
          </Tooltip>
        </Box>
      ),
    },
    {
      field: "commitments",
      headerName: "Commitments",
      flex: 1.5,


      renderCell: (params) => (
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            height: "100%",
            width: "100%",
            overflow: "hidden",
          }}
        >
          <Tooltip title={params.value || ""}>
            <Typography
              variant="body2"
              sx={{
                display: "-webkit-box",//  Hiển thị dạng box
                WebkitLineClamp: 2,//  Giới hạn 2 dòng
                WebkitBoxOrient: "vertical",//  Hướng box theo chiều dọc
                overflow: "hidden",
                textOverflow: "ellipsis",//  Hiển thị dấu ... nếu tràn
                whiteSpace: "normal",//  Cho phép xuống dòng
                wordBreak: "break-word",//  Ngắt từ nếu quá dài
                lineHeight: 1.4,//  Chiều cao dòng
                textAlign: "left",//  Căn trái
                width: "100%",//  Chiếm toàn bộ chiều rộng
              }}
            >
              {params.value}
            </Typography>
          </Tooltip>
        </Box>
      ),
    },
    {
      field: "measurementmethod",
      headerName: "Measurement Method",
      flex: 1.5,
      renderCell: (params) => (
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            height: "100%",
            width: "100%",
            overflow: "hidden",
          }}
        >
          <Tooltip title={params.value || ""}>
            <Typography
              variant="body2"
              sx={{
                display: "-webkit-box",
                WebkitLineClamp: 2,
                WebkitBoxOrient: "vertical",
                overflow: "hidden",
                textOverflow: "ellipsis",
                whiteSpace: "normal",
                wordBreak: "break-word",
                lineHeight: 1.4,
                textAlign: "left",
                width: "100%",
              }}
            >
              {params.value}
            </Typography>
          </Tooltip>
        </Box>
      ),

    },
    {
      field: "totalexpectedcredits",
      headerName: "Technical Indicators",
      flex: 1.5,
      renderCell: (params) => (
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            height: "100%",
            width: "100%",
            overflow: "hidden",
          }}
        >
          <Tooltip title={params.value || ""}>
            <Typography
              variant="body2"
              sx={{
                display: "-webkit-box",
                WebkitLineClamp: 2,
                WebkitBoxOrient: "vertical",
                overflow: "hidden",
                textOverflow: "ellipsis",
                whiteSpace: "normal",
                wordBreak: "break-word",
                lineHeight: 1.4,
                textAlign: "left",
                width: "100%",
              }}
            >
              {params.value}
            </Typography>
          </Tooltip>
        </Box>
      ),

    },
    {
      field: "status",
      headerName: "Status",
      flex: 0.5,
      //  Hiển thị trạng thái với màu sắc khác nhau
      renderCell: ({ row: { status } }) => {
        //  Map trạng thái sang màu sắc
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

  //  Render Component 
  return (
    <Box m="20px" sx={{
      marginLeft: "290px",
      marginTop: "8px",
      marginRight: "20px",
      marginBottom: "20px",
    }} className="actionadmin">
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
        height="65vh"
        sx={{
          "& .MuiDataGrid-root": {
            border: "none",
            overflowX: "hidden !important", //  Ẩn scroll ngang
          },
          "& .MuiDataGrid-cell": {
            borderBottom: "none", //  Xóa border dưới mỗi cell
            display: "flex", 
            alignItems: "flex-start !important", //  Căn text lên trên
            whiteSpace: "normal",
          },
          "& .MuiDataGrid-columnHeaders": {
            backgroundColor: colors.blueAccent[700],// Màu nền header
            borderBottom: "none",
          },
          "& .MuiDataGrid-virtualScroller": {
            backgroundColor: colors.primary[400],// Màu nền body
          },
          "& .MuiDataGrid-footerContainer": {
            borderTop: "none",
            backgroundColor: colors.blueAccent[700],// Màu nền footer
          },
        }}
      >
        {/* Data Grid hiển thị danh sách project */}
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
