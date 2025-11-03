import { Box, Button, IconButton, Typography, useTheme } from "@mui/material";
import { tokens } from "@/theme";
import DownloadOutlinedIcon from "@mui/icons-material/DownloadOutlined";
import ElectricCarIcon from "@mui/icons-material/ElectricCar";
import PersonAddIcon from "@mui/icons-material/PersonAdd";
import PointOfSaleOutlinedIcon from "@mui/icons-material/PointOfSaleOutlined";
import AssessmentOutlinedIcon from "@mui/icons-material/AssessmentOutlined";
import Header from "@/components/Chart/Header.jsx";
import LineChart from "@/components/Chart/LineChart.jsx";
import GeographyChart from "@/components/Chart/GeographyChart.jsx";
import BarChart from "@/components/Chart/BarChart.jsx";
import StatBox from "@/components/Chart/StatBox.jsx";
import ProgressCircle from "@/components/Chart/ProgressCircle.jsx";
import { useEffect, useState } from "react";
import { countVehicle, getWithdrawlHistoryByAdmin, countWalletTransactions,countUsers } from "@/apiAdmin/apiDashboard.js";

const Dashboard = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  // States
  const [electricVehicleCount, setElectricVehicleCount] = useState(0);
  const [walletTransactionCount, setWalletTransactionCount] = useState(0);
  const [withdrawHistory, setWithdrawHistory] = useState([]);
  const [userCount, setUserCount] = useState(0);

  useEffect(() => {
    // Fetch electric vehicles
    const fetchElectricVehicleCount = async () => {
      try {
        const res = await countVehicle({
          requestTrace: "dashboard-electric-vehicle",
          requestDateTime: new Date().toISOString(),
        });
        setElectricVehicleCount(res.response ?? 0);
      } catch (error) {
        console.error("Error fetching electric vehicle count:", error);
      }
    };

    // Fetch wallet transactions
    const fetchWalletTransactionCount = async () => {
  try {
    const count = await countWalletTransactions({
      requestTrace: "dashboard-wallet-transactions",
      requestDateTime: new Date().toISOString(),
    });
    setWalletTransactionCount(Number(count) || 0);
  } catch (error) {
    console.error("Error fetching wallet transaction count:", error);
  }
};


    // Fetch withdrawal history
    const fetchWithdrawHistory = async () => {
      try {
        const res = await getWithdrawlHistoryByAdmin({
          requestTrace: "admin-withdraw-history",
          requestDateTime: new Date().toISOString(),
        });
        setWithdrawHistory(res.response || []);
      } catch (error) {
        console.error("Error fetching withdraw history:", error);
      }
    };

    //  Fetch user count
    const fetchUserCount = async () => {
      try {
        const res = await countUsers({
          requestTrace: "dashboard-user-count",
          requestDateTime: new Date().toISOString(),
        });
        setUserCount(res);
      } catch (error) {
        console.error("Error fetching user count:", error);
      }
    }

    fetchElectricVehicleCount();
    fetchWalletTransactionCount();
    fetchWithdrawHistory();
    fetchUserCount();
  }, []);

  return (
    <Box m="20px">
      {/* HEADER */}
      <Box display="flex" justifyContent="space-between" alignItems="center">
        <Header title="DASHBOARD" subtitle="Welcome to your dashboard" />
      </Box>

      {/* GRID & CHARTS */}
      <Box
        display="grid"
        gridTemplateColumns="repeat(12, 1fr)"
        gridAutoRows="140px"
        gap="20px"
      >
        {/* ROW 1 - Overview Cards */}
        {/* Reports card with live data */}
        <Box
          gridColumn="span 3"
          backgroundColor={colors.primary[400]}
          display="flex"
          alignItems="center"
          justifyContent="center"
        >
          <StatBox
            title="1,254"
            subtitle="Reports"
            icon={<AssessmentOutlinedIcon sx={{ color: colors.greenAccent[600], fontSize: "26px" }} />}
          />
        </Box>
        
        {/* Withdrawal transactions card with live data */}
        <Box
          gridColumn="span 3"
          backgroundColor={colors.primary[400]}
          display="flex"
          alignItems="center"
          justifyContent="center"
        >
          <StatBox
            title={Number(walletTransactionCount || 0).toLocaleString()}
            subtitle="Wallet Transactions"
            icon={<PointOfSaleOutlinedIcon sx={{ color: colors.greenAccent[600], fontSize: "26px" }} />}
          />
        </Box>

        {/* Users card with live data */}
        <Box
          gridColumn="span 3"
          backgroundColor={colors.primary[400]}
          display="flex"
          alignItems="center"
          justifyContent="center"
        >
          <StatBox
            title={userCount.toLocaleString()}
            subtitle="Users"
            icon={<PersonAddIcon sx={{ color: colors.greenAccent[600], fontSize: "26px" }} />}
          />
        </Box>

        <Box
          gridColumn="span 3"
          backgroundColor={colors.primary[400]}
          display="flex"
          alignItems="center"
          justifyContent="center"
        >
          <StatBox
            title={electricVehicleCount.toLocaleString()}
            subtitle="Electric-Vehicles"
            icon={<ElectricCarIcon sx={{ color: colors.greenAccent[600], fontSize: "26px" }} />}
          />
        </Box>

        {/* ROW 2 */}
        <Box
          gridColumn="span 8"
          gridRow="span 2"
          backgroundColor={colors.primary[400]}
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
              <Typography variant="h3" fontWeight="bold" color={colors.greenAccent[500]}>
                9,345
              </Typography>
            </Box>
          </Box>
          <Box height="250px" m="-20px 0 0 0">
            <LineChart isDashboard={true} />
          </Box>
        </Box>

        {/* Recent Transactions / Withdrawal History */}
        <Box
          gridColumn="span 4"
          gridRow="span 2"
          backgroundColor={colors.primary[400]}
          overflow="auto"
        >
          <Box
            display="flex"
            justifyContent="space-between"
            alignItems="center"
            borderBottom={`4px solid ${colors.primary[500]}`}
            colors={colors.grey[100]}
            p="15px"
          >
            <Typography color={colors.grey[100]} variant="h5" fontWeight="600">
              Recent Transactions
            </Typography>
          </Box>

          {withdrawHistory.length > 0 ? (
            withdrawHistory.map((item) => (
              <Box
                key={item.id}
                display="flex"
                justifyContent="space-between"
                alignItems="center"
                borderBottom={`4px solid ${colors.primary[500]}`}
                p="15px"
              >
                <Box>
                  <Typography color={colors.greenAccent[500]} variant="h5" fontWeight="600">
                    {item.user?.email || "Unknown User"}
                  </Typography>
                  <Typography color={colors.grey[100]}>#{item.id}</Typography>
                </Box>
                <Box color={colors.grey[100]}>
                  {item.processedAt ? new Date(item.processedAt).toLocaleString() : "-"}
                </Box>
                <Box
                  backgroundColor={colors.greenAccent[500]}
                  p="5px 10px"
                  borderRadius="4px"
                >
                  ${item.amount?.toLocaleString() || 0}
                </Box>
              </Box>
            ))
          ) : (
            <Typography textAlign="center" p="20px" color={colors.grey[300]}>
              No withdrawal history found.
            </Typography>
          )}
        </Box>

        {/* ROW 3 */}
        <Box
          gridColumn="span 4"
          gridRow="span 2"
          backgroundColor={colors.primary[400]}
          p="30px"
        >
          <Typography variant="h5" fontWeight="600">
            Setting
          </Typography>
          <Box display="flex" flexDirection="column" alignItems="center" mt="25px">
            <ProgressCircle size="125" />
            <Typography variant="h5" color={colors.greenAccent[500]} sx={{ mt: "15px" }}>
              Modules are active (Active Features) – 78%
            </Typography>
            <Typography>
              Modules are under maintenance / temporarily turned off (Under Maintenance) – 22%
            </Typography>
          </Box>
        </Box>

        <Box
          gridColumn="span 4"
          gridRow="span 2"
          backgroundColor={colors.primary[400]}
        >
          <Typography variant="h5" fontWeight="600" sx={{ padding: "30px 30px 0 30px" }}>
            Credit Status
          </Typography>
          <Box height="250px" mt="-20px">
            <BarChart isDashboard={true} />
          </Box>
        </Box>

        <Box
          gridColumn="span 4"
          gridRow="span 2"
          backgroundColor={colors.primary[400]}
          padding="30px"
        >
          <Typography variant="h5" fontWeight="600" sx={{ marginBottom: "15px" }}>
            Geography Based Carbon Credit
          </Typography>
          <Box height="200px">
            <GeographyChart isDashboard={true} />
          </Box>
        </Box>
      </Box>
    </Box>
  );
};

export default Dashboard;
