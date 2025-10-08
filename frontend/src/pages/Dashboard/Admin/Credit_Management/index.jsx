import { Box } from "@mui/material";
import { DataGrid, GridToolbar } from "@mui/x-data-grid";
import { tokens } from "@/theme";
import { mockDataCredits } from "@/data/mockData";
import Header from "@/components/Chart/Header.jsx";
import { useTheme } from "@mui/material";

const Contacts = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  const columns = [
    { field: "id", headerName: "", },
    { field: "creditid", headerName: "Credit ID", },
    {
      field: "aggregator",
      headerName: "Aggregator",
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
      headerName: "Estimated value",
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
          active: "#4CAF50",   // xanh lá
          revoked: "#E53935",  // đỏ
          pending: "#42A5F5",     // xanh dương
          sold: "#FFB300",  // vàng cam
          listed: "#FDD835",   // vàng nhạt
          retire: "#757575",   // xám
        };

        const color = colorMap[value.toLowerCase()] || "#E0E0E0";

        return (
          <div
            style={{
              width: "100%",
              height: "100%",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
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
  ];

  return (
    <Box m="20px">
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
        }}
      >
        <DataGrid
          rows={mockDataCredits}
          columns={columns}
          components={{ Toolbar: GridToolbar }}
        />
      </Box>
    </Box>
  );
};

export default Contacts;
