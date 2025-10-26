import {
  Box,
  Typography,
  Grid,
  Paper,
  TextField,
  Button,
  Divider,
  useTheme,
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import AccountCircleIcon from "@mui/icons-material/AccountCircle";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { useState, useEffect } from "react";
import { getUserByEmail } from "@/apiAdmin/userAdmin.js";

// ===================== ROLE MAPPING =====================
const ACCESS_TO_ROLE = {
  ev_owner: "EV_OWNER",
  company: "COMPANY",
  cva: "CVA",
  admin: "ADMIN",
};
const ROLE_TO_ACCESS = Object.fromEntries(
  Object.entries(ACCESS_TO_ROLE).map(([k, v]) => [v, k])
);

const ViewUser = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();
  const { email } = useParams();

  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // ===================== FETCH USER =====================
  useEffect(() => {
    async function fetchUser() {
      setLoading(true);
      try {
        const res = await getUserByEmail(email);
        const data = res?.responseData;
        if (!data) throw new Error("No user data found");

        const primaryRole =
          Array.isArray(data.roles) && data.roles.length
            ? data.roles[0].name
            : "EV_OWNER";
        const access = ROLE_TO_ACCESS[primaryRole] ?? "ev_owner";

        let companyName = "";
        if (access === "company") {
          companyName =
            data.wallet?.carbonCredit?.company?.companyName ||
            data.company?.companyName ||
            "";
        }

        let organization = "";
        if (access === "cva") {
          organization =
            data.organization ||
            data.wallet?.carbonCredit?.project?.applications?.[0]?.reviewer
              ?.organization ||
            "";
        }

        const uiUser = {
          id: data.id ?? "",
          email: data.email || "",
          access,
          status: data.status || "ACTIVE",
          company: companyName,
          organization,
        };

        setUser(uiUser);
      } catch (error) {
        console.error("Error fetching user:", error);
      } finally {
        setLoading(false);
      }
    }

    fetchUser();
  }, [email]);

  if (loading) return <Typography>Loading...</Typography>;
  if (!user) return <Typography>User not found</Typography>;

  // ===================== UI =====================
  return (
    <Box m="20px">
      <Header title="USER DETAILS" subtitle="View user information" />
      <Paper
        elevation={2}
        sx={{
          p: 4,
          borderRadius: 3,
          backgroundColor: colors.primary[400],
          color: colors.grey[100],
        }}
      >
        {/* HEADER */}
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
          <Typography variant="h5" fontWeight="bold">
            Profile Information
          </Typography>
        </Box>

        {/* ICON ONLY */}
        <Box display="flex" justifyContent="center" alignItems="center" mb={4}>
          <AccountCircleIcon sx={{ fontSize: 100, color: colors.grey[200] }} />
        </Box>

        <Divider sx={{ mb: 3, borderColor: colors.grey[700] }} />

        {/* BASIC DETAILS */}
        <Grid container spacing={3} mb={4}>
          <Grid item xs={12} md={6}>
            <TextField
              label="Email Address"
              fullWidth
              value={user.email || ""}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Role"
              fullWidth
              value={user.access}
              InputProps={{ readOnly: true }}
            />
          </Grid>

          {user.access === "company" && (
            <Grid item xs={12} md={6}>
              <TextField
                label="Company Name"
                fullWidth
                value={user.company || ""}
                InputProps={{ readOnly: true }}
              />
            </Grid>
          )}

          {user.access === "cva" && (
            <Grid item xs={12} md={6}>
              <TextField
                label="Organization"
                fullWidth
                value={user.organization || ""}
                InputProps={{ readOnly: true }}
              />
            </Grid>
          )}

          <Grid item xs={12} md={6}>
            <TextField
              label="Status"
              fullWidth
              value={user.status}
              InputProps={{ readOnly: true }}
            />
          </Grid>
        </Grid>

        {/* BUTTON BACK */}
        <Box display="flex" justifyContent="flex-end" mt={4}>
          <Button
            variant="outlined"
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate("/admin/user_management")}
            sx={{
              borderColor: colors.blueAccent[400],
              color: colors.blueAccent[400],
              textTransform: "none",
            }}
          >
            Back
          </Button>
        </Box>
      </Paper>
    </Box>
  );
};

export default ViewUser;
