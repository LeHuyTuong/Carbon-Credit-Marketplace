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
        console.log("API Company KYC Response:", response);

        const list = Array.isArray(response?.response)
          ? response.response
          : [];

        const mapped = list.map((c, index) => {
          const formatDate = (dateString) => {
            if (!dateString) return "—";
            const date = new Date(dateString);
            const day = String(date.getDate()).padStart(2, "0");
            const month = String(date.getMonth() + 1).padStart(2, "0");
            const year = date.getFullYear();
            const time = date.toLocaleTimeString();
            return `${day}/${month}/${year}, ${time}`;
          };

          return {
            id: index + 1,
            companyId: c.id,
            businessLicense: c.businessLicense || "N/A",
            taxCode: c.taxCode || "N/A",
            companyName: c.companyName || "N/A",
            address: c.address || "N/A",
            createdAt: formatDate(c.createAt),
            updatedAt: formatDate(c.updatedAt),
          };
        });

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
    {
      field: "address",
      headerName: "Address",
      flex: 1.2,
      renderCell: (params) => (
        <Typography
          variant="body2"
          sx={{
            display: "flex",
            alignItems: "center", // căn giữa theo chiều dọc nếu 1 dòng
            height: "100%",
            whiteSpace: "normal",
            wordWrap: "break-word",
            lineHeight: 1.4,
            textAlign: "left",
          }}
        >
          {params.value}
        </Typography>
      ),
    },
    { field: "createdAt", headerName: "Created At", flex: 1 },
    { field: "updatedAt", headerName: "Updated At", flex: 1 },
    // {
    //   field: "action",
    //   headerName: "Action",
    //   flex: 0.6,
    //   renderCell: (params) => (
    //     <div className="cellAction">
    //       <Link
    //         to={`/admin/companies_view/${params.row.companyId}`}
    //         style={{ textDecoration: "none" }}
    //       >
    //         <div className="viewButton">View</div>
    //       </Link>
    //     </div>
    //   ),
    // },
  ];

  return (
    <Box m="20px" sx={{ marginLeft: "290px" }} className="actionadmin">
      <Header
        title="COMPANY KYC PROFILES"
        subtitle="Managing registered companies"
      />
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
