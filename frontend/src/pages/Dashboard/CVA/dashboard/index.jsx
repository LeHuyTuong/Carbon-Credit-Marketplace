import React, { useEffect, useMemo, useState } from "react";
import { Box, Typography, useTheme, CircularProgress, Alert } from "@mui/material";
import { tokens } from "@/themeCVA";
import AssessmentIcon from "@mui/icons-material/Assessment";
import CreditCardIcon from "@mui/icons-material/CreditCard";
import CorporateFareIcon from "@mui/icons-material/CorporateFare";
import WorkIcon from "@mui/icons-material/Work";
import Header from "@/components/Chart/Header.jsx";
import LineChart from "@/components/Chart/LineChart.jsx";
import StatBox from "@/components/Chart/StatBox.jsx";
import { useAuth } from "@/context/AuthContext.jsx";
import PieChart from "@/components/Chart/PieChart.jsx";


import {
  fetchCvaCards,
  fetchMonthlyReportStatus,
  fetchMonthlyApplicationStatus
} from "@/apiCVA/dashboardCVA";


//REPORT LINE CHART 
function buildReportSeries(monthly = []) {
  const label = (o) => o?.month ?? o?.monthLabel ?? o?.period ?? o?.label ?? "";
  const num = (o, k) => Number(o?.[k] ?? 0);

  if (monthly.length === 1) {
    monthly = [{ month: "Previous", approved: 0, pending: 0, rejected: 0 }, ...monthly];
  }

  return [
    { id: "Approved", data: monthly.map((m) => ({ x: label(m), y: num(m, "approved") })) },
    { id: "Pending", data: monthly.map((m) => ({ x: label(m), y: num(m, "pending") })) },
    { id: "Rejected", data: monthly.map((m) => ({ x: label(m), y: num(m, "rejected") })) },
  ];
}


// APPLICATION BAR CHART
function buildApplicationPieData(monthly = []) {
  if (!monthly.length) return [];

  // Lấy tháng cuối cùng để hiển thị
  const last = monthly.at(-1);

  return [
    { id: "Approved", label: "Approved", value: last.approved ?? 0, color: "#4CAF50" },
    { id: "Submitted", label: "Submitted", value: last.submitted ?? 0, color: "#FFC107" },
    { id: "Rejected", label: "Rejected", value: last.rejected ?? 0, color: "#F44336" },
  ];
}

// Helper lấy số
const getVal = (obj) => obj?.value ?? obj?.total ?? obj ?? 0;



//  MAIN COMPONENT
const Dashboard = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const { token } = useAuth();

  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState(null);
  const [cards, setCards] = useState({});
  const [reportMonthly, setReportMonthly] = useState([]);
  const [applicationMonthly, setApplicationMonthly] = useState([]);

  useEffect(() => {
    let active = true;

    (async () => {
      try {
        setLoading(true);

        const [c, r, a] = await Promise.all([
          fetchCvaCards(token),
          fetchMonthlyReportStatus(token),
          fetchMonthlyApplicationStatus(token),
        ]);

        if (!active) return;

        setCards(c || {});
        setReportMonthly(Array.isArray(r) ? r : []);
        setApplicationMonthly(Array.isArray(a) ? a : []);

      } catch (e) {
        setErr(e?.message ?? "Load dashboard failed");
      } finally {
        if (active) setLoading(false);
      }
    })();

    return () => (active = false);
  }, [token]);

  const reportSeries = useMemo(() => buildReportSeries(reportMonthly), [reportMonthly]);
  const applicationPie = useMemo(() => buildApplicationPieData(applicationMonthly), [applicationMonthly]);


  const totalReports = useMemo(() => {
    const last = reportMonthly.at(-1);
    if (!last) return 0;
    return (last.approved ?? 0) + (last.pending ?? 0) + (last.rejected ?? 0);
  }, [reportMonthly]);



  // ================== UI ==================
  return (
    <Box m="20px" sx={{ marginLeft: "290px" }}>

      <Box display="flex" justifyContent="space-between" alignItems="center">
        <Header
          title="DASHBOARD - CARBON VERIFICATION AUTHORITY"
          subtitle="Live statistics from backend APIs"
        />
      </Box>

      {loading && (
        <Box mt={3} display="flex" alignItems="center" gap={1}>
          <CircularProgress size={22} />
          <Typography>Loading dashboard…</Typography>
        </Box>
      )}

      {err && (
        <Box mt={2}>
          <Alert severity="error">{String(err)}</Alert>
        </Box>
      )}

      {!loading && !err && (
        <Box
          display="grid"
          gridTemplateColumns="repeat(12, 1fr)"
          gridAutoRows="140px"
          gap="20px"
        >
          {/* Summary Cards */}
          {/* Reports */}
          <Box gridColumn="span 3" sx={{ backgroundColor: colors.greenAccent[900] }} display="flex" justifyContent="center" alignItems="center">
            <StatBox
              title={getVal(cards.reports).toLocaleString()}
              subtitle="Reports"
              progress="0.75"
              icon={<AssessmentIcon sx={{ color: colors.primary[600], fontSize: 26 }} />}
            />
          </Box>

          {/* Credits */}
          <Box gridColumn="span 3" sx={{ backgroundColor: colors.greenAccent[900] }} display="flex" justifyContent="center" alignItems="center">
            <StatBox
              title={getVal(cards.credits).toLocaleString()}
              subtitle="Credits"
              progress="0.5"
              icon={<CreditCardIcon sx={{ color: colors.primary[600], fontSize: 26 }} />}
            />
          </Box>

          {/* Companies */}
          <Box gridColumn="span 3" sx={{ backgroundColor: colors.greenAccent[900] }} display="flex" justifyContent="center" alignItems="center">
            <StatBox
              title={getVal(cards.companies).toLocaleString()}
              subtitle="Companies"
              progress="0.3"
              icon={<CorporateFareIcon sx={{ color: colors.primary[600], fontSize: 26 }} />}
            />
          </Box>

          {/* Projects */}
          <Box gridColumn="span 3" sx={{ backgroundColor: colors.greenAccent[900] }} display="flex" justifyContent="center" alignItems="center">
            <StatBox
              title={getVal(cards.projects).toLocaleString()}
              subtitle="Projects"
              progress="0.8"
              icon={<WorkIcon sx={{ color: colors.primary[600], fontSize: 26 }} />}
            />
          </Box>


          {/* REPORT LINE CHART */}
          <Box gridColumn="span 6" gridRow="span 2" bgcolor={colors.greenAccent[800]} >
            <Box mt="25px" px="30px">
              <Typography variant="h5" fontWeight="600">Report Status</Typography>
              <Typography variant="h3" fontWeight="bold" color={colors.redAccent[500]}>
                {totalReports.toLocaleString()}
              </Typography>
            </Box>
            <Box height="250px" m="-20px 0 0 0">
              <LineChart isDashboard series={reportSeries} />
            </Box>
          </Box>
          {/* APPLICATION PIE CHART */}
          <Box gridColumn="span 6" gridRow="span 2" bgcolor={colors.greenAccent[800]} p="30px">
            <Typography variant="h5" fontWeight="600" mb="15px">
              Application Status
            </Typography>

            <Box height="120%" mt="-40px" sx={{ transform: "translateY(-25px)", ml: 5}}>
              <PieChart data={applicationPie} />
            </Box>
          </Box>



        </Box>
      )}
    </Box>
  );
};

export default Dashboard;
