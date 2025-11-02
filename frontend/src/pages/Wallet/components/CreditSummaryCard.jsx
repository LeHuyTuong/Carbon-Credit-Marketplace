import React from "react";
import "../wallet.css";

export default function CreditSummaryCard({ summary }) {
  if (!summary)
    return (
      <div className="text-center text-light mt-4">Loading summary...</div>
    );

  const listedCount =
    summary.byStatus?.find((s) => s.status === "LISTED")?.count || 0;
  const soldCount = summary.sold || 0;

  const items = [
    { label: "Total", value: summary.total || 0, color: "#ffb1b1ff" },
    { label: "Issued", value: summary.issued || 0, color: "#00ffc8" },
    { label: "Available", value: summary.available || 0, color: "#1bb7cf" },
    { label: "Retired", value: summary.retired || 0, color: "#c484d4" },
    {
      label: "Listed / Sold",
      value: `${listedCount} / ${soldCount}`,
      color: "#ffc107",
    },
  ];

  return (
    <div className="credit-summary-grid">
      {items.map((item, i) => (
        <div key={i} className="credit-summary-card">
          <h3 style={{ color: item.color }}>
            {item.value} <span>credits</span>
          </h3>
          <p>{item.label}</p>
        </div>
      ))}
    </div>
  );
}
