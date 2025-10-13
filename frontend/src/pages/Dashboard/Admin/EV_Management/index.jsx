import { Box, Typography, useTheme } from "@mui/material";
import { DataGrid } from "@mui/x-data-grid";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { Link } from "react-router-dom";
import { useState } from "react";
import "@/styles/actionadmin.scss"; // dùng style đã copy từ template
import { mockDataEV} from "@/data/mockData";
const Invoices = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  // lưu dữ liệu (để có thể xóa hàng)
  const [data, setData] = useState(mockDataEV);
  
  const columns = [
    { field: "id", headerName: "" },
    { field: "evid", headerName: "EV ID", flex: 1 },

    {
      field: "numberplate",
      headerName: "Number Plate",
      flex: 1,
    },
    {
      field: "vehiclebrand",
      headerName: "Vehicle Brand",
      flex: 1,
    },
    {
      field: "vehiclemodel",
      headerName: "Vehicle Model",
      flex: 1,
    },
    {
      field: "yearofmanufacture",
      headerName: " Year of manufacture",
      flex: 1,

    },
    {
      field: "aggregator",
      headerName: "Company",
      flex: 1,
      cellClassName: "name-column--cell",
    },
    {
      field: "status",
      headerName: "Status",
      flex: 1,

    },
    {
      field: "action",
      headerName: "Action",
      flex: 1,
      renderCell: (params) => {
        return (
          <div className="cellAction">
            <Link to={`/admin/view_EV/${params.row.id}`} style={{ textDecoration: "none" }}>
              <div className="viewButton">View</div>
            </Link>
            
          </div>
        );
      },
    },

  ];

  return (
    <Box m="20px" className="actionadmin">
      <Header title="ELECTRIC VEHICLES" subtitle="List of electric vehicles" />
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
        <DataGrid checkboxSelection rows={data} columns={columns} />
      </Box>
    </Box>
  );
};

export default Invoices;
