import { useState, useEffect } from "react";
import { Box, Typography } from "@mui/material";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { useTheme } from "@mui/material";
import { Link } from "react-router-dom";
import "@/styles/actionadmin.scss";
import { getProjectApplications } from "@/apiCVA/registrationCVA.js";
import CVADataGrid from "@/components/DataGrid/CVADataGrid.jsx";




const ApplicationList = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  // State
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [rowCount, setRowCount] = useState(0);

  // Fetch API
  useEffect(() => {
    const fetchData = async () => {
      try {
        console.log("Fetching pending CVA applications...");
        const response = await getProjectApplications();

        console.log(" Raw API response:", response);

        let applications = [];



        //  Chuẩn format data theo swagger


        if (Array.isArray(response?.response)) {
          applications = response.response;
        } else if (Array.isArray(response?.responseData?.response)) {
          applications = response.responseData.response;
        } else if (Array.isArray(response)) {
          applications = response;
        }

        console.log(" Parsed applications:", applications);

        setData(applications || []);
        setRowCount(applications?.length || 0);
      } catch (error) {
        console.error(" Error fetching applications:", error);
        setData([]);
        setRowCount(0);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  // Columns
  const columns = [
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
          CVA_APPROVED: colors.blueAccent[500],
          ADMIN_APPROVED: colors.greenAccent[500],
          ADMIN_REJECTED: colors.redAccent[500],
          CVA_REJECTED: "#FFB300",
          UNDER_REVIEW: colors.grey[500],
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
    <Box m="20px" sx={{ marginLeft: "290px" }} className="actionadmin">
      <Header
        title="APPLICATIONS"
        subtitle="List of Pending CVA Project Applications"
      />
      <Box
        m="40px 0 0 0"
        height="70vh"
        sx={{
          "& .MuiDataGrid-root": { border: "none", },
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
            backgroundColor: colors.greenAccent[700],
          },
        }}
      >
        {data.length > 0 ? (
          <CVADataGrid

            rows={data}
            columns={columns}
            getRowId={(r) => r.id}
            page={page}
            onPageChange={(newPage) => setPage(newPage)}
            pageSize={pageSize}
            onPageSizeChange={(newSize) => setPageSize(newSize)}
            rowCount={rowCount}
            loading={loading}
            getRowHeight={() => "auto"}
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
