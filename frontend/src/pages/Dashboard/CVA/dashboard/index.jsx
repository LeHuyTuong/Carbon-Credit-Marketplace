import React, { useEffect, useMemo, useState } from "react";
import { Box, IconButton, Typography, useTheme, CircularProgress, Alert } from "@mui/material";
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
import { useAuth } from "@/context/AuthContext.jsx";
import { fetchCvaCards, fetchMonthlyReportStatus, fetchMonthlyCreditStatus } from "@/apiCVA/dashboardCVA";

//  Helper for LineChart (Report Status) 
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

//Helper for BarChart (Credit Status) 
function buildCreditBarData(monthly = []) {
  if (!monthly.length) return { data: [], keys: [], indexBy: "month" };
  const indexBy = "month";
  const first = monthly[0];
  const normalize = (row) => ({
    month: row?.month ?? row?.monthLabel ?? row?.period ?? row?.label ?? "",
    ...row,
  });
  const keys = Object.keys(first).filter((k) => k !== "month" && typeof first[k] === "number");
  const data = monthly.map(normalize);
  return { data, keys, indexBy };
}

//  Helper: extract number whether it's .value, .total, or raw 
const getVal = (obj) => obj?.value ?? obj?.total ?? obj ?? 0;

const Dashboard = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const { token } = useAuth();

  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState(null);
  const [cards, setCards] = useState({});
  const [reportMonthly, setReportMonthly] = useState([]);
  const [creditMonthly, setCreditMonthly] = useState([]);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        setLoading(true);
        const [c, r, cr] = await Promise.all([
          fetchCvaCards(token),
          fetchMonthlyReportStatus(token),
          fetchMonthlyCreditStatus(token),
        ]);
        if (!active) return;
        setCards(c?.response ?? c ?? {});
        setReportMonthly(Array.isArray(r) ? r : []);
        setCreditMonthly(Array.isArray(cr) ? cr : []);
      } catch (e) {
        setErr(e?.message ?? "Load dashboard failed");
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => (active = false);
  }, [token]);

  const reportSeries = useMemo(() => buildReportSeries(reportMonthly), [reportMonthly]);
  const creditBar = useMemo(() => buildCreditBarData(creditMonthly), [creditMonthly]);

  const totalReports = useMemo(() => {
    const last = reportMonthly.at(-1);
    if (!last) return 0;
    return (last.approved ?? 0) + (last.pending ?? 0) + (last.rejected ?? 0);
  }, [reportMonthly]);

  return (
    <Box m="20px" sx={{ marginLeft: "290px" }}>
      {/* HEADER */}
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
          {/* === ROW 1: SUMMARY CARDS === */}
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
              subtitle="Projects"
              progress="0.8"
              icon={<WorkIcon sx={{ color: colors.primary[600], fontSize: 26 }} />}
            />
          </Box>
          {/* === ROW 2: LINE CHART === */}
          <Box gridColumn="span 12" gridRow="span 2" bg={colors.greenAccent[800]}>
            <Box mt="25px" px="30px" display="flex" justifyContent="space-between" alignItems="center">
              <Box>
                <Typography variant="h5" fontWeight="600" color={colors.grey[100]}>
                  Report Status
                </Typography>
                <Typography variant="h3" fontWeight="bold" color={colors.redAccent[500]}>
                  {totalReports.toLocaleString()}
                </Typography>
              </Box>
            </Box>
            <Box height="250px" m="-20px 0 0 0">
              <LineChart isDashboard series={reportSeries} />
            </Box>
          </Box>

          {/* === ROW 2: BAR CHART === */}
          {/* <Box gridColumn="span 6" gridRow="span 2" bg={colors.greenAccent[800]} p="30px">
            <Typography variant="h5" fontWeight="600" mb="15px">
              Credit Status
            </Typography>
            <Box height="100%" mt="-20px">
              <BarChart
                isDashboard
                data={creditBar.data}
                keys={creditBar.keys}
                indexBy={creditBar.indexBy}
              />
            </Box>
          </Box> */}
        </Box>
      )}
    </Box>
  );
};

export default Dashboard;