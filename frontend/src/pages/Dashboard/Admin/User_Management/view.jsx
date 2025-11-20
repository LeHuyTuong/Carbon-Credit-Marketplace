import {
  Box,
  Typography,
  Grid,
  Paper,
  TextField,
  Button,
  Divider,
  CircularProgress,
  MenuItem,
  useTheme,
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { useState, useEffect } from "react";
import { getUserById, updateUserStatus } from "@/apiAdmin/userAdmin.js";
import { useSnackbar } from "@/hooks/useSnackbar.jsx";


// ROLE MAPPING
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
  const { id } = useParams();

  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const { showSnackbar, SnackbarComponent } = useSnackbar();
  const [newStatus, setNewStatus] = useState("");

  // FETCH USER
  useEffect(() => {
    async function fetchUser() {
      setLoading(true);
      try {
        const res = await getUserById(id);
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
        setNewStatus(uiUser.status);
      } catch (error) {
        console.error("Error fetching user:", error);
        showSnackbar("Failed to load user data", "error");
      } finally {
        setLoading(false);
      }
    }

    fetchUser();
  }, [id]);

  if (loading)
    return (
      <Box
        display="flex"
        justifyContent="center"
        alignItems="center"
        height="70vh"
        sx={{ marginLeft: "290px" }}
      >
        <CircularProgress />
      </Box>
    );

  if (!user)
    return (
      <Box textAlign="center" sx={{ marginLeft: "290px" }} mt={5}>
        <Typography variant="h6" color="error">
          User not found.
        </Typography>
        <Button
          onClick={() => navigate("/admin/user_management")}
          variant="contained"
          sx={{ mt: 2 }}
        >
          Back
        </Button>
      </Box>
    );

  // UPDATE STATUS
  const handleUpdateStatus = async () => {
    try {
      const payload = { status: newStatus };
      await updateUserStatus(user.id, payload);

      showSnackbar("success","Status updated successfully!");
    } catch (error) {
      const msg =
        error?.response?.data?.message ||
        error?.message ||
        "Failed to update status.";
      showSnackbar(msg, "error");
      console.error(error);
    }
  };

  return (
    <Box m={3} sx={{ marginLeft: "290px", maxWidth: "800px", width: "100%" }}>
      <Header title="USER DETAILS" subtitle="Edit user information" />

      <Paper elevation={3} sx={{ p: 3, mt: 2, backgroundColor: colors.primary[400] }}>


        <Grid container spacing={6}>
          {/* GENERAL INFO */}
          <Grid item xs={12} md={4}>
            <Typography variant="h5" fontWeight="700" color="secondary" gutterBottom>
              General Info
            </Typography>

            <Typography fontWeight={600}>Email:</Typography>
            <TextField
              value={user.email}
              fullWidth
              size="small"
              InputProps={{ readOnly: true }}
              sx={{ mb: 2, backgroundColor: "rgba(255,255,255,0.08)", borderRadius: 1 }}
            />

            <Typography fontWeight={600}>Role:</Typography>
            <TextField
              value={user.access.toUpperCase()}
              fullWidth
              size="small"
              InputProps={{ readOnly: true }}
              sx={{ mb: 2, backgroundColor: "rgba(255,255,255,0.08)", borderRadius: 1 }}
            />

            {user.company && (
              <>
                <Typography fontWeight={600}>Company:</Typography>
                <TextField
                  value={user.company}
                  fullWidth
                  size="small"
                  InputProps={{ readOnly: true }}
                  sx={{
                    mb: 2,
                    backgroundColor: "rgba(255,255,255,0.08)",
                    borderRadius: 1,
                  }}
                />
              </>
            )}

            {user.organization && (
              <>
                <Typography fontWeight={600}>Organization:</Typography>
                <TextField
                  value={user.organization}
                  fullWidth
                  size="small"
                  InputProps={{ readOnly: true }}
                  sx={{
                    mb: 2,
                    backgroundColor: "rgba(255,255,255,0.08)",
                    borderRadius: 1,
                  }}
                />
              </>
            )}
          </Grid>

          {/* STATUS EDIT */}
          <Grid item xs={12} md={4}>
            <Typography variant="h5" fontWeight="700" color="secondary" gutterBottom>
              Status Control
            </Typography>

            <Typography fontWeight={600}>Status:</Typography>
            <TextField
              select
              fullWidth
              size="small"
              value={newStatus}
              onChange={(e) => setNewStatus(e.target.value)}
              sx={{
                mb: 2,
                backgroundColor: "rgba(255,255,255,0.1)",
                color: "#fff",
                borderRadius: 1,
              }}
            >
              <MenuItem value="ACTIVE">ACTIVE</MenuItem>
              <MenuItem value="INACTIVE">INACTIVE</MenuItem>
            </TextField>


          </Grid>
        </Grid>

        {/* STATUS + BACK BUTTONS */}
        <Box display="flex" justifyContent="flex-end" mt={4}>
          <Button
            variant="contained"
            color="secondary"
            onClick={handleUpdateStatus}
            sx={{ fontWeight: 600, mr: 2 }}
          >
            Save Status
          </Button>

          <Button
            variant="outlined"
            color="info"
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate("/admin/user_management")}
          >
            Back
          </Button>
        </Box>

      </Paper>

      {/* SNACKBAR */}
      {SnackbarComponent}
    </Box>
  );
};

export default ViewUser;
