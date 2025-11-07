import { Box, Typography, useTheme } from "@mui/material";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { Link } from "react-router-dom";
import { useState, useEffect } from "react";
import "@/styles/actionadmin.scss";
import { getAllCompanyKYCProfiles } from "@/apiAdmin/companiesAdmin.js";
import AdminDataGrid from "@/components/DataGrid/AdminDataGrid.jsx";

const CompanyTeam = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const [data, setData] = useState([]);

  useEffect(() => {
    async function fetchCompanies() {
      try {
        const response = await getAllCompanyKYCProfiles();
        console.log(" API Company KYC Response:", response);

        const list = Array.isArray(response?.response)
          ? response.response
          : [];

        const mapped = list.map((c, index) => ({
          id: index + 1,
          companyId: c.id,
          businessLicense: c.businessLicense || "N/A",
          taxCode: c.taxCode || "N/A",
          companyName: c.companyName || "N/A",
          address: c.address || "N/A",
          createdAt: c.createAt
            ? new Date(c.createAt).toLocaleString()
            : "—",
          updatedAt: c.updatedAt
            ? new Date(c.updatedAt).toLocaleString()
            : "—",
        }));

        setData(mapped);
      } catch (err) {
        console.error("Error fetching company KYC profiles:", err);
      }
    }

    fetchCompanies();
  }, []);

  const columns = [
    { field: "companyId", headerName: "Company ID", width: 120 },
    { field: "companyName", headerName: "Company Name", flex: 1 },
    { field: "businessLicense", headerName: "Business License", flex: 1 },
    { field: "taxCode", headerName: "Tax Code", flex: 1 },
    { field: "address", headerName: "Address", flex: 1.2 },
    { field: "createdAt", headerName: "Created At", flex: 1 },
    { field: "updatedAt", headerName: "Updated At", flex: 1 },
    {
      field: "action",
      headerName: "Action",
      flex: 0.6,
      renderCell: (params) => (
        <div className="cellAction">
          <Link
            to={`/admin/companies_view/${params.row.companyId}`}
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
      <Header title="COMPANY KYC PROFILES" subtitle="Managing registered companies" />
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

export default CompanyTeam;
