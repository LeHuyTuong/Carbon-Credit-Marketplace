import { Box, Typography } from "@mui/material";
import { DataGrid, GridToolbar } from "@mui/x-data-grid";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { useTheme } from "@mui/material";
import { Link } from "react-router-dom";
import { useState, useEffect } from "react";
import "@/styles/actionadmin.scss"; // style cũ
import { getCredits } from "@/apiAdmin/creditAdmin.js"; // API lấy ra credit 
import AdminDataGrid from "@/components/DataGrid/AdminDataGrid.jsx";

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
          company: c.companyName,
          projectname: c.projectTitle,
          numbercredit: c.creditsCount,
          estimatedvalue: c.totalTco2e,
          issuedday: (() => {
            if (!c.issuedAt) return "N/A";
            const date = new Date(c.issuedAt);
            const day = String(date.getDate()).padStart(2, "0");
            const month = String(date.getMonth() + 1).padStart(2, "0");
            const year = date.getFullYear();
            const time = date.toLocaleTimeString();
            return `${day}/${month}/${year}, ${time}`;
          })(),
          status: c.status,
          expiredday: c.vintageYear,
          serial: `${c.serialFrom}-${c.serialTo}`,
          creditcertificateurl: c.certificateUrl,
        }));
        setData(mapped);
      } catch (err) {
        console.error("Failed to fetch credits:", err);
      }
    };
    fetchData();
  }, []);

  const columns = [
    {
      field: "creditid",
      headerName: "Credit ID",
      flex: 1.3,
      renderCell: (params) => (
        <Box
          sx={{
            display: "flex",
            alignItems: "center",     // canh giữa theo chiều dọc
            justifyContent: "center", // canh giữa theo chiều ngang
            height: "100%",           // để căn giữa hoạt động đúng
            textAlign: "left",
            whiteSpace: "normal",     // cho phép xuống dòng
            wordBreak: "break-word",  // tự ngắt từ
            lineHeight: 1.3,
            p: 0.5,
          }}
        >
          {params.value}
        </Box>
      ),
    },
    {
      field: "company",
      headerName: "Company",
      flex: 1,
      cellClassName: "name-column--cell",
    },
    {
      field: "projectname",
      headerName: "Project Name",
      flex: 1.3,
      renderCell: (params) => (
        <Box
          sx={{
            display: "flex",
            alignItems: "center",     // canh giữa theo chiều dọc
            justifyContent: "center", // canh giữa theo chiều ngang
            height: "100%",           // để căn giữa hoạt động đúng
            textAlign: "left",
            whiteSpace: "normal",     // cho phép xuống dòng
            wordBreak: "break-word",  // tự ngắt từ
            lineHeight: 1.3,
            p: 0.5,
          }}
        >
          {params.value}
        </Box>
      ),
    },
    {
      field: "numbercredit",
      headerName: "Number of Credits",
      type: "number",
      align: "left",
      headerAlign: "left",
      flex: 1,
    },
    { field: "estimatedvalue", headerName: "Estimated value", flex: 1 },
    { field: "issuedday", headerName: "Issued Day", flex: 1.5 },
    {
      field: "status",
      headerName: "Status",
      flex: 1,
      renderCell: (params) => {
        const value = params?.row?.status || "Unknown";
        const lower = value.toLowerCase();

        // mapping màu (tùy chỉnh theo trạng thái)
        const colorMap = {
          issued: "#2E7D32", // xanh lá
          pending: "#F9A825", // vàng
          rejected: "#C62828", // đỏ
          default: "#9E9E9E", // xám
        };

        const color = colorMap[lower] || colorMap.default;

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
              sx={{
                color,
                fontWeight: 600,
                textTransform: "capitalize",
                px: 1.5,
                py: 0.5,
                borderRadius: "8px",
                fontSize: "0.95rem",
                minWidth: "90px",
                textAlign: "left",
                pl: -5, // đẩy nội dung sang trái
              }}
            >
              {value}
            </Typography>
          </Box>
        );
      },
    },
    { field: "expiredday", headerName: "Expired Year", flex: 1 },
    { field: "serial", headerName: "Serial", flex: 1 },
    {
      field: "creditcertificateurl",
      headerName: "Certificate",
      flex: 1,
      renderCell: (params) => {
        const url = params.value;
        if (!url) return <Typography color="text.secondary">No certificate</Typography>;

        return (
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              justifyContent: "flex-end",
              height: "100%",
              width: "100%",
              pl: 2,
            }}
          >
            <a
              href={url}
              target="_blank"
              rel="noopener noreferrer"
              style={{
                textDecoration: "none",
                color: colors.blueAccent[400],
                fontWeight: 600,
                borderRadius: "6px",
                padding: "4px 10px",
                transition: "all 0.2s ease",
              }}
            >
              Link Certificate
            </a>
          </Box>
        );
      },
    },
    ,
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
        <AdminDataGrid rows={data} columns={columns} getRowId={(row) => row.id} />
      </Box>
    </Box>
  );
};

export default CreditsList;
