import { ResponsivePie } from "@nivo/pie";
import { tokens } from "@/theme";
import { useTheme } from "@mui/material";

const PieChart = ({ data = [] }) => {
  const theme = useTheme();                 // Lấy theme MUI hiện tại (light/dark mode)
  const colors = tokens(theme.palette.mode); // Lấy bộ màu custom theo mode

  return (
    <ResponsivePie
      data={data}                           // Dữ liệu biểu đồ pie
      colors={{ datum: "data.color" }}      // Mỗi phần nhận màu từ trường "color" của data
 
      // Custom Tooltip cho từng phần
      tooltip={({ datum }) => (
        <div
          style={{
            background: "#222",             // Tooltip nền tối
            padding: "8px 12px",            // Khoảng cách bên trong
            borderRadius: "6px",            // Bo góc
            color: "#fff",                  // Chữ trắng
            fontSize: "14px",               // Cỡ chữ
          }}
        >
          <strong>{datum.label}</strong>: {datum.value}
        </div>
      )}

      // Theme override: chỉnh màu text & lines theo theme
      theme={{
        axis: {
          domain: { line: { stroke: colors.grey[100] } },
          legend: { text: { fill: colors.grey[100] } },
          ticks: {
            line: { stroke: colors.grey[100], strokeWidth: 1 },
            text: { fill: colors.grey[100] },
          },
        },
        legends: {
          text: { fill: colors.grey[100] },
        },
      }}

      // Khoảng cách ngoài biểu đồ
      margin={{ top: 40, right: 80, bottom: 80, left: 80 }}

      innerRadius={0.5}                     // Tạo hiệu ứng donut (0 = pie)
      padAngle={0.7}                        // Khoảng cách giữa các phần
      cornerRadius={3}                      // Bo góc slice
      activeOuterRadiusOffset={8}           // Hover phóng to nhẹ

      // Border mỗi phần
      borderColor={{
        from: "color",
        modifiers: [["darker", 0.2]],
      }}

      // Label trên line
      arcLinkLabelsSkipAngle={10}           // Góc nhỏ sẽ bỏ qua label cho đỡ rối
      arcLinkLabelsTextColor={colors.grey[100]}
      arcLinkLabelsThickness={2}
      arcLinkLabelsColor={{ from: "color" }}

      enableArcLabels={false}               // Tắt label trực tiếp trên slice
      arcLabelsRadiusOffset={0.55}
      arcLabelsSkipAngle={7}
      arcLabelsTextColor={{
        from: "color",
        modifiers: [["darker", 3]],
      }}

      // Pattern (nếu dùng) cho một số phần slice
      defs={[
        {
          id: "dots",
          type: "patternDots",
          background: "inherit",
          color: "rgba(255, 255, 255, 0.3)",
          size: 4,
          padding: 1,
          stagger: true,
        },
        {
          id: "lines",
          type: "patternLines",
          background: "inherit",
          color: "rgba(255, 255, 255, 0.3)",
          rotation: -45,
          lineWidth: 6,
          spacing: 10,
        },
      ]}

      // Legend phía dưới
      legends={[
        {
          anchor: "bottom",                 // Vị trí bottom
          direction: "row",                 // Hiển thị theo hàng ngang
          justify: false,
          translateX: 0,
          translateY: 56,
          itemsSpacing: 20,
          itemWidth: 100,
          itemHeight: 18,
          itemTextColor: "#999",
          itemDirection: "left-to-right",
          itemOpacity: 1,
          symbolSize: 18,                   // Icon tròn
          symbolShape: "circle",
          effects: [
            {
              on: "hover",
              style: { itemTextColor: "#000" }, // Hover đổi màu text
            },
          ],
        },
      ]}
    />
  );
};

export default PieChart;
