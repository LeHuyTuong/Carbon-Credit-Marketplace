import { Box, Typography, useTheme } from "@mui/material";
import { DataGrid } from "@mui/x-data-grid";
import { tokens } from "@/theme";
import { mockDataEV } from "@/data/mockData";
import Header from "@/components/Chart/Header.jsx";

const Invoices = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
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
      headerName: "Aggregator",
      flex: 1,
      cellClassName: "name-column--cell",
    },
    
  ];

  return (
    <Box m="20px">
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
        }}
      >
        <DataGrid checkboxSelection rows={mockDataEV} columns={columns} />
      </Box>
    </Box>
  );
};

export default Invoices;
