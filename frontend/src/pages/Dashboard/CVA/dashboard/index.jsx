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


//---------------------------------------------------------------
// Helper: Build LINE CHART DATA (Report Status Chart)
//---------------------------------------------------------------
/**
 * Chuyển định dạng API trả về → format dùng cho LineChart
 * Nếu chỉ có 1 tháng → thêm 1 tháng giả để tránh chart bị gãy
 */
function buildReportSeries(monthly = []) {
  const label = (o) => o?.month ?? o?.monthLabel ?? o?.period ?? o?.label ?? "";
  const num = (o, k) => Number(o?.[k] ?? 0);

  // Nếu chỉ có 1 tháng thì thêm 1 tháng "Previous"
  if (monthly.length === 1) {
    monthly = [{ month: "Previous", approved: 0, pending: 0, rejected: 0 }, ...monthly];
  }

  return [
    { id: "Approved", data: monthly.map((m) => ({ x: label(m), y: num(m, "approved") })) },
    { id: "Pending", data: monthly.map((m) => ({ x: label(m), y: num(m, "pending") })) },
    { id: "Rejected", data: monthly.map((m) => ({ x: label(m), y: num(m, "rejected") })) },
  ];
}


//---------------------------------------------------------------
// Helper: Build PIE CHART DATA (Application Status)
//---------------------------------------------------------------
/**
 * Lấy dữ liệu tháng cuối cùng → hiển thị Pie Chart
 */
function buildApplicationPieData(monthly = []) {
  if (!monthly.length) return [];

  const last = monthly.at(-1); // tháng gần nhất

  return [
    { id: "Approved", label: "Approved", value: last.approved ?? 0, color: "#4CAF50" },
    { id: "Under_review", label: "Under_review", value: last.submitted ?? 0, color: "#FFC107" },
    { id: "Rejected", label: "Rejected", value: last.rejected ?? 0, color: "#F44336" },
  ];
}


// Helper lấy số từ API response
const getVal = (obj) => obj?.value ?? obj?.total ?? obj ?? 0;


//---------------------------------------------------------------
// MAIN COMPONENT: DASHBOARD
//---------------------------------------------------------------
const Dashboard = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const { token } = useAuth();

  // State
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState(null);
  const [cards, setCards] = useState({});

  // Data chart
  const [reportMonthly, setReportMonthly] = useState([]);
  const [applicationMonthly, setApplicationMonthly] = useState([]);


  //-------------------------------------------------------------
  // Fetch dữ liệu dashboard khi load
  //-------------------------------------------------------------
  useEffect(() => {
    let active = true; // ngăn setState khi component đã unmount

    (async () => {
      try {
        setLoading(true);

        // Gọi tất cả API song song cho nhanh
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
        // Xử lý lỗi chung
        setErr(e?.message ?? "Load dashboard failed");
      } finally {
        if (active) setLoading(false);
      }
    })();

    return () => (active = false);
  }, [token]);


  //-------------------------------------------------------------
  // Memo hóa để tránh render lại không cần thiết
  //-------------------------------------------------------------
  const reportSeries = useMemo(() => buildReportSeries(reportMonthly), [reportMonthly]);
  const applicationPie = useMemo(() => buildApplicationPieData(applicationMonthly), [applicationMonthly]);

  // Tổng số report tháng gần nhất
  const totalReports = useMemo(() => {
    const last = reportMonthly.at(-1);
    if (!last) return 0;
    return (last.approved ?? 0) + (last.pending ?? 0) + (last.rejected ?? 0);
  }, [reportMonthly]);



  //-------------------------------------------------------------
  // UI RENDER
  //-------------------------------------------------------------
  return (
    <Box m="20px" sx={{ marginLeft: "290px" }}>
      
      {/* HEADER */}
      <Box display="flex" justifyContent="space-between" alignItems="center">
        <Header
          title="DASHBOARD - CARBON VERIFICATION AUTHORITY"
          subtitle="Live statistics from backend APIs"
        />
      </Box>

      {/* Loading */}
      {loading && (
        <Box mt={3} display="flex" alignItems="center" gap={1}>
          <CircularProgress size={22} />
          <Typography>Loading dashboard…</Typography>
        </Box>
      )}

      {/* Error */}
      {err && (
        <Box mt={2}>
          <Alert severity="error">{String(err)}</Alert>
        </Box>
      )}

      {/* Dashboard CONTENT */}
      {!loading && !err && (
        <Box
          display="grid"
          gridTemplateColumns="repeat(12, 1fr)" // layout 12 cột
          gridAutoRows="140px"
          gap="20px"
        >
          {/*---------------------------------------------------------
           * 4 CARD THỐNG KÊ TRÊN CÙNG (Reports / Credits / Companies / Applications)
          ----------------------------------------------------------*/}

          <Box gridColumn="span 3" sx={{ backgroundColor: colors.greenAccent[900] }} display="flex" justifyContent="center" alignItems="center">
            <StatBox
              title={getVal(cards.reports).toLocaleString()}
              subtitle="Reports"
              progress="0.75"
              icon={<AssessmentIcon sx={{ color: colors.primary[600], fontSize: 26 }} />}
            />
          </Box>

          <Box gridColumn="span 3" sx={{ backgroundColor: colors.greenAccent[900] }} display="flex" justifyContent="center" alignItems="center">
            <StatBox
              title={getVal(cards.credits).toLocaleString()}
              subtitle="Credits"
              progress="0.5"
              icon={<CreditCardIcon sx={{ color: colors.primary[600], fontSize: 26 }} />}
            />
          </Box>

          <Box gridColumn="span 3" sx={{ backgroundColor: colors.greenAccent[900] }} display="flex" justifyContent="center" alignItems="center">
            <StatBox
              title={getVal(cards.companies).toLocaleString()}
              subtitle="Companies"
              progress="0.3"
              icon={<CorporateFareIcon sx={{ color: colors.primary[600], fontSize: 26 }} />}
            />
          </Box>

          <Box gridColumn="span 3" sx={{ backgroundColor: colors.greenAccent[900] }} display="flex" justifyContent="center" alignItems="center">
            <StatBox
              title={getVal(cards.projects).toLocaleString()}
              subtitle="Applications"
              progress="0.8"
              icon={<WorkIcon sx={{ color: colors.primary[600], fontSize: 26 }} />}
            />
          </Box>



          {/*---------------------------------------------------------
           * REPORT LINE CHART
          ----------------------------------------------------------*/}
          <Box gridColumn="span 6" gridRow="span 2" bgcolor={colors.greenAccent[800]}>
            <Box mt="25px" px="30px">
              <Typography variant="h5" fontWeight="600">Report Status</Typography>

              <Typography variant="h3" fontWeight="bold" color={colors.redAccent[500]}>
                {totalReports.toLocaleString()}
              </Typography>
            </Box>

            {/* Line Chart Container */}
            <Box height="250px" m="-20px 0 0 0">
              <LineChart isDashboard series={reportSeries} />
            </Box>
          </Box>



          {/*---------------------------------------------------------
           * APPLICATION PIE CHART
          ----------------------------------------------------------*/}
          <Box gridColumn="span 6" gridRow="span 2" bgcolor={colors.greenAccent[800]} p="30px">
            <Typography variant="h5" fontWeight="600" mb="15px">
              Application Status
            </Typography>

            {/* Pie Chart Container */}
            <Box height="120%" mt="-40px" sx={{ transform: "translateY(-25px)", ml: 5 }}>
              <PieChart data={applicationPie} />
            </Box>
          </Box>


        </Box>
      )}
    </Box>
  );
};

export default Dashboard;
