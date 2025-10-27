import React from "react";

export default function CreditSummaryCard({ summary }) {
  if (!summary) {
    return (
      <div className="text-center text-light mt-4">Loading summary...</div>
    );
  }

  const items = [
    {
      label: "Available (Wallet)",
      value: summary.available || 0,
      color: "#00ffc8",
    },
    { label: "Issued", value: summary.issued || 0, color: "#1bb7cfff" },
    { label: "Retired", value: summary.retired || 0, color: "#c484d4ff" },
    {
      label: "Listed / Sold",
      value: `${summary.listed || 0} / ${summary.sold || 0}`,
      color: "#ffc107",
    },
  ];

  return (
    <div className="credit-summary-grid mt-4 mb-5">
      {items.map((item, i) => (
        <div key={i} className="credit-summary-card glass-card text-center p-3">
          <h3 style={{ color: item.color }}>
            {item.value} <span style={{ fontSize: "1rem" }}>CC</span>
          </h3>
          <p className="text-light small">{item.label}</p>
        </div>
      ))}
    </div>
  );
}
