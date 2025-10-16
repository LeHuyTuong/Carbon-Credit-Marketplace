import { useState, useEffect } from "react";
import "../../EVOwner/ManageVehicle/manage.css";
import { Button, Modal, Form, Toast, ToastContainer } from "react-bootstrap";
import { Formik } from "formik";
import * as Yup from "yup";

// validation schema
const schema = Yup.object().shape({
  quantity: Yup.number()
    .required("Quantity is required")
    .positive("Must be greater than 0"),
  price: Yup.number()
    .required("Price is required")
    .positive("Must be greater than 0"),
});

export default function ManageCredits() {
  const [credits, setCredits] = useState([]);
  const [show, setShow] = useState(false);
  const [editData, setEditData] = useState(null);
  const [confirm, setConfirm] = useState({ show: false, id: null });
  const [toast, setToast] = useState({
    show: false,
    message: "",
    variant: "success",
  });

  // fetch từ localStorage (mock backend)
  const fetchCredits = () => {
    const stored = JSON.parse(localStorage.getItem("mockCredits")) || [];
    setCredits(stored);
  };

  useEffect(() => {
    fetchCredits();
  }, []);

  // mở modal thêm credit
  const handleAdd = () => {
    setEditData(null);
    setShow(true);
  };

  // mở modal edit credit
  const handleEdit = (credit) => {
    setEditData(credit);
    setShow(true);
  };

  // xóa credit
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

  // submit (thêm hoặc sửa)
  const handleSubmit = (values) => {
    let updated = [];
    if (editData) {
      updated = credits.map((c) =>
        c.id === editData.id
          ? {
              ...c,
              ...values,
              price: +values.price,
              quantity: +values.quantity,
            }
          : c
      );
      showToast("Credit updated successfully");
    } else {
      const newCredit = {
        id: Date.now(),
        title: "EV Charging Credit",
        price: +values.price,
        quantity: +values.quantity,
        status: "active",
        createdAt: new Date().toLocaleString(),
      };
      updated = [...credits, newCredit];
      showToast("Credit added successfully");
    }

    localStorage.setItem("mockCredits", JSON.stringify(updated));
    setCredits(updated);
    setShow(false);
    setEditData(null);
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
    <>
      <div className="vehicle-search-section">
        <h1 className="title">List Your Credits For Sale</h1>
        <Button className="mb-3" onClick={handleAdd}>
          Add Credit
        </Button>
      </div>

      {/* modal thêm/sửa */}
      <CreditModal
        show={show}
        onHide={handleClose}
        onSubmit={handleSubmit}
        data={editData}
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
              <th>Created</th>
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
                      className="action-btn edit"
                      onClick={() => handleEdit(row)}
                    >
                      <i className="bi bi-pencil"></i>
                    </button>
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

      {/*modal confirm delete */}
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
    </>
  );
}

// modal thêm/sửa credit
function CreditModal({ show, onHide, data, onSubmit }) {
  const initialValues = {
    quantity: data?.quantity ?? "",
    price: data?.price ?? "",
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
                <Form.Label>Price ($)</Form.Label>
                <Form.Control
                  type="number"
                  name="price"
                  placeholder="Enter price"
                  value={values.price}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.price && !!errors.price}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.price}
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
