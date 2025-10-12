import React from "react";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap-icons/font/bootstrap-icons.css";
import "./wallet.css";
import { useNavigate } from "react-router-dom";

export default function Wallet() {
  const nav = useNavigate();
  const transactions = [
    { id: 1, type: "BUY credits", amount: -500, date: "2025-10-09" },
    { id: 2, type: "SELL credits", amount: 99, date: "2025-10-09" },
  ];

  return (
    <div className="auth-hero wallet-page d-flex flex-column align-items-center py-5">
      {/*header */}
      <div className="text-center mb-4">
        <div
          className="d-flex justify-content-center align-items-center gap-2 mb-2"
          style={{ marginTop: "50px" }}
        >
          <i className="bi bi-wallet2 fs-3 text-accent"></i>
          <h3 className="fw-bold text-light mb-0">My Wallet</h3>
        </div>
      </div>

      {/*balance card */}
      <div className="wallet-card glass-card text-center p-4 mb-5">
        <h6 className="mb-2">Balance:</h6>
        <h2 className="display-6 fw-bold text-accent mb-4">
          <i className="bi bi-currency-dollar"></i>
          9,599.98
        </h2>

        <div className="d-flex justify-content-center gap-3 flex-wrap">
          <button className="wallet-action border-success text-success">
            <i className="bi bi-upload fs-5"></i>
            <span>Add Money</span>
          </button>
          <button className="wallet-action border-warning text-warning">
            <i className="bi bi-download fs-5"></i>
            <span>Withdraw</span>
          </button>
          <button className="wallet-action border-info text-info">
            <i className="bi bi-shuffle fs-5"></i>
            <span>Transfer</span>
          </button>
        </div>
      </div>

      {/*history */}
      <div className="wallet-history">
        <div className="d-flex justify-content-between align-items-center mb-3">
          <h5 className="text-light mb-0">
            History
            <i className="bi bi-arrow-clockwise small ms-2"></i>
          </h5>
        </div>

        <div className="history-list p-3">
          {transactions.map((tx) => (
            <div
              key={tx.id}
              className="d-flex justify-content-between align-items-center history-item"
            >
              <div>
                <span className="fw-semibold text-light">{tx.type}</span>
                <div className="small">{tx.date}</div>
              </div>
              <span
                className={`fw-bold ${
                  tx.amount > 0 ? "text-success" : "text-danger"
                }`}
              >
                {tx.amount > 0 ? `+${tx.amount} USD` : `${tx.amount} USD`}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
