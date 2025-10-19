// src/scenes/admin/view_project.jsx
import React, { useState } from "react";
import {
  Box,
  Typography,
  Button,
  Grid,
  Paper,
  Snackbar,
  Alert,
  useTheme,
} from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import { tokens } from "@/themeCVA";
import { mockDataProjects } from "@/data/mockData";
import Header from "@/components/Chart/Header.jsx";
import dayjs from "dayjs";
import "dayjs/locale/en";
import { LocalizationProvider } from "@mui/x-date-pickers/LocalizationProvider";
import { AdapterDayjs } from "@mui/x-date-pickers/AdapterDayjs";

const ViewProject = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const { id } = useParams();
  const navigate = useNavigate();

  const project = mockDataProjects.find((p) => p.id === parseInt(id));

  const [openSnackbar, setOpenSnackbar] = useState(false);

  const handleCloseSnackbar = () => setOpenSnackbar(false);

  if (!project) {
    return (
      <Box m="20px">
        <Typography variant="h5" color="error">
          Project not found.
        </Typography>
        <Button
          variant="contained"
          sx={{ mt: 2 }}
          onClick={() => navigate("/cva/project_management")}
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
          borderRadius: "10px",
        }}
      >
        <LocalizationProvider dateAdapter={AdapterDayjs} adapterLocale="en">
          <Grid container spacing={3}>
            {/* LEFT COLUMN */}
            <Grid item xs={12} sm={6}>
              <Typography variant="h6" fontWeight="600" gutterBottom>
                Project ID:
              </Typography>
              <Typography mb={2}>{project.projectid}</Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Project Name:
              </Typography>
              <Typography mb={2}>{project.projectname}</Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Short Description:
              </Typography>
              <Typography mb={2}>{project.shortdescription}</Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Started Date:
              </Typography>
              <Typography mb={2}>
                {project.starteddate || "—"}
              </Typography>
            </Grid>

            {/* RIGHT COLUMN */}
            <Grid item xs={12} sm={6}>
              <Typography variant="h6" fontWeight="600" gutterBottom>
                End Date:
              </Typography>
              <Typography mb={2}>
                {project.enddate || "—"}
              </Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Total Expected Credits:
              </Typography>
              <Typography mb={2}>{project.totalexpectedcredits}</Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Total Participating Companies:
              </Typography>
              <Typography mb={2}>{project.totalcompanies}</Typography>

              <Typography variant="h6" fontWeight="600" gutterBottom>
                Status:
              </Typography>
              <Typography mb={2}>{project.status}</Typography>
            </Grid>
          </Grid>
        </LocalizationProvider>

        {/* ONLY BACK BUTTON */}
        <Box display="flex" justifyContent="flex-end" mt={4}>
          <Button
            variant="outlined"
            color="info"
            onClick={() => navigate("/cva/project_management")}
            sx={{ fontWeight: 600 }}
          >
            Back
          </Button>
        </Box>
      </Paper>

      
    </Box>
  );
};

export default ViewProject;
