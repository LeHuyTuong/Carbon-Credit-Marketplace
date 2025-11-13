import {
  Box,
  Typography,
  useTheme,
  CircularProgress,
  Alert,
  Chip,
} from "@mui/material";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { useState, useEffect } from "react";
import "@/styles/actionadmin.scss";
import AdminDataGrid from "@/components/DataGrid/AdminDataGrid.jsx";
import { listEvOwners } from "@/apiAdmin/ev_ownerAdmin.js"; // gọi API list

const EvOwnerTeam = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState(null);

  useEffect(() => {
    (async () => {
      try {
        setLoading(true);
        setErr(null);

        // Gọi API backend
        const raw = await listEvOwners();
        const arr = Array.isArray(raw)
          ? raw
          : raw?.response ??
          raw?.responseData ??
          raw?.data ??
          raw?.items ??
          raw?.content ??
          [];

        // Chuẩn hoá dữ liệu
        const normalized = arr.map((u, idx) => ({
          id: u.userId ?? idx + 1,
          userid: u.userId ?? "-",
          name: u.name ?? "-",
          email: u.email ?? "-",
          phone: u.phone ?? "-",
          gender: u.gender ?? "-",
          country: u.country ?? "-",
          documentNumber: u.documentNumber ?? "-",
          birthday: u.birthday
            ? (() => {
              const d = new Date(u.birthday);
              const day = String(d.getDate()).padStart(2, "0");
              const month = String(d.getMonth() + 1).padStart(2, "0");
              const year = d.getFullYear();
              return `${day}/${month}/${year}`;
            })()
            : "-",

          address: u.address ?? "-",
          status: "active", // mặc định active
        }));

        setRows(normalized);
      } catch (e) {
        console.error("Error fetching EV Owner list:", e);
        setErr(e?.message || "Failed to load EV owners");
        setRows([]);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  //  Cấu hình các cột hiển thị
  const columns = [
  { field: "userid", headerName: "User ID", flex: 0.6 },
  { field: "name", headerName: "Full Name", flex: 1 },
  { field: "email", headerName: "Email", flex: 1.2 },
  { field: "phone", headerName: "Phone", flex: 0.8 },
  { field: "gender", headerName: "Gender", flex: 0.6 },
  { field: "country", headerName: "Country", flex: 0.8 },
  { field: "documentNumber", headerName: "CCCD", flex: 1 },
  { field: "birthday", headerName: "Birthday", flex: 0.8 },
  {
    field: "status",
    headerName: "Status",
    flex: 0.6,
    minWidth: 120,
    renderCell: ({ row }) => (
      <Chip
        size="small"
        label={row.status === "active" ? "Active" : "Inactive"}
        color={row.status === "active" ? "success" : "default"}
        variant={row.status === "active" ? "filled" : "outlined"}
      />
    ),
  },
];


  //  Render UI
  return (
    <Box m="20px" sx={{ marginLeft: "290px" }} className="actionadmin">
      <Header title="EV OWNERS" subtitle="Managing EV Owner KYC Profiles" />

      {loading && (
        <Box mt={3} display="flex" alignItems="center" gap={1}>
          <CircularProgress size={22} />
          <Typography>Loading EV owners…</Typography>
        </Box>
      )}

      {err && (
        <Box mt={2}>
          <Alert severity="error">{String(err)}</Alert>
        </Box>
      )}

      {!loading && !err && (
        <Box
          m="40px 0 0 0"
          height="69vh"
          sx={{
            "& .MuiDataGrid-root": { border: "none", },
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
          <AdminDataGrid
            rows={rows}
            columns={columns}
            getRowId={(r) => r.id}
            disableRowSelectionOnClick
          />
        </Box>
      )}
    </Box>
  );
};

export default EvOwnerTeam;