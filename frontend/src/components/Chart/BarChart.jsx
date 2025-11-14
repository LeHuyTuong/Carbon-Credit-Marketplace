import React from "react";
import { useTheme } from "@mui/material";
import { ResponsiveBar } from "@nivo/bar";
import { tokens } from "@/theme";

/**
 * BarChart - hiển thị Credit Status
 * @param {Array} data - dữ liệu động từ API (optional)
 * @param {boolean} isDashboard - ẩn legend, label nếu dùng trong dashboard
 */
const BarChart = ({ data = [], keys = [], indexBy = "month", isDashboard = false  }) => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  const colorById = (id) => {
    switch (id) {
      case "active":
        return "#4CAF50";
      case "pending":
        return "#42A5F5";
      case "listed":
        return "#FFCA28";
      case "revoke":
        return "#EF5350";
      case "sold":
        return "#AB47BC";
      case "retire":
        return "#9E9E9E";
      default:
        return "#90A4AE";
    }
  };

  return (
    <ResponsiveBar
      data={data}
      keys={keys}
      indexBy={indexBy}
      margin={{ top: 50, right: 130, bottom: 50, left: 60 }}
      padding={0.3}
      valueScale={{ type: "linear" }}
      indexScale={{ type: "band", round: true }}
      colors={({ id }) => colorById(id)}
      theme={{
        axis: {
          domain: { line: { stroke: colors.grey[100] } },
          legend: { text: { fill: colors.grey[100] } },
          ticks: {
            line: { stroke: colors.grey[100], strokeWidth: 1 },
            text: { fill: colors.grey[100] },
          },
        },
        legends: { text: { fill: colors.grey[100] } },
      }}
      tooltip={({ id, value, color, indexValue }) => (
        <div
          style={{
            background: "#222",
            padding: "8px 12px",
            borderRadius: "6px",
            color: "#fff",
            fontSize: "14px",
            display: "flex",
            alignItems: "center",
            gap: "8px",
          }}
        >
          <div
            style={{
              width: "12px",
              height: "12px",
              borderRadius: "50%",
              background: color,
            }}
          ></div>
          <div>
            <strong>{id}</strong> — {indexValue}: {value}
          </div>
        </div>
      )}
      borderColor={{ from: "color", modifiers: [["darker", "1.6"]] }}
      axisTop={null}
      axisRight={null}
      axisBottom={{
        tickSize: 5,
        tickPadding: 5,
        legend: isDashboard ? undefined : "month",
        legendPosition: "middle",
        legendOffset: 32,
      }}
      axisLeft={{
        tickSize: 5,
        tickPadding: 5,
        legend: isDashboard ? undefined : "credit",
        legendPosition: "middle",
        legendOffset: -40,
      }}
      enableLabel={false}
      legends={[
        {
          dataFrom: "keys",
          anchor: "bottom-right",
          direction: "column",
          translateX: 120,
          itemsSpacing: 2,
          itemWidth: 100,
          itemHeight: 20,
          itemOpacity: 0.85,
          symbolSize: 18,
        },
      ]}
      role="application"
      barAriaLabel={(e) =>
        `${e.id}: ${e.formattedValue} in month: ${e.indexValue}`
      }
    />
  );
};

export default BarChart;
