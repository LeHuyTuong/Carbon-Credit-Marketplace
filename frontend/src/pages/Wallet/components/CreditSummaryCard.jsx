import React from "react";
import "../wallet.css";

export default function CreditSummaryCard({ summary }) {
  // nếu chưa có dữ liệu summary → hiển thị trạng thái đang tải
  if (!summary)
    return (
      <div className="text-center text-light mt-4">Loading summary...</div>
    );

  // lấy số lượng credits có trạng thái "LISTED" từ mảng byStatus
  const listedCount =
    summary.byStatus?.find((s) => s.status === "LISTED")?.count || 0;

  // định nghĩa danh sách các loại credit cần hiển thị trên thẻ
  const items = [
    { label: "Total", value: summary.total || 0, color: "#ffb1b1ff" },
    { label: "Issued", value: summary.issued || 0, color: "#00ffc8" },
    { label: "Available", value: summary.available || 0, color: "#1bb7cf" },
    { label: "Retired", value: summary.retired || 0, color: "#c484d4" },
    {
      label: "Listed",
      value: `${listedCount}`,
      color: "#ffc107",
    },
  ];

  // render giao diện dạng lưới hiển thị tóm tắt các loại credit
  return (
    <div className="credit-summary-grid">
      {items.map((item, i) => (
        <div key={i} className="credit-summary-card">
          {/* hiển thị số lượng và đơn vị credits */}
          <h3 style={{ color: item.color }}>
            {item.value} <span>credits</span>
          </h3>
          {/* hiển thị nhãn tên loại */}
          <p>{item.label}</p>
        </div>
      ))}
    </div>
  );
}
