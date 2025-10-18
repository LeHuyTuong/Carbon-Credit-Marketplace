import { useEffect, useState } from "react";
import { Formik } from "formik";
import { Modal, Button, Form, Card, Spinner } from "react-bootstrap";
import * as Yup from "yup";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../../../utils/apiFetch";

const schema = Yup.object().shape({
  amount: Yup.number()
    .typeError("Amount must be a number")
    .positive("Amount must be greater than 0")
    .max(Yup.ref("maxBalance"), "Amount exceeds available balance")
    .required("Please enter withdrawal amount"),
});

export default function Withdraw({ show, onHide, onSubmit, wallet }) {
  const [paymentDetail, setPaymentDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const nav = useNavigate();

  //lấy payment detail khi mở modal
  useEffect(() => {
    if (show) {
      fetchPaymentDetail();
    }
  }, [show]);

  const fetchPaymentDetail = async () => {
    setLoading(true);
    try {
      const res = await apiFetch("/api/v1/paymentDetails", { method: "GET" });
      setPaymentDetail(res.response || null);
    } catch (err) {
      console.error("Error fetching payment detail:", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal show={show} onHide={onHide} centered size="md">
      <Modal.Header closeButton>
        <Modal.Title className="fw-bold text-center w-100">
          Request Withdrawal
        </Modal.Title>
      </Modal.Header>

      <Formik
        validationSchema={schema}
        initialValues={{ amount: "", maxBalance: wallet?.balance || 0 }}
        onSubmit={(values) => onSubmit(values)}
      >
        {({
          handleSubmit,
          handleChange,
          handleBlur,
          values,
          errors,
          touched,
        }) => (
          <Form noValidate onSubmit={handleSubmit}>
            <Modal.Body>
              {/*Khối 1: Available balance*/}
              <div
                className="d-flex justify-content-between align-items-center p-3 mb-4 rounded"
                style={{
                  backgroundColor: "#f8f9fa",
                  border: "1px solid #e0e0e0",
                }}
              >
                <span className="fw-semibold">Available balance</span>
                <span className="fw-bold text-primary">
                  ${wallet?.balance?.toLocaleString() ?? "0.00"}
                </span>
              </div>

              {/*Khối 2: Withdrawal amount*/}
              <Form.Group
                className="mb-4 text-center p-3 rounded"
                style={{
                  backgroundColor: "#f8f9fa",
                  border: "1px solid #e0e0e0",
                }}
              >
                <Form.Label className="mb-2 fw-semibold">
                  Enter withdrawal amount
                </Form.Label>
                <div className="fs-4 fw-bold text-warning">
                  $
                  <Form.Control
                    autoFocus
                    type="number"
                    name="amount"
                    placeholder="0.00"
                    value={values.amount}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    isInvalid={touched.amount && !!errors.amount}
                    style={{
                      display: "inline-block",
                      width: "150px",
                      background: "transparent",
                      color: "#000",
                      border: "none",
                      borderBottom: "2px solid #ffc107",
                      textAlign: "center",
                      fontSize: "1.5rem",
                    }}
                  />
                </div>
                <Form.Control.Feedback type="invalid">
                  {errors.amount}
                </Form.Control.Feedback>
              </Form.Group>

              {/*Khối 3: Transfer to*/}
              <div
                className="p-3 rounded mb-3"
                style={{
                  backgroundColor: "#f8f9fa",
                  border: "1px solid #e0e0e0",
                }}
              >
                <Form.Label className="fw-semibold">Transfer to</Form.Label>

                {loading ? (
                  <div className="text-center py-3">
                    <Spinner animation="border" size="sm" /> Loading...
                  </div>
                ) : paymentDetail ? (
                  <Card className="bg-transparent border-0">
                    <Card.Body className="d-flex align-items-center justify-content-between p-0">
                      <div className="d-flex align-items-center gap-3">
                        <i className="bi bi-bank2 fs-4 text-primary"></i>
                        <div>
                          <div className="fw-bold">
                            {paymentDetail.accountHolderName}
                          </div>
                          <div className="small text-muted">
                            **{paymentDetail.accountNumber?.slice(-4) || "****"}
                          </div>
                          <div className="small text-muted">
                            Bank: {paymentDetail.bankCode}
                          </div>
                        </div>
                      </div>
                    </Card.Body>
                  </Card>
                ) : (
                  <div className="text-center">
                    <p className="text-muted mb-2">No payment details found.</p>
                    <Button
                      variant="outline-primary"
                      size="sm"
                      onClick={() => {
                        onHide();
                        nav("/payment-detail?create=true");
                      }}
                    >
                      Add Payment Detail
                    </Button>
                  </div>
                )}
              </div>
            </Modal.Body>

            <Modal.Footer className="border-0">
              <Button
                type="submit"
                disabled={values.amount <= 0 || !paymentDetail}
                className="btn-primary w-100 py-2 fw-bold"
              >
                Withdraw
              </Button>
            </Modal.Footer>
          </Form>
        )}
      </Formik>
    </Modal>
  );
}
