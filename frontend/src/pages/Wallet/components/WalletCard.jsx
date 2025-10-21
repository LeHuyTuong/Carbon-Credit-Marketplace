import React from "react";
import { Button } from "react-bootstrap";
import "../wallet.css";

export default function WalletCard({
  balance,
  currency = "USD",
  onDeposit,
  onWithdraw,
  loading,
}) {
  const exchangeRate = 27396; //giá 1$

  const formatCurrency = (amount, cur) => {
    if (amount === undefined || amount === null) return "0";
    return amount.toLocaleString(cur === "USD" ? "en-US" : "vi-VN", {
      style: "currency",
      currency: cur,
      minimumFractionDigits: 2,
    });
  };

  // Tính quy đổi nếu là USD
  const convertedToVND =
    currency === "USD" && balance ? balance * exchangeRate : null;

  return (
    <div className="wallet-card glass-card text-center p-4 mb-5">
      <h6 className="mb-2">Balance:</h6>

      <h2 className="display-6 fw-bold text-accent mb-2">
        {formatCurrency(balance, currency)}
      </h2>

      {convertedToVND && (
        <p className="text-light mb-4">
          ≈ {convertedToVND.toLocaleString("vi-VN")} VND
        </p>
      )}

      <div className="d-flex justify-content-center gap-3 flex-wrap">
        <button
          className="wallet-action border-info text-info"
          onClick={onDeposit}
          disabled={loading}
        >
          <i className="bi bi-upload fs-5"></i>
          <span>{loading ? "Processing..." : "Add Money"}</span>
        </button>
        <button
          className="wallet-action border-warning text-warning"
          onClick={onWithdraw}
          disabled={loading}
        >
          <i className="bi bi-download fs-5"></i>
          <span>{loading ? "Checking..." : "Withdraw"}</span>
        </button>
      </div>
    </div>
  );
}
