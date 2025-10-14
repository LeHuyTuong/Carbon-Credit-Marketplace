// src/scenes/admin/view_project.jsx
import React, { useState } from "react";
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
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import { tokens } from "@/theme";
import { mockDataProjects } from "@/data/mockData";
import Header from "@/components/Chart/Header.jsx";

// Date picker
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

  const project = mockDataProjects.find((p) => p.id === parseInt(id));

  const [formData, setFormData] = useState({
    projectid: project?.projectid || "",
    projectname: project?.projectname || "",
    shortdescription: project?.shortdescription || "",
    starteddate: project?.starteddate || "",
    enddate: project?.enddate || "",
    totalexpectedcredits: project?.totalexpectedcredits || "",
    totalcompanies: project?.totalcompanies || "12",
    status: project?.status || "Coming_Soon",
  });

  const [isEditing, setIsEditing] = useState(false);
  const [openSnackbar, setOpenSnackbar] = useState(false);

  const handleEdit = () => setIsEditing(true);
  const handleUpdate = () => {
    setIsEditing(false);
    setOpenSnackbar(true);
  };
  const handleCloseSnackbar = () => setOpenSnackbar(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  if (!project) {
    return (
      <Box m="20px">
        <Typography variant="h5" color="error">
          Project not found.
        </Typography>
        <Button
          variant="contained"
          sx={{ mt: 2 }}
          onClick={() => navigate("/admin/project")}
        >
          Back
        </Button>
      </Box>
    );
  }

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
                <Typography mb={2}>{formData.shortdescription}</Typography>
              )}

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Started Date:
              </Typography>
              {isEditing ? (
                <DatePicker
                  value={
                    formData.starteddate
                      ? dayjs(formData.starteddate, "DD/MM/YYYY")
                      : null
                  }
                  onChange={(date) =>
                    setFormData((prev) => ({
                      ...prev,
                      starteddate: date ? date.format("DD/MM/YYYY") : "",
                    }))
                  }
                  sx={{ mb: 2, width: "100%" }}
                />
              ) : (
                <Typography mb={2}>{formData.starteddate}</Typography>
              )}
            </Grid>

            {/* RIGHT COLUMN */}
            <Grid item xs={12} sm={6}>
              <Typography variant="h6" fontWeight="600" gutterBottom>
                End Date:
              </Typography>
              {isEditing ? (
                <DatePicker
                  value={
                    formData.enddate
                      ? dayjs(formData.enddate, "DD/MM/YYYY")
                      : null
                  }
                  onChange={(date) =>
                    setFormData((prev) => ({
                      ...prev,
                      enddate: date ? date.format("DD/MM/YYYY") : "",
                    }))
                  }
                  sx={{ mb: 2, width: "100%" }}
                />
              ) : (
                <Typography mb={2}>
                  {formData.enddate ? formData.enddate : "—"}
                </Typography>
              )}

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Total Expected Credits:
              </Typography>
              <Typography mb={2}>{formData.totalexpectedcredits}</Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Total Participating Companies:
              </Typography>
              <Typography mb={2}>{formData.totalcompanies}</Typography>

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
                  <MenuItem value="Is_Open">Is_Open</MenuItem>
                  <MenuItem value="Coming_Soon">Coming_Soon</MenuItem>
                  <MenuItem value="End">End</MenuItem>
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
