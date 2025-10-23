import { useState, useEffect } from "react";
import { Box, Typography } from "@mui/material";
import { DataGrid, GridToolbar } from "@mui/x-data-grid";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { useTheme } from "@mui/material";
import { Link } from "react-router-dom";
import "@/styles/actionadmin.scss";
import { getProjectApplications } from "@/apiCVA/registrationCVA.js";

const ApplicationList = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        console.log("📡 Fetching pending CVA applications...");
        const response = await getProjectApplications();

        console.log("✅ Raw API response:", response);

        let applications = [];

        // ✅ Chuẩn format data theo swagger
        if (Array.isArray(response?.response)) {
          applications = response.response;
        } else if (Array.isArray(response?.responseData?.response)) {
          applications = response.responseData.response;
        } else if (Array.isArray(response)) {
          applications = response;
        }

        console.log("📊 Parsed applications:", applications);

        setData(applications || []);
      } catch (error) {
        console.error("❌ Error fetching applications:", error);
        setData([]);
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
        subtitle="List of Pending CVA Project Applications"
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
        }}
      >
        {data.length > 0 ? (
          <DataGrid
            rows={data}
            columns={columns}
            slots={{ toolbar: GridToolbar }}
            getRowId={(row) => row.id}
            loading={loading}
          />
        ) : (
          !loading && (
            <Typography color={colors.grey[300]} align="center" mt={5}>
              No data available.
            </Typography>
          )
        )}
      </Box>
    </Box>
  );
};

export default ApplicationList;
