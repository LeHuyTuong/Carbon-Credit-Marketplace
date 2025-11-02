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
  selectedCredit: Yup.number()
    .required("Please select a credit")
    .typeError("Please select a credit"),
  quantity: Yup.number()
    .required("Quantity is required")
    .positive("Must be greater than 0")
    .when("maxAvailable", (maxAvailable, schema) =>
      schema.max(
        maxAvailable || 0,
        `Quantity cannot exceed available balance (${maxAvailable || 0})`
      )
    ),
  pricePerCredit: Yup.number()
    .required("Price is required")
    .positive("Must be greater than 0"),
});

export default function ListCredits() {
  const [credits, setCredits] = useState([]);
  const [userCredits, setUserCredits] = useState([]);
  const [show, setShow] = useState(false);
  const [editData, setEditData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [confirmDelete, setConfirmDelete] = useState({ show: false, id: null });
  const [toast, setToast] = useState({
    show: false,
    message: "",
    variant: "success",
  });
  const nav = useNavigate();
  const sectionRef = useRef(null);
  useReveal(sectionRef);

  //fetch user credits
  useEffect(() => {
    const fetchUserCredits = async () => {
      try {
        const [batchRes, walletRes] = await Promise.all([
          apiFetch("/api/v1/my/credits/batches", { method: "GET" }),
          apiFetch("/api/v1/wallet", { method: "GET" }),
        ]);

        const allBatches = batchRes?.response?.content || [];
        const wallet = walletRes?.response || {};
        const walletCredits = wallet.carbonCredits || [];

        // ===credits được cấp (ISSUED) ===
        const issuedCredits = allBatches
          .filter((b) => b.status === "ISSUED")
          .map((b) => ({
            id: b.id,
            type: "ISSUED",
            title: `${b.projectTitle || "Untitled"} (${b.batchCode || "-"})`,
            balance: Number(b.creditsCount ?? 0),
            expiresAt: b.expiresAt || null, // từ batch API
            issuedAt: new Date(b.issuedAt).toLocaleDateString("en-GB"),
          }));

        // ===credits đã mua (TRADED) ===
        const tradedCredits = walletCredits
          .filter((c) => c.status === "TRADED")
          .map((c) => ({
            id: c.creditId,
            type: "WALLET",
            title: `${c.creditCode || "Carbon Credit"} (From ${
              c.originCompanyName || "Unknown"
            })`,
            balance: Number(c.availableQuantity ?? c.ownedQuantity ?? 0),
            expiresAt: c.expirationDate || null,
          }));

        setUserCredits([...issuedCredits, ...tradedCredits]);
      } catch (err) {
        console.error("Failed to fetch user credits:", err);
        setUserCredits([]);
      }
    };

    fetchUserCredits();
  }, []);

  //fetch list credit(Marketplace)
  useEffect(() => {
    fetchCredits();
  }, []);

  const fetchCredits = async () => {
    try {
      setLoading(true);
      const res = await apiFetch("/api/v1/marketplace/company", {
        method: "GET",
      });
      const list = res?.response || [];
      const mapped = list.map((item) => ({
        id: item.listingId,
        title: item.projectTitle || "Unnamed Project",
        price: item.pricePerCredit,
        quantity: item.quantity,
        seller: item.sellerCompanyName,
        expiresAt: item.expiresAt || null,
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

  //mở modal list credits
  const handleList = () => {
    setEditData(null);
    setShow(true);
  };

  //mở modal update
  const handleEdit = (credits) => {
    setEditData(credits);
    setShow(true);
  };

  //xóa list credits
  const handleDeleteClick = (id) => {
    setConfirmDelete({ show: true, id });
  };

  const confirmDeleteAction = async () => {
    try {
      await deleteListing(confirmDelete.id);
      await fetchCredits();
      showToast("Credits deleted successfully");
    } catch (err) {
      showToast("Cannot delete credits: " + err.message, "danger");
    } finally {
      setConfirmDelete({ show: false, id: null });
    }
  };

  //handle submit( thêm hoặc sửa)
  const handleSubmit = async (values) => {
    try {
      setLoading(true);

      const payload = {
        quantity: Number(values.quantity),
        pricePerCredit: Number(values.pricePerCredit),
      };

      // Nếu là credit được cấp thì dùng batchId
      if (values.type === "ISSUED") {
        payload.batchId = Number(values.batchId);
      }
      // Nếu là credit mua trong ví thì dùng creditId
      else if (values.type === "WALLET") {
        payload.carbonCreditId = Number(values.creditId);
      } else {
        throw new Error("Invalid credit type.");
      }

      if (editData) {
        await updateListing(editData.id, {
          pricePerCredit: payload.pricePerCredit,
        });
        showToast("Credit updated successfully!");
      } else {
        await apiFetch("/api/v1/marketplace", {
          method: "POST",
          body: { data: payload },
        });
        showToast("Credit listed successfully!");
      }

      setShow(false);
      setEditData(null);
      await fetchCredits();
    } catch (error) {
      console.error("Submit error:", error);
      showToast(error.message || "Failed to publish credit.", "danger");
    } finally {
      setLoading(false);
    }
  };

  const showToast = (message, variant = "success") => {
    setToast({ show: true, message, variant });
    setTimeout(() => setToast({ show: false, message: "", variant }), 3000);
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
        <Button className="mb-3" onClick={handleList}>
          List Credit
        </Button>
      </div>

      {userCredits.length === 0 && (
        <p className="text-warning small">
          You don’t have any available credits to list yet.
        </p>
      )}

      {loading ? (
        <div className="d-flex justify-content-center align-items-center py-5">
          <Spinner animation="border" />
        </div>
      ) : (
        <div className="table-wrapper">
          <table className="vehicle-table">
            <thead>
              <tr>
                <th>#</th>
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
              itemsPerPage={5}
              renderRow={(row, index) => (
                <tr key={row.id}>
                  <td>{index + 1}</td>
                  <td>{row.title}</td>
                  <td>${row.price}</td>
                  <td>{row.quantity}</td>
                  <td>{row.seller}</td>
                  <td>
                    {row.expiresAt
                      ? new Date(row.expiresAt).toLocaleDateString("en-GB")
                      : "N/A"}
                  </td>
                  <td>
                    <button
                      className="action-btn view"
                      onClick={() => nav(`/detail-credit/${row.id}`)}
                    >
                      <i className="bi bi-eye"></i>
                    </button>
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
              )}
            />
          </table>
        </div>
      )}

      <CreditModal
        show={show}
        onHide={() => setShow(false)}
        onSubmit={handleSubmit}
        userCredits={userCredits}
        editData={editData}
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

      <Modal
        show={confirmDelete.show}
        onHide={() => setConfirmDelete({ show: false, id: null })}
        centered
      >
        <Modal.Header closeButton>
          <Modal.Title>Confirm Delete</Modal.Title>
        </Modal.Header>
        <Modal.Body>Are you sure you want to delete these credits?</Modal.Body>
        <Modal.Footer>
          <Button
            variant="secondary"
            onClick={() => setConfirmDelete({ show: false, id: null })}
          >
            Cancel
          </Button>
          <Button variant="danger" onClick={confirmDeleteAction}>
            Delete
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
}
export const updateListing = async (listingId, data) => {
  return await apiFetch(`/api/v1/marketplace`, {
    method: "PUT",
    body: {
      data: {
        listingId,
        pricePerCredit: data.pricePerCredit,
      },
    },
  });
};

export const deleteListing = async (listingId) => {
  return await apiFetch(`/api/v1/marketplace/${listingId}`, {
    method: "DELETE",
  });
};

//modal up credit
function CreditModal({ show, onHide, onSubmit, userCredits, editData }) {
  const initialValues = editData
    ? {
        selectedCredit: editData.id,
        quantity: editData.quantity || "",
        pricePerCredit: editData.price || "",
        expirationDate: editData.expiresAt || "",
        maxAvailable: editData.quantity || 0,

        type: userCredits.find((c) => c.id === editData.id)?.type || "ISSUED",
        batchId:
          userCredits.find((c) => c.id === editData.id)?.type === "ISSUED"
            ? editData.id
            : null,
        creditId:
          userCredits.find((c) => c.id === editData.id)?.type === "WALLET"
            ? editData.id
            : null,
      }
    : {
        selectedCredit: "",
        quantity: "",
        pricePerCredit: "",
        expirationDate: "",
        maxAvailable: 0,
      };

  return (
    <Modal show={show} onHide={onHide} centered>
      <Modal.Header closeButton>
        <Modal.Title>
          {editData ? "Edit Credits" : "List New Credits"}
        </Modal.Title>
      </Modal.Header>

      <Formik
        validationSchema={schema}
        initialValues={initialValues}
        onSubmit={(values) => onSubmit(values)}
        context={{ userCredits }}
      >
        {({
          handleSubmit,
          handleChange,
          values,
          errors,
          touched,
          setFieldValue,
        }) => (
          <Form noValidate onSubmit={handleSubmit}>
            <Modal.Body>
              <Form.Group className="mb-3">
                <Form.Label>Select Credit</Form.Label>
                <Form.Select
                  name="selectedCredit"
                  value={values.selectedCredit}
                  onChange={(e) => {
                    handleChange(e);
                    const selected = userCredits.find(
                      (credit) => String(credit.id) === e.target.value
                    );

                    //cập nhật state formik
                    setFieldValue("type", selected?.type || "");
                    setFieldValue(
                      "expirationDate",
                      selected?.expiresAt || selected?.expirationDate || ""
                    );
                    setFieldValue("batchId", null);
                    setFieldValue("creditId", null);
                    setFieldValue("maxAvailable", selected?.balance ?? 0);

                    if (selected?.type === "ISSUED") {
                      setFieldValue("batchId", selected.id);
                    } else if (selected?.type === "WALLET") {
                      setFieldValue("creditId", selected.id);
                    }
                  }}
                  disabled={!!editData}
                  isInvalid={touched.selectedCredit && !!errors.selectedCredit}
                >
                  <option value="">-- Select your credit --</option>

                  <optgroup label="Credits Granted (Batch)">
                    {userCredits
                      .filter((c) => c.type === "ISSUED" && c.balance > 0)
                      .map((credit) => (
                        <option key={`batch-${credit.id}`} value={credit.id}>
                          {credit.title} (Available: {credit.balance})
                        </option>
                      ))}
                  </optgroup>

                  <optgroup label="Credits Purchased">
                    {userCredits
                      .filter((c) => c.type === "WALLET" && c.balance > 0)
                      .map((credit) => (
                        <option key={`wallet-${credit.id}`} value={credit.id}>
                          {credit.title} (Quantity: {credit.balance})
                        </option>
                      ))}
                  </optgroup>
                </Form.Select>
                <Form.Control.Feedback type="invalid">
                  {errors.selectedCredit}
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
                <Form.Text className="text-muted">
                  Max available:{" "}
                  {userCredits.find(
                    (c) => String(c.id) === values.selectedCredit
                  )?.balance ?? 0}
                </Form.Text>
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
                  type="text"
                  name="expirationDate"
                  value={
                    values.expirationDate &&
                    !isNaN(Date.parse(values.expirationDate))
                      ? new Date(values.expirationDate).toLocaleDateString(
                          "en-GB"
                        )
                      : values.expirationDate || "-"
                  }
                  readOnly
                  disabled
                />
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
