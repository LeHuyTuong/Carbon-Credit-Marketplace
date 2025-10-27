import React, { useEffect, useState, useRef } from "react";
import { Button, Nav } from "react-bootstrap";
import { FaArrowLeft } from "react-icons/fa";
import { apiFetch } from "../../../utils/apiFetch";
import { useNavigate } from "react-router-dom";
import PaginatedList from "../../../components/Pagination/PaginatedList";
import useReveal from "../../../hooks/useReveal";

export default function WalletHistory() {
  const nav = useNavigate();
  const [tab, setTab] = useState("transactions");
  const [transactions, setTransactions] = useState([]);
  const [withdrawals, setWithdrawals] = useState([]);
  const [loading, setLoading] = useState(false);
  const sectionRef = useRef(null);
  useReveal(sectionRef);

  useEffect(() => {
    if (tab === "transactions") fetchTransactions();
    if (tab === "withdrawals") fetchWithdrawals();
  }, [tab]);

  //lịch sử giao dịch
  const fetchTransactions = async () => {
    setLoading(true);
    try {
      const res = await apiFetch("/api/v1/wallet/transactions", {
        method: "GET",
      });
      //sort hiện cái mới nhất
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

  //ls yêu cầu rút tiền
  const fetchWithdrawals = async () => {
    setLoading(true);
    try {
      const res = await apiFetch("/api/v1/withdrawal", { method: "GET" });
      setWithdrawals(res.response || []);
    } catch (err) {
      console.error("Failed to fetch withdrawals:", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      ref={sectionRef}
      className="auth-hero2 wallet-page d-flex flex-column align-items-center py-5 reveal"
    >
      <Button
        variant="outline-info"
        size="sm"
        className="position-fixed top-0 start-0 m-3 px-3 py-2 d-flex align-items-center gap-2 fw-semibold shadow-sm"
        style={{
          borderRadius: "10px",
          background: "rgba(255,255,255,0.85)",
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

      <Nav
        variant="tabs"
        className="mb-4 bg-dark px-3 py-2 rounded"
        activeKey={tab}
        onSelect={(selected) => setTab(selected)}
      >
        <Nav.Item>
          <Nav.Link eventKey="transactions" className="text-light">
            Transactions
          </Nav.Link>
        </Nav.Item>
        <Nav.Item>
          <Nav.Link eventKey="withdrawals" className="text-light">
            Withdrawal Requests
          </Nav.Link>
        </Nav.Item>
      </Nav>

      <div className="glass-card p-4 w-75">
        {loading ? (
          <div className="text-light text-center">Loading...</div>
        ) : tab === "transactions" ? (
          transactions.length === 0 ? (
            <div className="text-light text-center">No transactions yet</div>
          ) : (
            <PaginatedList
              items={transactions}
              itemsPerPage={5}
              renderItem={(tx) => {
                const incomeTypes = ["ADD_MONEY", "SELL_CARBON_CREDIT"];
                const expenseTypes = ["WITHDRAWAL", "BUY_CARBON_CREDIT"];
                const isIncome = incomeTypes.includes(tx.transactionType);
                const isExpense = expenseTypes.includes(tx.transactionType);
                const typeClass = isIncome
                  ? "text-success"
                  : isExpense
                  ? "text-warning"
                  : "text-info";
                const amountClass = isIncome
                  ? "text-success"
                  : isExpense
                  ? "text-danger"
                  : "text-light";
                const prefix = isIncome ? "+" : isExpense ? "-" : "";

                return (
                  <div
                    key={tx.id}
                    className="d-flex justify-content-between align-items-center border-bottom py-2"
                  >
                    <div>
                      <span
                        className={`fw-semibold ${typeClass}`}
                      >
                        {tx.transactionType}
                      </span>
                      <div className="small text-light">
                        {new Date(tx.createdAt).toLocaleString("vi-VN", {
                          timeZone: "Asia/Ho_Chi_Minh",
                          hour12: false,
                        })}
                      </div>
                    </div>
                    <span className={`fw-bold ${amountClass}`}>
                      {`${prefix}${tx.amount} USD`}
                    </span>
                  </div>
                );
              }}

            />
          )
        ) : withdrawals.length === 0 ? (
          <div className="text-light text-center">
            No withdrawal requests yet
          </div>
        ) : (
          <PaginatedList
            items={withdrawals}
            itemsPerPage={5}
            renderItem={(w) => (
              <div
                key={w.id}
                className="d-flex justify-content-between align-items-center border-bottom py-2"
              >
                <div>
                  <span className="fw-semibold text-info">Request #{w.id}</span>
                  <div className="small text-light">
                    {new Date(w.createdAt || w.requestedAt).toLocaleString(
                      "vi-VN",
                      {
                        timeZone: "Asia/Ho_Chi_Minh",
                        hour12: false,
                      }
                    )}
                  </div>
                </div>
                <span
                  className={`badge ${
                    w.status === "SUCCEEDED"
                      ? "bg-success"
                      : w.status === "REJECTED"
                      ? "bg-danger"
                      : "bg-warning text-dark"
                  }`}
                >
                  {w.status}
                </span>
                <span className="fw-bold text-light">{w.amount} USD</span>
              </div>
            )}
          />
        )}
      </div>
    </div>
  );
}
