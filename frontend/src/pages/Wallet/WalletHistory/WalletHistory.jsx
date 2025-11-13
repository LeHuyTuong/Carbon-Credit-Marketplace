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

  const parseVNTime = (ts) => {
    const normalized = ts.endsWith("Z") ? ts : `${ts}Z`; // ép UTC
    const date = new Date(normalized);
    return date
      .toLocaleString("vi-VN", {
        timeZone: "Asia/Ho_Chi_Minh",
        hour12: false,
      })
      .replace(",", "");
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
                const incomeTypes = [
                  "ADD_MONEY",
                  "SELL_CARBON_CREDIT",
                  "ISSUE_CREDIT",
                ];
                const expenseTypes = [
                  "WITHDRAWAL",
                  "BUY_CARBON_CREDIT",
                  "PROFIT_SHARING",
                ];
                // logic màu số tiền và prefix cho PROFIT_SHARING dựa vào dấu amount
                let isIncome = incomeTypes.includes(tx.transactionType);
                let isExpense = expenseTypes.includes(tx.transactionType);

                // Nếu là profit sharing → override theo dấu amount
                if (tx.transactionType === "PROFIT_SHARING") {
                  if (tx.amount >= 0) {
                    isIncome = true; // màu xanh + prefix +
                    isExpense = false;
                  } else {
                    isIncome = false;
                    isExpense = true; // màu đỏ + prefix -
                  }
                }
                // Màu chữ của transactionType
                let typeClass = "text-info"; // mặc định xanh dương

                // PROFIT_SHARING và ISSUE_CREDIT luôn xanh dương
                if (
                  tx.transactionType === "PROFIT_SHARING" ||
                  tx.transactionType === "ISSUE_CREDIT"
                ) {
                  typeClass = "text-info";
                } else {
                  // income → xanh lá
                  if (isIncome) typeClass = "text-success";
                  // expense → vàng (theo logic cũ bạn dùng text-warning)
                  if (isExpense) typeClass = "text-warning";
                }

                // Màu số tiền
                const amountClass = isIncome
                  ? "text-success"
                  : isExpense
                  ? "text-danger"
                  : "text-light";

                // Prefix: BE không trả dấu nên front tự thêm
                const prefix = isIncome ? "+" : isExpense ? "-" : "";

                //nếu là ISSUE_CREDIT thì hiển thị credits thay vì USD
                const unit =
                  tx.transactionType === "ISSUE_CREDIT" ? "credits" : "USD";

                return (
                  <div
                    key={tx.id}
                    className="d-flex justify-content-between align-items-center border-bottom py-2"
                  >
                    <div>
                      <span className={`fw-semibold ${typeClass}`}>
                        {tx.transactionType}
                      </span>
                      <div className="small text-light">
                        {parseVNTime(tx.createdAt)}
                      </div>

                      {/*nếu là Profit Sharing → show nút View Details */}
                      {tx.transactionType === "PROFIT_SHARING" &&
                        (() => {
                          //tìm pattern "(distribution id)" trong description
                          const match = tx.description?.match(
                            /distribution\s+#(\d+)/i
                          );
                          const distId = match ? match[1] : null;
                          return (
                            distId && (
                              <Button
                                variant="outline-info"
                                size="sm"
                                className="mt-2"
                                onClick={() => nav(`/payout/review/${distId}`)}
                              >
                                View Payout Details
                              </Button>
                            )
                          );
                        })()}
                    </div>
                    <div className="text-end">
                      <div className={`fw-bold ${amountClass}`}>
                        {`${prefix}${tx.amount} ${unit}`}
                      </div>

                      <div className="small text-light">
                        <span className="opacity-75">Before:</span>{" "}
                        {tx.balanceBefore}
                      </div>

                      <div className="small text-light">
                        <span className="opacity-75">After:</span>{" "}
                        {tx.balanceAfter}
                      </div>
                    </div>
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
