import { Box } from "@mui/material";
import Header from "@/components/Chart/Header.jsx";
import BarChart from "@/components/Chart/BarChart.jsx";

const Bar = () => {
  return (
    <Box m="20px">
      <Header title="Bar Chart" subtitle="Simple Bar Chart" />
      <Box height="75vh">
        <BarChart />
      </Box>
    </Box>
  );
};

export default Bar;
