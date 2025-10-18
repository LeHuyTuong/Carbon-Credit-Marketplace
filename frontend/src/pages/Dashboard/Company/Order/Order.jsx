import React, { useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import {
  Form,
  Button,
  Accordion,
  Card,
  Row,
  Col,
  Alert,
  Container,
} from "react-bootstrap";
import { FaLock, FaArrowLeft, FaShoppingCart } from "react-icons/fa";
import { Modal, Toast, ToastContainer } from "react-bootstrap";
import { toast } from "react-toastify";

export default function Order() {
  const nav = useNavigate();
  const { state } = useLocation();
  const credit = state?.credit;
  const [showConfirm, setShowConfirm] = useState(false);
  const [showToast, setShowToast] = useState(false);

  // Nếu user vào thẳng /order không qua marketplace thì redirect
  if (!credit) {
    return (
      <div className="d-flex flex-column align-items-center justify-content-center min-vh-100 text-center">
        <p className="text-muted mb-3">No credit selected.</p>
        <Button variant="primary" onClick={() => nav("/marketplace")}>
          Back to Marketplace
        </Button>
      </div>
    );
  }

  //thông tin giá & tồn kho từ credit được chọn
  const pricePerTonne = credit.price;
  const availableTonnes = credit.quantity;

  const [formData, setFormData] = useState({
    quantity: "",
    beneficiaryName: "",
  });

  //tính tổng tiền dựa trên quantity * pricePerTonne
  const totalPrice =
    formData.quantity && formData.quantity > 0
      ? (formData.quantity * pricePerTonne).toFixed(2)
      : "0.00";

  //cập nhật state khi user nhập form
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  //khi user bấm “Purchase” → mở modal xác nhận
  const handleSubmit = (e) => {
    e.preventDefault();
    setShowConfirm(true);
  };

  const handleConfirmPurchase = () => {
    setShowConfirm(false);

    // Tạo object đơn hàng mới
    const newPurchase = {
      title: credit.title,
      quantity: Number(formData.quantity),
      pricePerTonne,
      total: Number(totalPrice),
      beneficiaryName: formData.beneficiaryName,
      purchasedAt: new Date().toISOString(),
    };

    // lưu vào localStorage (danh sách nhiều giao dịch)
    const history = JSON.parse(localStorage.getItem("purchases") || "[]");
    history.push(newPurchase);
    localStorage.setItem("purchases", JSON.stringify(history));

    toast.success("Purchase successfully!");
    setTimeout(() => nav("/purchase-history"), 3000);
  };

  return (
    <div className="auth-hero min-vh-100 py-5 bg-light">
      <Container>
        <div className="mb-4">
          {/*nút Back to marketplace cố định góc trên trái */}
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

        <Row>
          {/* LEFT FORM */}
          <Col lg={8}>
            <Card className="shadow-sm border-0 mb-4">
              <Card.Body>
                <h3 className="fw-bold mb-3 d-flex align-items-center gap-2">
                  <FaShoppingCart color="green" /> Buy Carbon Credits
                </h3>
                <p className="text-muted mb-4">
                  Finalize your purchase for <strong>{credit.title}</strong>.
                </p>

                <Form onSubmit={handleSubmit}>
                  {/* Quantity */}
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

                  {/* Beneficiary */}
                  <Form.Group className="mb-3">
                    <Form.Label className="fw-semibold">
                      Beneficiary name <span className="text-danger">*</span>
                    </Form.Label>
                    <Form.Control
                      type="text"
                      name="beneficiaryName"
                      placeholder="Who will receive credit"
                      value={formData.beneficiaryName}
                      onChange={handleChange}
                      required
                    />
                  </Form.Group>

                  {/* Submit */}
                  <div className="mt-4">
                    <Button
                      variant="success"
                      type="submit"
                      className="btn-primary w-100 py-2 fw-semibold"
                      disabled={!formData.quantity || !formData.beneficiaryName}
                    >
                      Purchase
                    </Button>
                  </div>
                </Form>
              </Card.Body>
            </Card>

            {/* After purchase info */}
            <Accordion>
              <Accordion.Item eventKey="1">
                <Accordion.Header>After Purchase</Accordion.Header>
                <Accordion.Body>
                  <ul className="mb-0">
                    <li>Payment is processed securely.</li>
                    <li>You receive amount of credits in your wallet.</li>
                  </ul>
                </Accordion.Body>
              </Accordion.Item>
            </Accordion>
          </Col>

          {/* RIGHT SUMMARY */}
          <Col lg={4}>
            <ProjectSummary
              totalPrice={totalPrice}
              quantity={formData.quantity}
              pricePerTonne={pricePerTonne}
              title={credit.title}
            />
          </Col>
        </Row>
        {/*confirm modal */}
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

function ProjectSummary({ totalPrice, quantity, pricePerTonne, title }) {
  return (
    <div className="sticky-top" style={{ top: "80px" }}>
      <Card className="shadow-sm border-0">
        <Card.Body>
          <Card.Title>{title || "Carbon Credit"}</Card.Title>
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
          <Row>
            <Col xs={6} className="text-muted">
              Total
            </Col>
            <Col xs={6} className="text-end fw-bold text-success">
              ${totalPrice}
            </Col>
          </Row>
        </Card.Body>
      </Card>
    </div>
  );
}
