import { Box, Typography, useTheme } from "@mui/material";
import { DataGrid } from "@mui/x-data-grid";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";

const Invoices = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const columns = [
    { field: "id", headerName: "" },
    { field: "projectid", headerName: "Project ID", flex: 1 },
    {
      field: "projectname",
      headerName: "Project Name",
      flex: 1.5,
      cellClassName: "name-column--cell",
      renderCell: (params) => (
        <Typography
          sx={{
            whiteSpace: "normal",
            wordWrap: "break-word",
            lineHeight: 1.4,
          }}
        >
          {params.value}
        </Typography>
      ),
    },
    {
      field: "shortdescription",
      headerName: "Short Description",
      flex: 1.5,
      renderCell: (params) => (
        <Typography
          sx={{
            whiteSpace: "normal",
            wordWrap: "break-word",
            lineHeight: 1.4,
          }}
        >
          {params.value}
        </Typography>
      ),
    },
    {
      field: "starteddate", 
      headerName: "Started Date",
      flex: 1,
    },
    {
      field: "totalexpectedcredits",
      headerName: "Total Expected Credits",
      flex: 1,
    },
    
    {
      field: "status",
      headerName: "Status",
      flex: 1,
      renderCell: ({ row: { status } }) => {
        const statusColorMap = {
          Coming_Soon: colors.grey[500],
          Is_Open: colors.greenAccent[500],
          
        };
        return (
          <Box
            display="flex"
            alignItems="center"
            justifyContent="left"
            height="100%"
          >
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
    
  ];

  return (
    <Box m="20px">
      <Header title="PROJECTS" subtitle="List of projects" />
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
        <DataGrid checkboxSelection rows={mockDataProjects} columns={columns} />
      </Box>
    </Box>
  );
};

export default Invoices;
