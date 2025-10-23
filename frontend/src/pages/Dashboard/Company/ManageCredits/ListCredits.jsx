import { useState, useEffect, useRef } from "react";
import "../../EVOwner/ManageVehicle/manage.css";
import {
  Button,
  Modal,
  Form,
  Toast,
  ToastContainer,
  Spinner,
} from "react-bootstrap";
import { Formik } from "formik";
import * as Yup from "yup";
import useReveal from "../../../../hooks/useReveal";
import { FaArrowLeft } from "react-icons/fa";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../../../../utils/apiFetch";
import "../Marketplace/marketplace.css";
import PaginatedTable from "../../../../components/Pagination/PaginatedTable";

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

export default function ListCredits() {
  const [credits, setCredits] = useState([]);
  const [userCredits, setUserCredits] = useState([]); // mock dữ liệu user
  const [show, setShow] = useState(false);
  const [loading, setLoading] = useState(true);
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

  useEffect(() => {
    fetchCredits();
  }, []);

  // fetch danh sách credit đang bán
  const fetchCredits = async () => {
    try {
      setLoading(true);
      const res = await apiFetch("/api/v1/marketplace", { method: "GET" });
      const list = res?.response || [];
      const mapped = list.map((item) => ({
        id: item.listingId,
        title: item.projectTitle || "Unnamed Project",
        price: item.pricePerCredit,
        quantity: item.quantity,
        seller: item.sellerCompanyName,
        expiresAt: new Date(item.expiresAt).toLocaleDateString("en-GB"),
        status: "active",
      }));
      setCredits(mapped);
    } catch (err) {
      console.error("Failed to fetch marketplace:", err);
      showToast(err.message || "Unable to load credits.", "danger");
    } finally {
      setLoading(false);
    }
  };

  // submit đăng bán credit
  const handleSubmit = async (values) => {
    try {
      setLoading(true);
      await apiFetch("/api/v1/marketplace", {
        method: "POST",
        body: {
          data: {
            carbonCreditId: Number(values.carbonCreditId),
            quantity: Number(values.quantity),
            pricePerCredit: Number(values.pricePerCredit),
            expirationDate: new Date(values.expirationDate).toISOString(),
          },
        },
      });

      showToast("Credit listed successfully!");
      setShow(false);
      await fetchCredits(); // reload sau khi đăng
    } catch (error) {
      console.error("Submit error:", error);
      showToast(error.message || "Failed to publish credit.", "danger");
    } finally {
      setLoading(false);
    }
  };

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
      {/* Back button */}
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
        <Button className="mb-3" onClick={() => setShow(true)}>
          Add Credit
        </Button>
      </div>

      {loading ? (
        <div className="d-flex justify-content-center align-items-center py-5">
          <Spinner animation="border" />
        </div>
      ) : (
        <div className="table-wrapper">
          <table className="vehicle-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Title</th>
                <th>Price ($)</th>
                <th>Quantity</th>
                <th>Seller</th>
                <th>Expires At</th>
                <th>Actions</th>
              </tr>
            </thead>
            <PaginatedTable
              items={credits}
              itemsPerPage={5} // tuỳ chỉnh số item mỗi trang
              renderRow={(row, index) => (
                <tr key={row.id}>
                  <td>{index + 1}</td>
                  <td>{row.title}</td>
                  <td>${row.price}</td>
                  <td>{row.quantity}</td>
                  <td>{row.seller}</td>
                  <td>{row.expiresAt}</td>
                  <td>
                    <button
                      className="btn-detail w-90"
                      onClick={() => nav(`/credit-detail/${row.id}`)}
                    >
                      <i></i> View Detail
                    </button>
                  </td>
                </tr>
              )}
            />
          </table>
        </div>
      )}

      <CreditModal
        show={show}
        onHide={handleClose}
        onSubmit={handleSubmit}
        userCredits={userCredits}
      />

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
    </div>
  );
}

// Modal đăng credit thật
function CreditModal({ show, onHide, onSubmit, userCredits }) {
  const initialValues = {
    carbonCreditId: "",
    quantity: "",
    pricePerCredit: "",
    expirationDate: "",
  };

  return (
    <Modal show={show} onHide={onHide} centered>
      <Modal.Header closeButton>
        <Modal.Title>Publish New Credit</Modal.Title>
      </Modal.Header>

      <Formik
        validationSchema={schema}
        initialValues={initialValues}
        onSubmit={(values) => onSubmit(values)}
      >
        {({ handleSubmit, handleChange, values, errors, touched }) => (
          <Form noValidate onSubmit={handleSubmit}>
            <Modal.Body>
              <Form.Group className="mb-3">
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

              <Form.Group className="mb-3">
                <Form.Label>Quantity</Form.Label>
                <Form.Control
                  type="number"
                  name="quantity"
                  placeholder="Enter quantity"
                  value={values.quantity}
                  onChange={handleChange}
                  isInvalid={touched.quantity && !!errors.quantity}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.quantity}
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Price per Credit ($)</Form.Label>
                <Form.Control
                  type="number"
                  name="pricePerCredit"
                  placeholder="Enter price"
                  value={values.pricePerCredit}
                  onChange={handleChange}
                  isInvalid={touched.pricePerCredit && !!errors.pricePerCredit}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.pricePerCredit}
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Expiration Date</Form.Label>
                <Form.Control
                  type="date"
                  name="expirationDate"
                  value={values.expirationDate}
                  onChange={handleChange}
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
