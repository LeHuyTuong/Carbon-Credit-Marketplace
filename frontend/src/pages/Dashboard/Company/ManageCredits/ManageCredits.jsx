import { useState, useEffect, useRef } from "react";
import "../../EVOwner/ManageVehicle/manage.css";
import { Button, Modal, Form, Toast, ToastContainer } from "react-bootstrap";
import { Formik } from "formik";
import * as Yup from "yup";
import useReveal from "../../../../hooks/useReveal";
import { FaArrowLeft } from "react-icons/fa";
import { useNavigate } from "react-router-dom";

//Validation schema
const schema = Yup.object().shape({
  carbonCreditId: Yup.number()
    .required("Please select a credit")
    .typeError("Please select a credit"),
  quantity: Yup.number()
    .required("Quantity is required")
    .positive("Must be greater than 0"),
  pricePerCredit: Yup.number()
    .required("Price is required")
    .positive("Must be greater than 0"),
  expirationDate: Yup.date().required("Expiration date is required"),
});

export default function ManageCredits() {
  const [credits, setCredits] = useState([]);
  const [userCredits, setUserCredits] = useState([]); // mock dữ liệu user
  const [show, setShow] = useState(false);
  const [editData, setEditData] = useState(null);
  const [confirm, setConfirm] = useState({ show: false, id: null });
  const nav = useNavigate();
  const [toast, setToast] = useState({
    show: false,
    message: "",
    variant: "success",
  });
  const sectionRef = useRef(null);
  useReveal(sectionRef);

  // mock credit có sẵn của user
  useEffect(() => {
    setUserCredits([
      { id: 101, title: "EV Charging Credit - Project A", balance: 200 },
      { id: 102, title: "EV Charging Credit - Project B", balance: 500 },
    ]);
  }, []);

  // đọc từ localStorage
  const fetchCredits = () => {
    const local = JSON.parse(localStorage.getItem("mockCredits") || "[]");
    setCredits(local);
  };

  useEffect(() => {
    fetchCredits();
  }, []);

  // Mở modal
  const handleAdd = () => {
    setEditData(null);
    setShow(true);
  };

  // Xoá tín chỉ
  const handleDeleteClick = (id) => {
    setConfirm({ show: true, id });
  };

  const handleConfirmDelete = () => {
    const updated = credits.filter((c) => c.id !== confirm.id);
    localStorage.setItem("mockCredits", JSON.stringify(updated));
    setCredits(updated);
    setConfirm({ show: false, id: null });
    showToast("Credit removed successfully");
  };

  const handleCancelDelete = () => setConfirm({ show: false, id: null });

  // Thêm mới tín chỉ (mock lưu localStorage)
  const handleSubmit = async (values) => {
    try {
      const mockCredit = {
        id: Date.now(),
        title: userCredits.find((u) => u.id === +values.carbonCreditId)?.title,
        price: values.pricePerCredit,
        quantity: values.quantity,
        status: "active",
        createdAt: new Date(values.expirationDate).toLocaleDateString(),
      };

      const stored = JSON.parse(localStorage.getItem("mockCredits") || "[]");
      stored.push(mockCredit);
      localStorage.setItem("mockCredits", JSON.stringify(stored));
      setCredits(stored);

      showToast("Credit listed successfully");
      setShow(false);
      setEditData(null);
    } catch (error) {
      console.error("Submit error:", error);
      showToast("Error submitting credit", "danger");
    }
  };

  const showToast = (message, variant = "success") => {
    setToast({ show: true, message, variant });
    setTimeout(() => setToast({ show: false, message: "", variant }), 3000);
  };

  const handleClose = () => {
    setShow(false);
    setEditData(null);
  };

  return (
    <div ref={sectionRef} className="reveal">
      {/*nút Back to Home cố định góc trên trái */}
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
        onClick={() => nav("/home")}
      >
        <FaArrowLeft /> Back to Home
      </Button>
      <div className="vehicle-search-section">
        <h1 className="title">List Your Credits For Sale</h1>
        <Button className="mb-3" onClick={handleAdd}>
          Add Credit
        </Button>
      </div>

      <CreditModal
        show={show}
        onHide={handleClose}
        onSubmit={handleSubmit}
        data={editData}
        userCredits={userCredits}
      />

      <div className="table-wrapper">
        <table className="vehicle-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Title</th>
              <th>Price ($)</th>
              <th>Quantity</th>
              <th>Status</th>
              <th>Expires At</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {credits.length > 0 ? (
              credits.map((row) => (
                <tr key={row.id}>
                  <td>{row.id}</td>
                  <td>{row.title}</td>
                  <td>${row.price}</td>
                  <td>{row.quantity}</td>
                  <td>
                    <span
                      className={`status-badge ${
                        row.status === "active" ? "approved" : "rejected"
                      }`}
                    >
                      {row.status}
                    </span>
                  </td>
                  <td>{row.createdAt}</td>
                  <td className="action-buttons">
                    <button
                      className="action-btn delete"
                      onClick={() => handleDeleteClick(row.id)}
                    >
                      <i className="bi bi-trash"></i>
                    </button>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="7" className="no-data">
                  <h5>No credits yet</h5>
                  <p>Add credits to start selling.</p>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <ToastContainer position="top-center" className="p-3">
        <Toast
          onClose={() => setToast({ ...toast, show: false })}
          show={toast.show}
          bg={toast.variant}
          delay={3000}
          autohide
        >
          <Toast.Body className="text-white">{toast.message}</Toast.Body>
        </Toast>
      </ToastContainer>

      <Modal show={confirm.show} onHide={handleCancelDelete} centered>
        <Modal.Header closeButton>
          <Modal.Title>Confirm Delete</Modal.Title>
        </Modal.Header>
        <Modal.Body>Are you sure you want to remove this credit?</Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={handleCancelDelete}>
            Cancel
          </Button>
          <Button variant="danger" onClick={handleConfirmDelete}>
            Delete
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
}

// Modal thêm/sửa credit
function CreditModal({ show, onHide, data, onSubmit, userCredits }) {
  const initialValues = {
    carbonCreditId: data?.carbonCreditId ?? "",
    quantity: data?.quantity ?? "",
    pricePerCredit: data?.price ?? "",
    expirationDate: data?.expirationDate ?? "",
  };

  return (
    <Modal show={show} onHide={onHide}>
      <Modal.Header closeButton>
        <Modal.Title>{data ? "Edit Credit" : "Publish New Credit"}</Modal.Title>
      </Modal.Header>

      <Formik
        enableReinitialize
        validationSchema={schema}
        initialValues={initialValues}
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
              <Form.Group className="mb-3" controlId="formCreditId">
                <Form.Label>Select Credit</Form.Label>
                <Form.Select
                  name="carbonCreditId"
                  value={values.carbonCreditId}
                  onChange={handleChange}
                  isInvalid={touched.carbonCreditId && !!errors.carbonCreditId}
                >
                  <option value="">-- Select your credit --</option>
                  {userCredits.map((credit) => (
                    <option key={credit.id} value={credit.id}>
                      {credit.title} (Available: {credit.balance})
                    </option>
                  ))}
                </Form.Select>
                <Form.Control.Feedback type="invalid">
                  {errors.carbonCreditId}
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-3" controlId="formQuantity">
                <Form.Label>Quantity</Form.Label>
                <Form.Control
                  type="number"
                  name="quantity"
                  placeholder="Enter quantity"
                  value={values.quantity}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.quantity && !!errors.quantity}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.quantity}
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-3" controlId="formPrice">
                <Form.Label>Price per Credit ($)</Form.Label>
                <Form.Control
                  type="number"
                  name="pricePerCredit"
                  placeholder="Enter price"
                  value={values.pricePerCredit}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.pricePerCredit && !!errors.pricePerCredit}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.pricePerCredit}
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-3" controlId="formExpiration">
                <Form.Label>Expiration Date</Form.Label>
                <Form.Control
                  type="date"
                  name="expirationDate"
                  value={values.expirationDate}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.expirationDate && !!errors.expirationDate}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.expirationDate}
                </Form.Control.Feedback>
              </Form.Group>
            </Modal.Body>

            <Modal.Footer>
              <Button variant="secondary" onClick={onHide}>
                Close
              </Button>
              <Button type="submit" variant="primary">
                Submit
              </Button>
            </Modal.Footer>
          </Form>
        )}
      </Formik>
    </Modal>
  );
}
