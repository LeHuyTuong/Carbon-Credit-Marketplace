import React, { useEffect, useState } from "react";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap-icons/font/bootstrap-icons.css";
import { Button } from "react-bootstrap";
import { apiFetch } from "../../../utils/apiFetch";
import { useNavigate } from "react-router-dom";
import { FaArrowLeft } from "react-icons/fa";

export default function WalletHistory() {
  const nav = useNavigate();
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(false);

  //khi component mount -> gọi API để load lịch sử giao dịch
  useEffect(() => {
    fetchTransactions();
  }, []);

  const fetchTransactions = async () => {
    setLoading(true);
    try {
      const res = await apiFetch("/api/v1/wallet/transactions", {
        method: "GET",
      });

      //sắp xếp giao dịch mới nhất lên đầu (theo createdAt)
      const sorted = [...(res.response || [])].sort(
        (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
      );
      setTransactions(sorted);
    } catch (err) {
      console.error("Failed to fetch transactions:", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-hero2 wallet-page d-flex flex-column align-items-center py-5">
      {/*nút back */}
      <Button
        variant="outline-info"
        size="sm"
        className="position-fixed top-0 start-0 m-3 px-3 py-2 d-flex align-items-center gap-2 fw-semibold shadow-sm"
        style={{
          borderRadius: "10px",
          background: "rgba(255, 255, 255, 0.85)",
          backdropFilter: "blur(6px)",
          zIndex: 20,
        }}
        onClick={() => nav("/wallet")}
      >
        <FaArrowLeft /> Back to Wallet
      </Button>
      <div className="text-center mb-4">
        <div className="d-flex justify-content-center align-items-center gap-2 mb-2">
          <i className="bi bi-clock-history fs-3 text-accent"></i>
          <h3 className="fw-bold text-light mb-0">Transaction History</h3>
        </div>
      </div>

      <div className="glass-card p-4 w-75">
        {loading ? (
          <div className="text-light text-center">Loading...</div>
        ) : transactions.length === 0 ? (
          <div className="text-light text-center">No transactions yet</div>
        ) : (
          //có giao dịch -> render danh sách
          transactions.map((tx) => (
            <div
              key={tx.id}
              className="d-flex justify-content-between align-items-center border-bottom py-2"
            >
              <div>
                <span
                  className={`fw-semibold ${
                    tx.transactionType === "ADD_MONEY"
                      ? "text-info"
                      : tx.transactionType === "WITH_DRAWL"
                      ? "text-warning"
                      : "text-info"
                  }`}
                >
                  {tx.transactionType === "WITH_DRAWL"
                    ? "Withdraw"
                    : tx.transactionType === "DEPOSIT"
                    ? "Deposit"
                    : tx.transactionType}
                </span>
                <div className="small text-light">
                  {tx.createdAt ? new Date(tx.createdAt).toLocaleString() : ""}
                </div>
              </div>
              <span
                className={`fw-bold ${
                  tx.transactionType === "ADD_MONEY"
                    ? "text-success"
                    : "text-danger"
                }`}
              >
                {tx.transactionType === "ADD_MONEY"
                  ? `+${tx.amount} USD`
                  : `-${tx.amount} USD`}
              </span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
