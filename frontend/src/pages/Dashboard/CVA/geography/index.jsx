import { Box, useTheme } from "@mui/material";
import GeographyChart from "@/components/Chart/GeographyChart.jsx";
import Header from "@/components/Chart/Header.jsx";
import { tokens } from "@/themeCVA";

const Geography = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  return (
    <Box m="20px">
      <Header title="Geography" subtitle="Simple Geography Chart" />

      <Box
        height="75vh"
        border={`1px solid ${colors.grey[100]}`}
        borderRadius="4px"
      >
        <GeographyChart />
      </Box>
    </Box>
  );
};

export default Geography;
