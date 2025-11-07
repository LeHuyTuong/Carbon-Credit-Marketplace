import { Box } from "@mui/material";
import Header from "@/components/Chart/Header.jsx";
import PieChart from "@/components/Chart/PieChart.jsx";

const Pie = () => {
  return (
    <Box m="20px" sx={{ marginLeft: "290px" }}>
      <Header title="Pie Chart" subtitle="Simple Pie Chart Of EV_Brand" />
      <Box height="75vh">
        <PieChart />
      </Box>
    </Box>
  );
};

export default Pie;
