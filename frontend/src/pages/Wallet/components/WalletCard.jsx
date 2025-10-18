import React from "react";
import { Button } from "react-bootstrap";
import "../wallet.css";

export default function WalletCard({
  balance,
  onDeposit,
  onWithdraw,
  loading,
}) {
  return (
    <div className="wallet-card glass-card text-center p-4 mb-5">
      <h6 className="mb-2">Balance:</h6>
      <h2 className="display-6 fw-bold text-accent mb-4">
        <i className="bi bi-currency-dollar"></i>
        {balance !== undefined && balance !== null
          ? balance.toLocaleString()
          : "0.00"}
      </h2>

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
