import React, { useEffect, useRef, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { apiFetch } from "../../utils/apiFetch";
import useWalletData from "./components/useWalletData";
import WalletCard from "./components/WalletCard";
import WalletToast from "./components/WalletToast";
import Deposit from "./Deposit/Deposit";
import Withdraw from "./Withdraw/Withdraw";
import useReveal from "../../hooks/useReveal";
import CreditsList from "./components/CreditsList";
import CreditSummaryCard from "./components/CreditSummaryCard";
import { useAuth } from "../../context/AuthContext";
import ChooseReportModal from "./components/ChooseReportModal";

export default function Wallet() {
  const nav = useNavigate();
  const location = useLocation();
  const sectionRef = useRef(null);
  useReveal(sectionRef);
  const { primaryRole } = useAuth();
  const isEVOwner = primaryRole === "EV_OWNER";

  // dùng hook chính
  const {
    wallet,
    transactions,
    issuedCredits,
    purchasedCredits,
    summary,
    loading,
    shareProfit,
    setLoading,
    fetchWallet,
    fetchTransactions,
    fetchIssuedCredits,
    fetchPurchasedCredits,
    fetchSummary,
    fetchApprovedReports,
  } = useWalletData();

  const [toast, setToast] = React.useState({
    show: false,
    msg: "",
    type: "success",
  });
  const [showDepositModal, setShowDepositModal] = React.useState(false);
  const [showWithdrawModal, setShowWithdrawModal] = React.useState(false);
  const [showPaymentToast, setShowPaymentToast] = React.useState(false);
  const [activeTab, setActiveTab] = React.useState("issued");
  const [showShareModal, setShowShareModal] = useState(false);
  const [approvedReports, setApprovedReports] = useState([]);

  //khi trở lại từ Order.jsx với state.refreshCredits
  useEffect(() => {
    if (location.state?.refreshCredits) {
      console.log("Refreshing credits after purchase...");
      fetchIssuedCredits();
      fetchPurchasedCredits();
      nav("/wallet", { replace: true });
    }
  }, [location.state]);

  //load trang: check nếu có order_id & payment_id
  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const orderId = params.get("order_id");
    const paymentId = params.get("payment_id");

    const authData =
      JSON.parse(sessionStorage.getItem("auth")) ||
      JSON.parse(localStorage.getItem("auth"));
    const token = authData?.token;

    if (!token) {
      console.warn("Token is not ready, delay confirm...");
      const timer = setTimeout(() => {
        if (orderId) confirmDeposit(orderId, paymentId || "");
        else {
          fetchWallet();
          fetchIssuedCredits();
          fetchPurchasedCredits();
          fetchSummary();
        }
      }, 1000);
      return () => clearTimeout(timer);
    }

    if (orderId) confirmDeposit(orderId, paymentId || "");
    else {
      fetchWallet();
      fetchIssuedCredits();
      fetchPurchasedCredits();
      fetchSummary();
    }
  }, [location.search]);

  //khi ví có ID -> lấy lịch sử giao dịch
  useEffect(() => {
    if (wallet?.id) fetchTransactions();
  }, [wallet]);

  //handle add money
  const handleDepositSubmit = async (values) => {
    setLoading(true);
    try {
      const res = await apiFetch("/api/v1/payment", {
        method: "POST",
        body: {
          data: {
            amount: parseFloat(values.amount),
            paymentMethod: values.paymentMethod,
          },
        },
      });

      const paymentUrl = res?.response?.payment_url;
      if (paymentUrl) {
        window.open(paymentUrl, "_blank");
        setToast({
          show: true,
          msg: "Redirecting to payment gateway...",
          type: "success",
        });
      } else {
        setToast({
          show: true,
          msg: "No payment URL returned from server.",
          type: "warning",
        });
      }
    } catch (err) {
      console.error("Deposit error:", err);
      setToast({
        show: true,
        msg: err.message || "Failed to initiate deposit.",
        type: "danger",
      });
    } finally {
      setLoading(false);
      setShowDepositModal(false);
    }
  };

  //confirm payment
  const confirmDeposit = async (orderId, paymentId) => {
    setLoading(true);
    try {
      await apiFetch(
        `/api/v1/wallet/deposit?order_id=${orderId}&payment_id=${paymentId}`,
        { method: "POST" }
      );

      await Promise.all([
        fetchWallet(),
        fetchTransactions(),
        fetchIssuedCredits(),
        fetchPurchasedCredits(),
      ]);

      setToast({
        show: true,
        msg: "Deposit successful! Balance updated.",
        type: "success",
      });

      nav("/wallet", { replace: true });
    } catch (err) {
      console.error("Deposit confirmation failed:", err);
      setToast({
        show: true,
        msg: err.message || "Failed to confirm deposit.",
        type: "danger",
      });
    } finally {
      setLoading(false);
    }
  };

  //handle withdraw
  const handleWithdrawSubmit = async (values) => {
    setLoading(true);
    try {
      const res = await apiFetch(`/api/v1/withdrawal/${values.amount}`, {
        method: "POST",
      });

      if (res?.response) {
        setToast({
          show: true,
          msg: "Withdrawal request submitted. Awaiting admin approval.",
          type: "info",
        });
        await Promise.all([fetchWallet(), fetchTransactions()]);
      } else {
        throw new Error(
          res?.responseStatus?.responseMessage || "Withdrawal failed."
        );
      }
    } catch (err) {
      console.error("Withdraw error:", err);
      setToast({
        show: true,
        msg: err.message || "Unable to process withdrawal request.",
        type: "danger",
      });
    } finally {
      setLoading(false);
      setShowWithdrawModal(false);
    }
  };

  const openShareModal = async () => {
    const list = await fetchApprovedReports(); // lấy report đã duyệt
    setApprovedReports(list);
    setShowShareModal(true);
  };

  return (
    <div
      ref={sectionRef}
      className="auth-hero2 wallet-page reveal d-flex flex-column align-items-center py-5"
    >
      {/* Header */}
      <div className="text-center mb-4">
        <div
          className="d-flex justify-content-center align-items-center gap-2 mb-2"
          style={{ marginTop: "50px" }}
        >
          <i className="bi bi-wallet2 fs-3 text-accent"></i>
          <h3 className="fw-bold text-light mb-0">My Wallet</h3>
        </div>
      </div>

      {/* Balance Card */}
      <WalletCard
        balance={wallet?.balance}
        currency={wallet?.currency || "USD"}
        onDeposit={!isEVOwner ? () => setShowDepositModal(true) : undefined} //ẩn nút nạp của ev
        onWithdraw={() => setShowWithdrawModal(true)}
        loading={loading}
      />

      {/* Buttons */}
      <div className="wallet-history-btn my-3 d-flex flex-column align-items-center gap-2">
        {/* Hàng đầu: 2 nút lịch sử */}
        <div className="d-flex flex-wrap justify-content-center gap-2">
          <button
            className="btn btn-outline-light btn-sm d-flex align-items-center gap-2"
            onClick={() => nav("/transaction-history")}
          >
            <i className="bi bi-clock-history"></i>
            Transaction History
          </button>

          {!isEVOwner && (
            <button
              className="btn btn-outline-info btn-sm d-flex align-items-center gap-2"
              onClick={() =>
                nav("/purchase-history", { state: { from: "wallet" } })
              }
            >
              <i className="bi bi-bag-check"></i>
              Purchases History
            </button>
          )}
        </div>

        {/*nút chia lợi nhuận*/}
        {!isEVOwner && (
          <button
            className="btn btn-success d-flex align-items-center gap-2 mt-2"
            onClick={openShareModal}
            disabled={loading}
          >
            <i className="bi bi-cash-coin"></i>
            Distribute Profit to EV Owners
          </button>
        )}
      </div>

      {/*chỉ company thấy credits */}
      {!isEVOwner && (
        <>
          {/* Credit Summary */}
          <CreditSummaryCard summary={summary} />

          {/* Tabs for Issued / Purchased */}
          <div className="wallet-credits-tabs mt-0 w-100">
            <div className="d-flex justify-content-center mb-3">
              <div className="btn-group">
                <button
                  className={`btn ${
                    activeTab === "issued" ? "btn-accent" : "btn-outline-accent"
                  }`}
                  onClick={() => setActiveTab("issued")}
                >
                  Issued Credits
                </button>
                <button
                  className={`btn ${
                    activeTab === "purchased"
                      ? "btn-accent"
                      : "btn-outline-accent"
                  }`}
                  onClick={() => setActiveTab("purchased")}
                >
                  Purchased Credits
                </button>
              </div>
            </div>

            <div className="tab-content">
              {activeTab === "issued" ? (
                <CreditsList credits={issuedCredits} />
              ) : (
                <CreditsList credits={purchasedCredits} />
              )}
            </div>
          </div>
        </>
      )}
      {/* Deposit Modal */}
      {!isEVOwner && (
        <Deposit
          show={showDepositModal}
          onHide={() => setShowDepositModal(false)}
          onSubmit={handleDepositSubmit}
        />
      )}

      {/* Withdraw Modal */}
      <Withdraw
        show={showWithdrawModal}
        onHide={() => setShowWithdrawModal(false)}
        onSubmit={handleWithdrawSubmit}
        wallet={wallet}
      />

      {/* Toast */}
      <WalletToast
        toast={toast}
        setToast={setToast}
        showPaymentToast={showPaymentToast}
        setShowPaymentToast={setShowPaymentToast}
        nav={nav}
      />
      <ChooseReportModal
        show={showShareModal}
        onHide={() => setShowShareModal(false)}
        onConfirm={async (data) => {
          try {
            const res = await shareProfit({
              ...data,
              description: "Profit sharing initiated by company",
            });
            setToast({
              show: true,
              msg: res?.responseMessage || "Profit shared successfully!",
              type: "success",
            });
            await fetchWallet();
            setShowShareModal(false);
          } catch (err) {
            setToast({
              show: true,
              msg: err.message || "Profit sharing failed.",
              type: "danger",
            });
          }
        }}
      />
    </div>
  );
}
