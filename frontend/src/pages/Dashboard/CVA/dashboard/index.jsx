import React, { useState, useEffect } from "react";
import { Box, IconButton, Typography, useTheme } from "@mui/material";
import { tokens } from "@/themeCVA";
import DownloadOutlinedIcon from "@mui/icons-material/DownloadOutlined";
import AssessmentIcon from "@mui/icons-material/Assessment";
import CreditCardIcon from "@mui/icons-material/CreditCard";
import CorporateFareIcon from "@mui/icons-material/CorporateFare";
import WorkIcon from "@mui/icons-material/Work";
import Header from "@/components/Chart/Header.jsx";
import LineChart from "@/components/Chart/LineChart.jsx";
import BarChart from "@/components/Chart/BarChart.jsx";
import StatBox from "@/components/Chart/StatBox.jsx";

const Dashboard = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  const [applications, setApplications] = useState([]);
  const [stats, setStats] = useState({
    reports: 0,
    transactions: 0,
    users: 0,
    vehicles: 0,
  });

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        // Nếu API yêu cầu token
        const token = localStorage.getItem("token");

        const response = await apiFetch.get("/api/v1/project-applications", {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        const data = response.data.data || [];

        setApplications(data);
        setStats({
          reports: data.length,
          transactions: 5732,
          users: 32441,
          vehicles: 1325134,
        });
      } catch (error) {
        console.error("❌ Failed to fetch dashboard data:", error);
      }
    };
    fetchDashboardData();
  }, []);


  return (
    <Box m="20px">
      {/* HEADER */}
      <Box display="flex" justifyContent="space-between" alignItems="center">
        <Header
          title="DASHBOARD-CARBON VERIFICATION AUTHORITY"
          subtitle="Welcome to your dashboard"
        />
      </Box>

      {/* GRID & CHARTS */}
      <Box
        display="grid"
        gridTemplateColumns="repeat(12, 1fr)"
        gridAutoRows="140px"
        gap="20px"
      >
        {/* ROW 1 - STAT BOXES */}
        <Box
          gridColumn="span 3"
          backgroundColor={colors.greenAccent[800]}
          display="flex"
          alignItems="center"
          justifyContent="center"
        >
          <StatBox
            title={stats.reports.toLocaleString()}
            subtitle="Reports"
            progress="0.75"
            increase="+14%"
            icon={<AssessmentIcon sx={{ color: colors.primary[600], fontSize: "26px" }} />}
          />
        </Box>
        <Box
          gridColumn="span 3"
          backgroundColor={colors.greenAccent[800]}
          display="flex"
          alignItems="center"
          justifyContent="center"
        >
          <StatBox
            title="5,732"
            subtitle="Credits"
            progress="0.50"
            increase="+21%"
            icon={<CreditCardIcon sx={{ color: colors.primary[600], fontSize: "26px" }} />}
          />
        </Box>
        <Box
          gridColumn="span 3"
          backgroundColor={colors.greenAccent[800]}
          display="flex"
          alignItems="center"
          justifyContent="center"
        >
          <StatBox
            title="32,441"
            subtitle="Companies"
            progress="0.30"
            increase="+5%"
            icon={<CorporateFareIcon sx={{ color: colors.primary[600], fontSize: "26px" }} />}
          />
        </Box>
        <Box
          gridColumn="span 3"
          backgroundColor={colors.greenAccent[800]}
          display="flex"
          alignItems="center"
          justifyContent="center"
        >
          <StatBox
            title="1,325,134"
            subtitle="Projects"
            progress="0.80"
            increase="+43%"
            icon={<WorkIcon sx={{ color: colors.primary[600], fontSize: "26px" }} />}
          />
        </Box>

        {/* ROW 2 - LINE CHART */}
        <Box
          gridColumn="span 6"
          gridRow="span 2"
          backgroundColor={colors.greenAccent[800]}
        >
          <Box
            mt="25px"
            p="0 30px"
            display="flex"
            justifyContent="space-between"
            alignItems="center"
          >
            <Box>
              <Typography variant="h5" fontWeight="600" color={colors.grey[100]}>
                Report Status
              </Typography>
              <Typography variant="h3" fontWeight="bold" color={colors.redAccent[500]}>
                9,345
              </Typography>
            </Box>
            <Box>
              <IconButton>
                <DownloadOutlinedIcon
                  sx={{ fontSize: "26px", color: colors.blueAccent[500] }}
                />
              </IconButton>
            </Box>
          </Box>
          <Box height="250px" m="-20px 0 0 0">
            <LineChart isDashboard={true} />
          </Box>
        </Box>

        {/* ROW 2 - BAR CHART (thay thế Recent Transactions) */}
        <Box
          gridColumn="span 6"
          gridRow="span 2"
          backgroundColor={colors.greenAccent[800]}
          p="30px"
        >
          <Typography variant="h5" fontWeight="600" sx={{ marginBottom: "15px" }}>
            Credit Status
          </Typography>
          <Box height="100%" mt="-20px">
            <BarChart isDashboard={true} />
          </Box>
        </Box>
      </Box>
    </Box>
  );
};

export default Dashboard;
