import { Box } from "@mui/material";
import { DataGrid, GridToolbar } from "@mui/x-data-grid";
import { tokens } from "@/themeCVA";
import Header from "@/components/Chart/Header.jsx";
import { useTheme } from "@mui/material";
import { Link } from "react-router-dom";
import { useState } from "react"; //thêm để quản lý dữ liệu
import "@/styles/actionadmin.scss"; // dùng style đã copy từ template
import { mockDataCreditsCVA} from "@/data/mockData";
const Contacts = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);


  const [data, setData] = useState(mockDataCreditsCVA);
  
  const columns = [
    { field: "id", headerName: "", flex: 0.5 },
    { field: "creditid", headerName: "Credit ID", },
    {
      field: "aggregator",
      headerName: "Company",
      flex: 1,
      cellClassName: "name-column--cell",
    },

    {
      field: "projectname",
      headerName: "Project Name",
      flex: 1,

    },
    {
      field: "numbercredit",
      headerName: "Number of Credits",
      type: "number",
      align: "left",
      headerAlign: "left",
      flex: 1,
    },
    {
      field: "estimatedvalue",
      headerName: "Value",
      flex: 1,
    },
    {
      field: "issuedday",
      headerName: "Issued Day",
      flex: 1,
    },
    {
      field: "status",
      headerName: "Status",
      flex: 1,
      renderCell: (params) => {
        const value = params?.row?.status || "unknown";

        // Map màu cho từng trạng thái
        const colorMap = {
          approved: "#4CAF50",   // xanh lá
          rejected: "#E53935",  // đỏ
          pending: "#42A5F5",     // xanh dương
          
        };

        const color = colorMap[value.toLowerCase()] || "#E0E0E0";

        return (
          <div
            style={{
              width: "100%",
              height: "100%",
              display: "flex",
              alignItems: "center",
              justifyContent: "left",
            }}
          >
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
          </div>
        );
      },
    },
    {
      field: "expiredday",
      headerName: "Expired Day",
      flex: 1,
    },
    { field: "linkedcertificate", headerName: "Linked Certificate", flex: 1 },
    {
      field: "action",
      headerName: "Action",
      flex: 1,
      renderCell: (params) => {
        return (
          <div className="cellAction">
            <Link to={`/cva/view_credit/${params.row.id}`} style={{ textDecoration: "none" }}>
              <div className="viewButton">View</div>
            </Link>
            
          </div>
        );
      },
    },
  ];

  return (
    <Box m="20px" className="actionadmin">
      <Header
        title="CREDITS"
        subtitle="List of Carbon Credits in the System"

      />
      <Box
        m="40px 0 0 0"
        height="75vh"
        sx={{
          "& .MuiDataGrid-root": {
            border: "none",
          },
          "& .MuiDataGrid-cell": {
            borderBottom: "none",
          },
          "& .name-column--cell": {
            color: colors.greenAccent[300],
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
          "& .MuiCheckbox-root": {
            color: `${colors.greenAccent[200]} !important`,
          },
          "& .MuiDataGrid-toolbarContainer .MuiButton-text": {
            color: `${colors.grey[100]} !important`,
          },
          "& .MuiTablePagination-root": {
            display: "flex",
            alignItems: "center",
            justifyContent: "flex-end",
          },
          "& .MuiTablePagination-selectLabel, & .MuiTablePagination-displayedRows": {
            marginTop: 0,
            marginBottom: 0,
            lineHeight: "normal",
          },
          "& .MuiTablePagination-select": {
            marginTop: "0 !important",
            marginBottom: "0 !important",
            paddingTop: "0 !important",
            paddingBottom: "0 !important",
          },
        }}
      >
        <DataGrid
          rows={data}
          columns={columns}
          components={{ Toolbar: GridToolbar }}
        />
      </Box>
    </Box>
  );
};

export default Contacts;
