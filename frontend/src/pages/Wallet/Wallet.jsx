import React, { useEffect, useState } from "react";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap-icons/font/bootstrap-icons.css";
import "./wallet.css";
import { useNavigate } from "react-router-dom";
import { Button, Toast, ToastContainer } from "react-bootstrap";
import { apiFetch } from "../../utils/apiFetch";
import Deposit from "./Deposit/Deposit";

export default function Wallet() {
  const nav = useNavigate();
  const [wallet, setWallet] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showDepositModal, setShowDepositModal] = useState(false);
  //toast state
  const [toast, setToast] = useState({
    show: false,
    msg: "",
    type: "success",
  });
  const [showPaymentToast, setShowPaymentToast] = useState(false);

  //load wallet and history
  useEffect(() => {
    //nếu url có order_id & payment_id thì gọi api xác nhận nạp tiền
    const params = new URLSearchParams(location.search);
    const orderId = params.get("order_id");
    const paymentId = params.get("payment_id");

    console.log("orderId:", orderId, "paymentId:", paymentId);

    //có data thì gọi
    if (orderId) {
      confirmDeposit(orderId, paymentId || "");
    } else {
      //nếu không có order_id thì load ví bình thường
      fetchWallet();
      fetchTransactions();
    }
  }, [location.search]); //chạy lại effect khi query đổi

  const fetchWallet = async () => {
    try {
      const res = await apiFetch("/api/v1/wallet", { method: "POST" });
      setWallet(res.response);
    } catch (err) {
      console.error("Failed to fetch wallet:", err);
    }
  };

  //lịch sử giao dịch
  // const fetchTransactions = async () => {
  //   try {
  //     if (!wallet?.id) return; //chờ wallet load xong

  //     const res = await apiFetch("/api/v1/wallet/transactions", {
  //       method: "GET",
  //     });

  //     setTransactions(res.response || []);
  //   } catch (err) {
  //     console.error("Failed to fetch transactions:", err);
  //   }
  // };
  const fetchTransactions = async () => {
    try {
      if (!wallet?.id) return;

      const reqObject = {
        requestTrace: crypto.randomUUID(),
        requestDateTime: new Date().toISOString(),
        data: {
          wallet: { id: wallet.id },
        },
      };

      // ⚠️ Không encodeURIComponent nữa, để BE nhận đúng JSON string
      const query = JSON.stringify(reqObject);

      const res = await apiFetch(`/api/v1/wallet/transactions?req=${query}`, {
        method: "GET",
      });

      setTransactions(res.response || []);
    } catch (err) {
      console.error("Failed to fetch transactions:", err);
    }
  };

  //show modal chon phg thuc thanh toan
  const handleAddMoney = () => {
    setShowDepositModal(true);
  };

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

  //xác nhận trạng thái thanh toán
  const confirmDeposit = async (orderId, paymentId) => {
    setLoading(true);
    try {
      const res = await apiFetch(
        `/api/v1/wallet/deposit?order_id=${orderId}&payment_id=${paymentId}`,
        {
          method: "POST",
        }
      );

      setToast({
        show: true,
        msg: "Deposit successful! Balance updated.",
        type: "success",
      });

      //reload ví sau khi cộng tiền thành công
      await fetchWallet();
      await fetchTransactions();

      //xóa params khỏi url (ko gọi lại api khi reload)
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

  //xử lý rút tiền
  const handleWithdraw = async () => {
    setLoading(true);
    try {
      const res = await apiFetch("/api/v1/paymentDetails");
      if (!res || !res.response) {
        setShowPaymentToast(true); //hiện toast yêu cầu thêm payment detail
      } else {
        nav("/payment-detail?confirm=true"); // chuyển đến trang Payment Detail để xác nhận rút tiền
      }
    } catch (err) {
      console.error("Error fetching payment detail:", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-hero2 wallet-page d-flex flex-column align-items-center py-5">
      {/* header */}
      <div className="text-center mb-4">
        <div
          className="d-flex justify-content-center align-items-center gap-2 mb-2"
          style={{ marginTop: "50px" }}
        >
          <i className="bi bi-wallet2 fs-3 text-accent"></i>
          <h3 className="fw-bold text-light mb-0">My Wallet</h3>
        </div>
      </div>
      {/* balance card */}
      <div className="wallet-card glass-card text-center p-4 mb-5">
        <h6 className="mb-2">Balance:</h6>
        <h2 className="display-6 fw-bold text-accent mb-4">
          <i className="bi bi-currency-dollar"></i>
          {wallet ? wallet.balance.toLocaleString() : "0.00"}
        </h2>

        <div className="d-flex justify-content-center gap-3 flex-wrap">
          <button
            className="wallet-action border-success text-success"
            onClick={handleAddMoney}
            disabled={loading}
          >
            <i className="bi bi-upload fs-5"></i>
            <span>{loading ? "Processing..." : "Add Money"}</span>
          </button>
          <button
            className="wallet-action border-warning text-warning"
            onClick={handleWithdraw}
            disabled={loading}
          >
            <i className="bi bi-download fs-5"></i>
            <span>{loading ? "Checking..." : "Withdraw"}</span>
          </button>
          <button className="wallet-action border-info text-info">
            <i className="bi bi-shuffle fs-5"></i>
            <span>Transfer</span>
          </button>
        </div>
      </div>

      {/* history */}
      <div className="wallet-history">
        <div className="d-flex justify-content-between align-items-center mb-3">
          <h5 className="text-light mb-0">
            History <i className="bi bi-arrow-clockwise small ms-2"></i>
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
                <div className="small text-muted">
                  {tx.createAt ? new Date(tx.createAt).toLocaleString() : ""}
                </div>
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

      {/*modal nap tiền */}
      <Deposit
        show={showDepositModal}
        onHide={() => setShowDepositModal(false)}
        onSubmit={handleDepositSubmit}
      />

      {/*toast */}
      <ToastContainer position="top-center" className="p-3">
        {/*toast add money & error message */}
        <Toast
          bg={toast.type}
          show={toast.show}
          onClose={() => setToast({ ...toast, show: false })}
          delay={4000}
          autohide
        >
          <Toast.Header>
            <strong className="me-auto text-capitalize">
              {toast.type === "success"
                ? "Success"
                : toast.type === "danger"
                ? "Error"
                : "Notice"}
            </strong>
          </Toast.Header>
          <Toast.Body className="text-light">{toast.msg}</Toast.Body>
        </Toast>

        {/*toast payment detail */}
        <Toast
          show={showPaymentToast}
          onClose={() => setShowPaymentToast(false)}
          autohide={false} // không tự biến mất
          bg="success"
        >
          <Toast.Header closeButton={true}>
            <strong className="me-auto text-success">
              Payment Detail Required
            </strong>
          </Toast.Header>
          <Toast.Body className="text-light">
            <p className="mb-3">
              You need to add your payment details before proceeding with a
              withdrawal.
            </p>
            <div className="d-flex justify-content-end gap-2">
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setShowPaymentToast(false)}
              >
                Close
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={() => {
                  setShowPaymentToast(false);
                  nav("/payment-detail?create=true");
                }}
              >
                Add Now
              </Button>
            </div>
          </Toast.Body>
        </Toast>
      </ToastContainer>
    </div>
  );
}
