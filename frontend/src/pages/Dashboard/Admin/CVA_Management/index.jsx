import { Box, Typography, useTheme } from "@mui/material";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { Link } from "react-router-dom";
import { useState, useEffect } from "react";
import "@/styles/actionadmin.scss";
import { getAllCVAKYCProfiles } from "@/apiAdmin/CVAAdmin.js";
import AdminDataGrid from "@/components/DataGrid/AdminDataGrid.jsx";

const CvaTeam = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const [data, setData] = useState([]);

  useEffect(() => {
    async function fetchCvaProfiles() {
      try {
        const response = await getAllCVAKYCProfiles();
        if (response?.response) {
          const profiles = response.response.map((item, index) => ({
            id: index + 1,
            cvaId: item.id,
            name: item.name,
            email: item.email,
            organization: item.organization,
            positionTitle: item.positionTitle,
            accreditationNo: item.accreditationNo,
            capacityQuota: item.capacityQuota,
            status: item.status,
            notes: item.notes,
            createdAt: new Date(item.createdAt).toLocaleString(),
            updatedAt: new Date(item.updatedAt).toLocaleString(),
          }));
          setData(profiles);
        }
      } catch (err) {
        console.error("Error fetching CVA KYC profiles:", err);
      }
    }

    fetchCvaProfiles();
  }, []);

  const columns = [
    { field: "cvaId", headerName: "ID", width: 80 },
    { field: "name", headerName: "Name", flex: 1 },
    { field: "email", headerName: "Email", flex: 1.2 },
    { field: "organization", headerName: "Organization", flex: 1 },
    { field: "positionTitle", headerName: "Position Title", flex: 1 },

    {
      field: "status",
      headerName: "Status",
      flex: 0.7,
      renderCell: ({ row: { status } }) => (
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
            color={status === "ACTIVE" ? "green" : "red"}
            fontWeight="600"
          >
            {status}
          </Typography>
        </Box>
      ),
    },

    { field: "createdAt", headerName: "Created At", flex: 1 },
    { field: "updatedAt", headerName: "Updated At", flex: 1 },
    {
      field: "action",
      headerName: "Action",
      flex: 0.7,
      renderCell: (params) => (
        <div className="cellAction">
          <Link
            to={`/admin/cva_view/${params.row.email}`}
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
      <Header title="CVA KYC PROFILES" subtitle="Managing CVA Verification Profiles" />
      <Box
        m="40px 0 0 0"
        height="75vh"
        sx={{
          "& .MuiDataGrid-root": { border: "none" },
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
        <AdminDataGrid rows={data} columns={columns} getRowId={(r) => r.id} />
      </Box>
    </Box>
  );
};

export default CvaTeam;
