import React, { useState, useRef, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import {
  Form,
  Button,
  Accordion,
  Card,
  Row,
  Col,
  Container,
  Modal,
  Spinner,
} from "react-bootstrap";
import { FaArrowLeft, FaShoppingCart } from "react-icons/fa";
import { toast } from "react-toastify";
import useReveal from "../../../../hooks/useReveal";
import { apiFetch } from "../../../../utils/apiFetch";
import { useAuth } from "../../../../context/AuthContext";
import CreditDetailCard from "./CreditDetailCard";

export default function Order() {
  const nav = useNavigate();
  const { state } = useLocation();
  const { user } = useAuth();
  const credit = state?.credit;
  const [showConfirm, setShowConfirm] = useState(false);
  const [walletBalance, setWalletBalance] = useState(null); // số dư ví hiện tại
  const [loadingBalance, setLoadingBalance] = useState(false);
  const sectionRef = useRef(null);
  useReveal(sectionRef);

  // nếu user vào thẳng /order không qua marketplace
  if (!credit) {
    return (
      <div
        ref={sectionRef}
        className="reveal d-flex flex-column align-items-center justify-content-center min-vh-100 text-center"
      >
        <p className="text-muted mb-3">No credit selected.</p>
        <Button variant="primary" onClick={() => nav("/marketplace")}>
          Back to Marketplace
        </Button>
      </div>
    );
  }

  //fetch số dư ví
  useEffect(() => {
    const fetchWalletBalance = async () => {
      try {
        setLoadingBalance(true);
        const res = await apiFetch("/api/v1/wallet", { method: "GET" });
        const balance = res?.response?.balance || 0;
        setWalletBalance(balance);
      } catch (err) {
        console.error("Failed to load wallet:", err);
        setWalletBalance(0); // nếu lỗi → gán mặc định 0
      } finally {
        setLoadingBalance(false);
      }
    };

    fetchWalletBalance();
  }, []);

  const pricePerTonne = credit.price; // giá mỗi tấn CO2
  const availableTonnes = credit.quantity; // số lượng khả dụng
  const [formData, setFormData] = useState({ quantity: "" }); // số lượng người dùng nhập

  // tính tổng tiền theo số lượng
  const totalPrice =
    formData.quantity && formData.quantity > 0
      ? (formData.quantity * pricePerTonne).toFixed(2)
      : "0.00";

  // cập nhật giá trị nhập form
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  // khi nhấn “Purchase” → mở modal xác nhận
  const handleSubmit = (e) => {
    e.preventDefault();
    setShowConfirm(true);
  };

  //khi user confirm mua
  const handleConfirmPurchase = async () => {
    setShowConfirm(false);
    try {
      // lấy company ID qua KYC
      const kycRes = await apiFetch("/api/v1/kyc/company", { method: "GET" });
      const buyerCompanyId = kycRes?.response?.id;

      if (!buyerCompanyId) {
        toast.error(
          "Cannot determine your company ID. Please complete company KYC."
        );
        return;
      }

      // tạo order
      const payload = {
        data: {
          buyerCompanyId,
          listingId: credit.id || credit.listingId,
          quantity: Number(formData.quantity),
        },
      };

      const res = await apiFetch("/api/v1/orders", {
        method: "POST",
        body: payload,
      });

      const orderId = res?.response?.id;
      if (!orderId) {
        toast.error("Order created but missing ID.");
        return;
      }

      // complete order
      await apiFetch(`/api/v1/orders/${orderId}/complete`, { method: "POST" });

      toast.success("Order completed successfully!");

      //navigate về purchase history
      setTimeout(() => {
        nav("/purchase-history", {
          state: { refreshCredits: true, from: "order" },
        });
      }, 1500);
    } catch (err) {
      console.error("Order create error:", err);
      toast.error(err.message || "Unable to create order.");
    }
  };

  return (
    <div ref={sectionRef} className="auth-hero min-vh-100 py-5 bg-light reveal">
      <Container>
        {/* back button */}
        <div className="mb-4">
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
            onClick={() => nav("/marketplace")}
          >
            <FaArrowLeft /> Back to Marketplace
          </Button>
        </div>

        {/* credit detail */}
        <CreditDetailCard credit={credit} />

        <Row>
          {/* CỘT TRÁI - FORM MUA CREDIT */}
          <Col lg={8}>
            <Card
              className="shadow-sm border-0 mb-4 overflow-hidden"
              style={{ background: "#fff", borderRadius: "12px" }}
            >
              <Card.Body>
                <h3 className="fw-bold mb-3 d-flex align-items-center gap-2">
                  <FaShoppingCart color="green" /> Buy Carbon Credits
                </h3>
                <p className="text-muted mb-4" style={{ lineHeight: "1" }}>
                  Finalize your purchase for <strong>{credit.title}</strong>.
                </p>

                {/* form nhập số lượng mua */}
                <Form onSubmit={handleSubmit}>
                  <Form.Group className="mb-3">
                    <Form.Label className="fw-semibold">
                      Quantity (tonnes) <span className="text-danger">*</span>
                    </Form.Label>
                    <Form.Text muted>
                      {" "}
                      Available: {availableTonnes} tonnes
                    </Form.Text>
                    <Form.Control
                      type="number"
                      name="quantity"
                      placeholder="Enter tonnes"
                      min="1"
                      max={availableTonnes}
                      value={formData.quantity}
                      onChange={handleChange}
                      required
                    />
                  </Form.Group>

                  <div className="mt-4">
                    <Button
                      variant="success"
                      type="submit"
                      className="btn-primary w-100 py-2 fw-semibold"
                      disabled={!formData.quantity}
                    >
                      Purchase
                    </Button>
                  </div>
                </Form>
              </Card.Body>
            </Card>

            {/* Info after purchase */}
            <Accordion>
              <Accordion.Item eventKey="1">
                <Accordion.Header>After Purchase</Accordion.Header>
                <Accordion.Body>
                  <ul className="mb-3">
                    <li>
                      Payment processed securely via{" "}
                      <strong>Stripe or Paypal</strong>.
                    </li>
                    <li>
                      Purchased credits are{" "}
                      <strong>added to your wallet</strong> once confirmed.
                    </li>
                    <li>
                      Review your purchases under{" "}
                      <strong>“Purchases History”</strong> in Wallet or{" "}
                      <strong>"Orders"</strong>.
                    </li>
                    <li>
                      All credits are <strong>verified by CarbonX</strong> for
                      authenticity.
                    </li>
                  </ul>
                  <p className="text-muted small mb-0">
                    Note: credits are non-refundable after issuance.
                  </p>
                </Accordion.Body>
              </Accordion.Item>
            </Accordion>
          </Col>

          {/* CỘT PHẢI - TÓM TẮT GIAO DỊCH */}
          <Col lg={4}>
            <ProjectSummary
              totalPrice={totalPrice}
              quantity={formData.quantity}
              pricePerTonne={pricePerTonne}
              title={credit.title}
              walletBalance={walletBalance}
              loadingBalance={loadingBalance}
            />
          </Col>
        </Row>

        {/* Confirm Modal */}
        <Modal show={showConfirm} onHide={() => setShowConfirm(false)} centered>
          <Modal.Header closeButton>
            <Modal.Title>Confirm Purchase</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <p>
              You are about to purchase <strong>{formData.quantity}</strong>{" "}
              tonnes of <strong>{credit.title}</strong> at{" "}
              <strong>${pricePerTonne.toFixed(2)}</strong> per tonne.
            </p>
            <p className="fw-bold text-success mb-0">Total: ${totalPrice}</p>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowConfirm(false)}>
              Cancel
            </Button>
            <Button className="btn-primary" onClick={handleConfirmPurchase}>
              Confirm
            </Button>
          </Modal.Footer>
        </Modal>
      </Container>
    </div>
  );
}

// hiển thị tóm tắt giao dịch
function ProjectSummary({
  totalPrice,
  quantity,
  pricePerTonne,
  title,
  walletBalance,
  loadingBalance,
}) {
  return (
    <div className="sticky-top" style={{ top: "80px" }}>
      <Card
        className="shadow-sm border-0 overflow-hidden"
        style={{ background: "#fff", borderRadius: "12px" }}
      >
        <Card.Body>
          <Card.Title>{title || "Carbon Credit"}</Card.Title>
          {/* hiển thị số lượng và giá */}
          <Row className="mt-3">
            <Col xs={6} className="text-muted">
              Quantity
            </Col>
            <Col xs={6} className="text-end">
              {quantity || 0} tCO₂
            </Col>
          </Row>
          <Row>
            <Col xs={6} className="text-muted">
              Price per tonne
            </Col>
            <Col xs={6} className="text-end">
              ${pricePerTonne.toFixed(2)}
            </Col>
          </Row>
          <hr />

          {/* tổng tiền */}
          <Row>
            <Col xs={6} className="text-muted">
              Total
            </Col>
            <Col xs={6} className="text-end fw-bold text-success">
              ${totalPrice}
            </Col>
          </Row>
          <hr />

          {/* hiển thị số dư ví */}
          <Row>
            <Col xs={6} className="text-muted">
              Your Wallet Balance
            </Col>
            <Col xs={6} className="text-end fw-bold text-warning">
              {loadingBalance ? (
                <Spinner size="sm" animation="border" />
              ) : (
                `$${walletBalance?.toLocaleString() || 0}`
              )}
            </Col>
          </Row>
        </Card.Body>
      </Card>
    </div>
  );
}
