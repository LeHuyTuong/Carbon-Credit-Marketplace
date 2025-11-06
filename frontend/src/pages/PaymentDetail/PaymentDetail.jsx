import React, { useEffect, useState, useRef } from "react";
import { useAuth } from "../../context/AuthContext";
import { Modal, Button, Form } from "react-bootstrap";
import { Formik } from "formik";
import * as Yup from "yup";
import { useLocation, useNavigate } from "react-router-dom";
import { FaArrowLeft } from "react-icons/fa";
import { apiFetch } from "../../utils/apiFetch";
import useReveal from "../../hooks/useReveal";

export default function PaymentDetail() {
  // lấy token từ context auth
  const { token } = useAuth();
  // state quản lý dữ liệu payment và các trạng thái phụ
  const [paymentData, setPaymentData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showModal, setShowModal] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  // hook điều hướng và lấy query params
  const nav = useNavigate();
  const location = useLocation();
  const params = new URLSearchParams(location.search);
  const isCreateMode = params.get("create") === "true";
  const isConfirmMode = params.get("confirm") === "true";
  // hiệu ứng reveal khi xuất hiện
  const sectionRef = useRef(null);
  useReveal(sectionRef);

  //lấy dữ liệu Payment Detail khi load trang
  useEffect(() => {
    const fetchPaymentDetail = async () => {
      setLoading(true);
      try {
        const res = await apiFetch("/api/v1/paymentDetails", {
          method: "GET",
        });

        if (!res.response) {
          //chưa có dl, mở modal tạo
          setPaymentData(null);
          if (isCreateMode) {
            setIsCreating(true);
            setShowModal(true);
          }
          return;
        }

        setPaymentData(res.response);
      } catch (err) {
        console.error("Error fetching payment detail:", err);
        setError("Failed to fetch payment detail");
      } finally {
        setLoading(false);
      }
    };

    fetchPaymentDetail();
  }, [token]);

  // UI xử lý các trạng thái
  if (loading)
    return (
      <div
        ref={sectionRef}
        className="d-flex justify-content-center align-items-center vh-100 reveal"
      >
        <div className="spinner-border text-primary" />
      </div>
    );

  // hiển thị lỗi nếu có
  if (error)
    return (
      <div className="text-center mt-5 text-danger">
        <p>{error}</p>
      </div>
    );

  //nếu chưa có dữ liệu payment
  if (!paymentData) {
    return (
      <div className="auth-hero d-flex align-items-center justify-content-center min-vh-100">
        <UpdateModal
          show={showModal}
          onHide={() => nav("/wallet")}
          data={{ paymentData }}
          token={token}
          isCreating={isCreating}
          onSuccess={(created) => {
            setPaymentData(created);
            setShowModal(false);
            setIsCreating(false);
            //nếu là tạo mới xong thì chuyển sang trang confirm
            nav("/payment-detail?confirm=true");
          }}
        />
      </div>
    );
  }

  //nếu đã có dữ liệu payment
  return (
    <div className="auth-hero d-flex flex-column align-items-center min-vh-100 bg-light py-4">
      {/* nút back về wallet */}
      <div className="w-100 text-start mb-5 px-4">
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
      </div>

      {/*card detail */}
      <div
        className="card shadow-lg border-0 rounded-4 p-4 mb-5"
        style={{ maxWidth: "800px", margin: "0 auto" }}
      >
        <h3 className="text-center mb-4 fw-bold">Payment Details</h3>
        <div className="row g-3">
          <div>
            Account Holder Name:{" "}
            <span className="fw-semibold">{paymentData.accountHolderName}</span>
          </div>
          <div>
            Account Number:{" "}
            <span className="fw-semibold">{paymentData.accountNumber}</span>
          </div>
          <div>
            Bank Code:{" "}
            <span className="fw-semibold">{paymentData.bankCode}</span>
          </div>
        </div>

        {/* hành động cập nhật hoặc confirm */}
        <div className="d-flex justify-content-between align-items-center mt-4">
          <Button
            variant="success"
            onClick={() => {
              setIsCreating(false);
              setShowModal(true);
            }}
          >
            Update Details
          </Button>
          {/* nếu đang ở chế độ confirm sau khi tạo mới */}
          {isConfirmMode && (
            <Button
              variant="primary"
              className="fw-bold"
              onClick={() => nav("/wallet")}
            >
              Confirm & Proceed to Withdraw
            </Button>
          )}
        </div>
      </div>

      {/* modal cập nhật/thêm mới thông tin */}
      <UpdateModal
        show={showModal}
        onHide={() => nav("/wallet")}
        data={paymentData}
        token={token}
        isCreating={isCreating}
        onSuccess={(updated) => {
          setPaymentData(updated);
          setShowModal(false);
        }}
      />
    </div>
  );
}

// schema validate form
const schema = Yup.object().shape({
  accountHolderName: Yup.string().required("Account holder name is required"),
  accountNumber: Yup.string().required("Account number is required"),
  confirm: Yup.string()
    .oneOf([Yup.ref("accountNumber"), null], "Account numbers must match")
    .required("Please confirm your account number"),
  bankCode: Yup.string().required("Bank code is required"),
});
// Modal tạo/sửa
function UpdateModal({ show, onHide, data, isCreating, onSuccess }) {
  // xử lý submit form
  const handleSubmitForm = async (values) => {
    try {
      const payload = {
        requestTrace: crypto.randomUUID(),
        requestDateTime: new Date().toISOString(),
        data: {
          accountNumber: values.accountNumber,
          accountHolderName: values.accountHolderName,
          bankCode: values.bankCode,
          customerName: values.accountHolderName,
        },
      };

      const method = isCreating ? "POST" : "PUT";

      const res = await apiFetch("/api/v1/paymentDetails", {
        method,
        body: payload,
      });

      const result = res.response || res.createdData || res.updatedData;
      if (result) onSuccess(result);
    } catch (err) {
      console.error("Error submitting form:", err);
      alert(err.message || "Failed to save details");
    }
  };

  return (
    <Modal show={show} onHide={onHide} centered dialogClassName="payment-detai">
      <Modal.Header closeButton>
        <Modal.Title>
          {isCreating ? "Add Payment Details" : "Update Payment Details"}
        </Modal.Title>
      </Modal.Header>

      {/* formik quản lý form nhập liệu và validate */}
      <Formik
        enableReinitialize
        validationSchema={schema}
        initialValues={{
          accountHolderName: data?.accountHolderName || "",
          accountNumber: data?.accountNumber || "",
          confirm: data?.accountNumber || "",
          bankCode: data?.bankCode || "",
        }}
        onSubmit={handleSubmitForm}
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
              <Form.Group className="mb-3">
                <Form.Label>Account Holder Name</Form.Label>
                <Form.Control
                  name="accountHolderName"
                  value={values.accountHolderName}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={
                    touched.accountHolderName && !!errors.accountHolderName
                  }
                />
                <Form.Control.Feedback type="invalid">
                  {errors.accountHolderName}
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Account Number</Form.Label>
                <Form.Control
                  name="accountNumber"
                  value={values.accountNumber}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.accountNumber && !!errors.accountNumber}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.accountNumber}
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Confirm Account Number</Form.Label>
                <Form.Control
                  name="confirm"
                  value={values.confirm}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.confirm && !!errors.confirm}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.confirm}
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Bank Code</Form.Label>
                <Form.Control
                  name="bankCode"
                  value={values.bankCode}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.bankCode && !!errors.bankCode}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.bankCode}
                </Form.Control.Feedback>
              </Form.Group>
            </Modal.Body>

            <Modal.Footer>
              <Button variant="secondary" onClick={onHide}>
                Close
              </Button>
              <Button type="submit" variant="primary">
                {isCreating ? "Create Payment Method" : "Update Payment Method"}
              </Button>
            </Modal.Footer>
          </Form>
        )}
      </Formik>
    </Modal>
  );
}
