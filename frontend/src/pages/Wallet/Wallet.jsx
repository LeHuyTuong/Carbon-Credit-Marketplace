import React, { useEffect, useRef } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { apiFetch } from "../../utils/apiFetch";
import useWalletData from "./components/useWalletData";
import WalletCard from "./components/WalletCard";
import WalletToast from "./components/WalletToast";
import Deposit from "./Deposit/Deposit";
import Withdraw from "./Withdraw/Withdraw";
import useReveal from "../../hooks/useReveal";
import CreditsList from "./components/CreditsList";

export default function Wallet() {
  const nav = useNavigate();
  const location = useLocation();
  const sectionRef = useRef(null);
  useReveal(sectionRef);

  const { wallet, loading, setLoading, fetchWallet, fetchTransactions } =
    useWalletData();

  const [toast, setToast] = React.useState({
    show: false,
    msg: "",
    type: "success",
  });
  const [showDepositModal, setShowDepositModal] = React.useState(false);
  const [showWithdrawModal, setShowWithdrawModal] = React.useState(false);
  const [showPaymentToast, setShowPaymentToast] = React.useState(false);

  //khi load trang: check nếu có order_id & payment_id
  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const orderId = params.get("order_id");
    const paymentId = params.get("payment_id");

    console.log("Redirect params:", orderId, paymentId);

    const authData =
      JSON.parse(sessionStorage.getItem("auth")) ||
      JSON.parse(localStorage.getItem("auth"));
    const token = authData?.token;

    if (!token) {
      console.warn("Token is not ready, delay confirm...");
      const timer = setTimeout(() => {
        if (orderId) confirmDeposit(orderId, paymentId || "");
        else fetchWallet();
      }, 1000);
      return () => clearTimeout(timer);
    }

    if (orderId) confirmDeposit(orderId, paymentId || "");
    else fetchWallet();
  }, [location.search]);

  //khi ví đã có ID -> gọi transaction list
  useEffect(() => {
    if (wallet?.id) fetchTransactions();
  }, [wallet]);

  // =============== HANDLE ADD MONEY ==================
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

  // =============== CONFIRM PAYMENT ==================
  const confirmDeposit = async (orderId, paymentId) => {
    setLoading(true);
    try {
      await apiFetch(
        `/api/v1/wallet/deposit?order_id=${orderId}&payment_id=${paymentId}`,
        { method: "POST" }
      );

      // reload ví + giao dịch
      await fetchWallet();
      await fetchTransactions();

      // show thông báo thành công
      setToast({
        show: true,
        msg: "Deposit successful! Balance updated.",
        type: "success",
      });

      // xóa param khỏi URL
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

  // =============== HANDLE WITHDRAW ==================
  // const handleWithdrawSubmit = async (values) => {
  //   setLoading(true);
  //   try {
  //     const res = await apiFetch(`/api/v1/withdrawal/${values.amount}`, {
  //       method: "POST",
  //     });

  //     if (res?.responseStatus?.responseCode === "00" || res?.response) {
  //       setToast({
  //         show: true,
  //         msg: "Withdrawal request has been submitted successfully!",
  //         type: "success",
  //       });
  //       await fetchWallet();
  //       await fetchTransactions();
  //     } else {
  //       throw new Error(
  //         res?.responseStatus?.responseMessage || "Withdrawal failed"
  //       );
  //     }
  //   } catch (err) {
  //     console.error("Withdraw error:", err);
  //     setToast({
  //       show: true,
  //       msg: err.message || "Unable to process withdrawal request.",
  //       type: "danger",
  //     });
  //   } finally {
  //     setLoading(false);
  //     setShowWithdrawModal(false);
  //   }
  // };
  const handleWithdrawSubmit = async (values) => {
    setLoading(true);
    try {
      const res = await apiFetch("/api/v1/withdrawal", {
        method: "POST",
        body: {
          data: {
            amount: parseFloat(values.amount),
          },
        },
      });

      if (res?.response) {
        setToast({
          show: true,
          msg: "Withdrawal request submitted. Awaiting admin approval.",
          type: "info",
        });
        await fetchWallet();
        await fetchTransactions();
        // Nếu muốn tự reload danh sách yêu cầu rút tiền thì gọi hàm fetchWithdrawals() ở đây
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

  // =============== CREDITS LIST ==================
  // const [credits, setCredits] = useState([]);

  // useEffect(() => {
  //   apiFetch("/api/v1/credits", { method: "GET" })
  //     .then((res) => setCredits(res.response || []))
  //     .catch((err) => console.error("Failed to fetch credits:", err));
  // }, []);

  return (
    <div
      ref={sectionRef}
      className="auth-hero2 wallet-page reveal d-flex flex-column align-items-center py-5"
    >
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
      <WalletCard
        balance={wallet?.balance}
        currency={wallet?.currency || "USD"}
        onDeposit={() => setShowDepositModal(true)}
        onWithdraw={() => setShowWithdrawModal(true)}
        loading={loading}
      />

      <div className="wallet-history-btn m-3 d-flex flex-wrap justify-content-end gap-2">
        <button
          className="btn btn-outline-light btn-sm d-flex align-items-center gap-2"
          onClick={() => nav("/transaction-history")}
        >
          <i className="bi bi-clock-history"></i>
          Transaction History
        </button>

        <button
          className="btn btn-outline-info btn-sm d-flex align-items-center gap-2"
          onClick={() =>
            nav("/purchase-history", { state: { from: "wallet" } })
          }
        >
          <i className="bi bi-bag-check"></i>
          Purchases History
        </button>
      </div>

      {/* Credits Section */}
      {/* <CreditsList credits={credits} /> */}

      <CreditsList
        credits={[
          {
            id: "1760684896281",
            creditCode: "EV-2025-001",
            title: "EV Charging Credit",
            price: 50000,
            quantity: 1000,
            sold: 200,
            status: "active",
            expiresAt: "10/17/2025, 2:08:16 PM",
          },
          {
            id: "1760684905467",
            creditCode: "EV-2025-002",
            title: "EV Charging Credit",
            price: 50000,
            quantity: 800,
            sold: 100,
            status: "active",
            expiresAt: "10/17/2025, 2:08:29 PM",
          },
        ]}
      />

      {/*deposit modal */}
      <Deposit
        show={showDepositModal}
        onHide={() => setShowDepositModal(false)}
        onSubmit={handleDepositSubmit}
      />

      {/*withdraw modal */}
      <Withdraw
        show={showWithdrawModal}
        onHide={() => setShowWithdrawModal(false)}
        onSubmit={handleWithdrawSubmit}
        wallet={wallet}
        paymentDetail={{ accountName: "hehe", maskedNumber: "**4180" }}
      />

      {/*toast */}
      <WalletToast
        toast={toast}
        setToast={setToast}
        showPaymentToast={showPaymentToast}
        setShowPaymentToast={setShowPaymentToast}
        nav={nav}
      />
    </div>
  );
}
