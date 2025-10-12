import React, { useEffect, useState } from "react";
import { useAuth } from "../../context/AuthContext";
import { Modal, Button, Form } from "react-bootstrap";
import { Formik } from "formik";
import * as Yup from "yup";

//giả lập API
async function apiFetch(url, options = {}) {
  console.log("[Mock API] Fetching:", url, options);
  await new Promise((r) => setTimeout(r, 800));

  //giả lập lần đầu load: có dữ liệu sẵn
  if (options.method === "GET" || !options.method) {
    const hasData = false;
    if (!hasData) return null;

    return {
      id: "mock-payment-001",
      name: "Diu Lin",
      number: "1901234567912",
      bank: "MB Bank",
    };
  }

  // POST: tạo mới
  if (options.method === "POST") {
    const body = JSON.parse(options.body);
    console.log("[Mock API] Create Payload:", body);
    return { status: "CREATED", createdData: body.data };
  }

  // PUT: cập nhật
  if (options.method === "PUT") {
    const body = JSON.parse(options.body);
    console.log("[Mock API] Update Payload:", body);
    return { status: "OK", updatedData: body.data };
  }

  throw new Error("Unsupported mock method");
}

export default function PaymentDetail() {
  const { user, token } = useAuth();
  const [paymentData, setPaymentData] = useState(null);
  const [paymentId, setPaymentId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showModal, setShowModal] = useState(false);
  const [isCreating, setIsCreating] = useState(false);

  //lấy dữ liệu Payment Detail khi load trang
  useEffect(() => {
    const fetchPaymentDetail = async () => {
      setLoading(true);
      try {
        const res = await apiFetch("/api/v1/payment-detail", {
          headers: { Authorization: `Bearer ${token}` },
        });

        if (!res) {
          setPaymentData(null);
          return;
        }

        setPaymentData(res);
        setPaymentId(res.id);
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
      <div className="d-flex justify-content-center align-items-center vh-100">
        <div className="spinner-border text-primary" />
      </div>
    );

  if (error)
    return (
      <div className="text-center mt-5 text-danger">
        <p>{error}</p>
      </div>
    );

  //nếu chưa có dữ liệu payment
  if (!paymentData) {
    return (
      <div className="text-center mt-5">
        <h4>No Payment Details Found</h4>
        <Button
          variant="success"
          onClick={() => {
            setIsCreating(true);
            setShowModal(true);
          }}
        >
          Add Payment Details
        </Button>

        <UpdateModal
          show={showModal}
          onHide={() => setShowModal(false)}
          data={{ name: "", number: "", confirm: "", bank: "" }}
          token={token}
          isCreating={isCreating}
          onSuccess={(created) => {
            setPaymentData(created);
            setShowModal(false);
            setIsCreating(false);
          }}
        />
      </div>
    );
  }

  //nếu đã có dữ liệu payment
  return (
    <div className="auth-hero d-flex justify-content-center align-items-center min-vh-100 bg-light">
      <div
        className="card shadow-lg border-0 rounded-4 p-4"
        style={{ maxWidth: "800px", margin: "0 auto" }}
      >
        <h3 className="text-center mb-4 fw-bold">Payment Details</h3>

        <div className="row g-3">
          <div>
            Account Holder Name:{" "}
            <span className="fw-semibold">{paymentData.name}</span>
          </div>
          <div>
            Account Number:{" "}
            <span className="fw-semibold">{paymentData.number}</span>
          </div>
          <div>
            Bank Name: <span className="fw-semibold">{paymentData.bank}</span>
          </div>
        </div>

        <div className="text-end mt-4">
          <Button
            variant="primary"
            onClick={() => {
              setIsCreating(false);
              setShowModal(true);
            }}
          >
            Update Details
          </Button>
        </div>

        <UpdateModal
          show={showModal}
          onHide={() => setShowModal(false)}
          data={paymentData}
          paymentId={paymentId}
          token={token}
          isCreating={isCreating}
          onSuccess={(updated) => {
            setPaymentData(updated);
            setShowModal(false);
          }}
        />
      </div>
    </div>
  );
}

// Modal tạo/sửa
function UpdateModal({
  show,
  onHide,
  data,
  paymentId,
  token,
  isCreating,
  onSuccess,
}) {
  const schema = Yup.object().shape({
    name: Yup.string().required("Account holder name is required"),
    number: Yup.string().required("Account number is required"),
    confirm: Yup.string()
      .oneOf([Yup.ref("number"), null], "Account numbers must match")
      .required("Please confirm your account number"),
    bank: Yup.string().required("Bank name is required"),
  });

  const handleSubmitForm = async (values) => {
    try {
      const payload = {
        requestTrace: crypto.randomUUID(),
        requestDateTime: new Date().toISOString(),
        data: {
          name: values.name,
          number: values.number,
          confirm: values.confirm,
          bank: values.bank,
        },
      };

      const res = await apiFetch(
        isCreating
          ? "/api/v1/payment-detail"
          : `/api/v1/payment-detail/${paymentId}`,
        {
          method: isCreating ? "POST" : "PUT",
          headers: { Authorization: `Bearer ${token}` },
          body: JSON.stringify(payload),
        }
      );

      if (res.status === "OK" || res.status === "CREATED") {
        onSuccess(res.updatedData || res.createdData);
      }
    } catch (err) {
      console.error("Error submitting form:", err);
      alert(err.message || "Failed to save details");
    }
  };

  return (
    <Modal show={show} onHide={onHide} centered>
      <Modal.Header closeButton>
        <Modal.Title>
          {isCreating ? "Add Payment Details" : "Update Payment Details"}
        </Modal.Title>
      </Modal.Header>

      <Formik
        enableReinitialize
        validationSchema={schema}
        initialValues={data}
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
                  name="name"
                  value={values.name}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.name && !!errors.name}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.name}
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Account Number</Form.Label>
                <Form.Control
                  name="number"
                  value={values.number}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.number && !!errors.number}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.number}
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
                <Form.Label>Bank Name</Form.Label>
                <Form.Control
                  name="bank"
                  value={values.bank}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.bank && !!errors.bank}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.bank}
                </Form.Control.Feedback>
              </Form.Group>
            </Modal.Body>

            <Modal.Footer>
              <Button variant="secondary" onClick={onHide}>
                Close
              </Button>
              <Button type="submit" variant="primary">
                {isCreating ? "Create" : "Save Changes"}
              </Button>
            </Modal.Footer>
          </Form>
        )}
      </Formik>
    </Modal>
  );
}
