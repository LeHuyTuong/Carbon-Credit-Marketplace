import React, { useState, useEffect } from "react";
import {
  Box,
  Typography,
  Button,
  Grid,
  TextField,
  MenuItem,
  Paper,
  Snackbar,
  Alert,
  useTheme,
  CircularProgress,
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { getProjectById,updateProjectById } from "@/apiAdmin/projectAdmin.js";
import dayjs from "dayjs";
import "dayjs/locale/en";
import { LocalizationProvider } from "@mui/x-date-pickers/LocalizationProvider";
import { AdapterDayjs } from "@mui/x-date-pickers/AdapterDayjs";
import { DatePicker } from "@mui/x-date-pickers/DatePicker";

const ViewProject = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const { id } = useParams();
  const navigate = useNavigate();

  const [formData, setFormData] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [openSnackbar, setOpenSnackbar] = useState(false);
  const [loading, setLoading] = useState(true);

  //  Gọi API lấy chi tiết project theo ID
  useEffect(() => {
    const fetchProject = async () => {
      try {
        const res = await getProjectById(id);
        console.log("API project detail:", res);

        // API trả về object => không cần [0]
        const project = res?.response;
        if (project) {
          setFormData({
            projectid: project.id,
            projectname: project.title,
            shortdescription: project.description,
            starteddate: project.createdDate || "",
            enddate: project.endedDate || "",
            totalexpectedcredits: project.commitments || "",
            totalcompanies: project.technicalIndicators || "",
            status: project.status || "SUBMITTED",
          });
        } else {
          console.warn("Project not found in API response:", res);
        }
      } catch (error) {
        console.error(" Error fetching project:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchProject();
  }, [id]);

  const handleEdit = () => setIsEditing(true);
  
  const handleUpdate = async () => {
  try {
    const updatedData = {
      title: formData.projectname,
      description: formData.shortdescription,
      logo: formData.logo || "",
      commitments: formData.totalexpectedcredits,
      technicalIndicators: formData.totalcompanies,
      measurementMethod: formData.measurementmethod || "",
      legalDocsUrl: formData.legaldocurl || "",
    };
    console.log(" PUT data:", updatedData);
    const res = await updateProjectById(formData.projectid, updatedData);
    console.log("API Update Response:", res);

    if (res?.responseStatus?.responseCode === "200") {
      setIsEditing(false);
      setOpenSnackbar(true);
    } else {
      console.error("Update failed:", res);
    }
  } catch (error) {
    console.error("Error updating project:", error);
  }
};

  const handleCloseSnackbar = () => setOpenSnackbar(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  //  Loading state
  if (loading) {
    return (
      <Box m="20px" display="flex" justifyContent="center" alignItems="center" height="60vh">
        <CircularProgress />
      </Box>
    );
  }

  //  Nếu không có dữ liệu
  if (!formData) {
    return (
      <Box m="20px">
        <Typography variant="h5" color="error">
          Project not found.
        </Typography>
        <Button
          variant="contained"
          sx={{ mt: 2 }}
          onClick={() => navigate("/admin/project_management")}
        >
          Back
        </Button>
      </Box>
    );
  }

  //  Render giao diện chính
  return (
    <Box m="20px">
      <Header title="PROJECT DETAILS" subtitle="Detailed information of project" />

      <Paper
        elevation={3}
        sx={{
          p: 3,
          mt: 3,
          backgroundColor: colors.primary[400],
        }}
      >
        <LocalizationProvider dateAdapter={AdapterDayjs} adapterLocale="en">
          <Grid container spacing={3}>
            {/* LEFT COLUMN */}
            <Grid item xs={12} sm={6}>
              <Typography variant="h6" fontWeight="600" gutterBottom>
                Project ID:
              </Typography>
              <Typography mb={2}>{formData.projectid}</Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Project Name:
              </Typography>
              <Typography mb={2}>{formData.projectname}</Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Short Description:
              </Typography>
              {isEditing ? (
                <TextField
                  fullWidth
                  name="shortdescription"
                  value={formData.shortdescription}
                  onChange={handleChange}
                  multiline
                  rows={3}
                  sx={{ mb: 2 }}
                />
              ) : (
                <Typography mb={2}>{formData.shortdescription || "—"}</Typography>
              )}

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Started Date:
              </Typography>
              {isEditing ? (
                <DatePicker
                  value={formData.starteddate ? dayjs(formData.starteddate) : null}
                  onChange={(date) =>
                    setFormData((prev) => ({
                      ...prev,
                      starteddate: date ? date.format("YYYY-MM-DD") : "",
                    }))
                  }
                  sx={{ mb: 2, width: "100%" }}
                />
              ) : (
                <Typography mb={2}>{formData.starteddate || "—"}</Typography>
              )}
            </Grid>

            {/* RIGHT COLUMN */}
            <Grid item xs={12} sm={6}>
              <Typography variant="h6" fontWeight="600" gutterBottom>
                End Date:
              </Typography>
              {isEditing ? (
                <DatePicker
                  value={formData.enddate ? dayjs(formData.enddate) : null}
                  onChange={(date) =>
                    setFormData((prev) => ({
                      ...prev,
                      enddate: date ? date.format("YYYY-MM-DD") : "",
                    }))
                  }
                  sx={{ mb: 2, width: "100%" }}
                />
              ) : (
                <Typography mb={2}>{formData.enddate || "—"}</Typography>
              )}

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Total Expected Credits:
              </Typography>
              <Typography mb={2}>{formData.totalexpectedcredits || "—"}</Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Technical Indicators:
              </Typography>
              <Typography mb={2}>{formData.totalcompanies || "—"}</Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Status:
              </Typography>
              {isEditing ? (
                <TextField
                  select
                  fullWidth
                  name="status"
                  value={formData.status}
                  onChange={handleChange}
                  sx={{ mb: 2 }}
                >
                  <MenuItem value="SUBMITTED">SUBMITTED</MenuItem>
                  <MenuItem value="APPROVED">APPROVED</MenuItem>
                  <MenuItem value="REJECTED">REJECTED</MenuItem>
                </TextField>
              ) : (
                <Typography mb={2}>{formData.status}</Typography>
              )}
            </Grid>
          </Grid>
        </LocalizationProvider>

        {/* ACTION BUTTONS */}
        <Box display="flex" justifyContent="flex-end" mt={4} gap={2}>
          {!isEditing ? (
            <Button
              variant="contained"
              color="secondary"
              onClick={handleEdit}
              sx={{ fontWeight: 600 }}
            >
              Edit
            </Button>
          ) : (
            <Button
              variant="contained"
              color="success"
              onClick={handleUpdate}
              sx={{ fontWeight: 600 }}
            >
              Update
            </Button>
          )}
          <Button
            variant="outlined"
            color="info"
            onClick={() => navigate("/admin/project_management")}
            sx={{ fontWeight: 600 }}
          >
            Back
          </Button>
        </Box>
      </Paper>

      {/* SNACKBAR */}
      <Snackbar
        open={openSnackbar}
        autoHideDuration={2500}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
      >
        <Alert onClose={handleCloseSnackbar} severity="success" sx={{ width: "100%" }}>
          Update successfully!
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ViewProject;
