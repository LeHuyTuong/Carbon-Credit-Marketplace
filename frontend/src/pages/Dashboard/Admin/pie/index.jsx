import { Box } from "@mui/material";
import Header from "@/components/Chart/Header.jsx";
import PieChart from "@/components/Chart/PieChart.jsx";

const Pie = () => {
  return (
    <Box m="20px">
      <Header title="Pie Chart" subtitle="Simple Pie Chart" />
      <Box height="75vh">
        <PieChart />
      </Box>
    </Box>
  );
};

export default Pie;
