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

// === Validation schema ===
const schema = Yup.object().shape({
  selectedCredit: Yup.string().required("Please select a credit"),
  quantity: Yup.number()
    .required("Quantity is required")
    .positive("Must be greater than 0")
    .when("maxAvailable", (max, schema) =>
      schema.max(max || 0, `Cannot exceed available balance (${max || 0})`)
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

  // === FETCH USER CREDITS (từ ví, group theo batchCode) ===
  const fetchUserCredits = async () => {
    try {
      setLoading(true);
      const res = await apiFetch("/api/v1/wallet", { method: "GET" });
      const wallet = res?.response || {};
      const credits = wallet.carbonCredits || [];

      // lấy credit còn khả dụng
      const available = credits.filter(
        (c) =>
          (c.status === "AVAILABLE" ||
            c.status === "TRADED" ||
            c.status === "LISTED") &&
          c.availableQuantity > 0
      );

      // group theo batchCode
      const grouped = Object.values(
        available.reduce((acc, c) => {
          if (!acc[c.batchCode]) {
            acc[c.batchCode] = {
              id: c.batchCode,
              batchCode: c.batchCode,
              batchId: c.batchId,
              projectTitle: c.originCompanyName || "Unknown Project",
              balance: 0,
              expiresAt: c.expirationDate || null,
              creditIds: [],
            };
          }

          acc[c.batchCode].balance +=
            c.availableQuantity || c.ownedQuantity || 0;
          acc[c.batchCode].creditIds.push(c.creditId);

          return acc;
        }, {})
      );

      const mapped = grouped.map((g) => {
        const sample = credits.find((c) => c.batchCode === g.batchCode) || {};
        const batchCredits = credits.filter((c) => c.batchCode === g.batchCode);
        const hasAvailable = batchCredits.some((c) => c.status === "AVAILABLE");
        const batchStatus = hasAvailable ? "AVAILABLE" : "TRADED";
        return {
          id: g.batchCode,
          batchCode: g.batchCode,
          batchId: g.batchId,
          title: `${g.projectTitle} (${g.batchCode})`,
          balance: g.balance,
          expiresAt: g.expiresAt,
          type: "WALLET",
          creditIds: g.creditIds,
          originCompanyId: sample.originCompanyId,
          sellerCompanyId: sample.sellerCompanyId,
          status: sample.batchStatus,
        };
      });

      setUserCredits(mapped);
    } catch (err) {
      console.error("Failed to fetch wallet credits:", err);
      setUserCredits([]);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    fetchUserCredits();
  }, []);

  // === FETCH COMPANY’S MARKETPLACE LISTINGS ===
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
        totalPrice: item.totalPrice,
        listedQuantity: item.originalQuantity,
        soldQuantity: item.soldQuantity,
        availableQuantity: item.quantity,
        seller: item.sellerCompanyName,
        expiresAt: item.expiresAt || null,
      }));
      setCredits(mapped);
    } catch (err) {
      console.error("Failed to fetch marketplace:", err);
      showToast(err.message || "Unable to load credits.", "danger");
    } finally {
      setLoading(false);
    }
  };

  // === Modal controls ===
  const handleList = () => {
    setEditData(null);
    setShow(true);
  };

  const handleEdit = (credit) => {
    setEditData(credit);
    setShow(true);
  };

  const handleDeleteClick = (id) => {
    setConfirmDelete({ show: true, id });
  };

  const confirmDeleteAction = async () => {
    try {
      await deleteListing(confirmDelete.id);
      await fetchCredits();
      await fetchUserCredits(); //reload credit trong ví
      showToast("Credits deleted successfully");
    } catch (err) {
      showToast("Cannot delete credits: " + err.message, "danger");
    } finally {
      setConfirmDelete({ show: false, id: null });
    }
  };

  // === Handle Submit (list or edit) ===
  const handleSubmit = async (values) => {
    try {
      setLoading(true);

      // edit: chỉ update price, không check balance
      if (editData) {
        await updateListing(editData.id, {
          pricePerCredit: values.pricePerCredit,
        });

        showToast("Credit updated successfully!");
        setShow(false);
        setEditData(null);

        await fetchCredits();
        return;
      }

      //list
      const selectedCreditObj = userCredits.find(
        (c) => c.id === values.selectedCredit
      );

      if (!selectedCreditObj) throw new Error("Invalid credit selection");
      if ((selectedCreditObj?.balance || 0) < values.quantity) {
        throw new Error("Not enough available credits in this batch.");
      }

      // --- Tạo payload động ---
      const payload = {
        quantity: Number(values.quantity),
        pricePerCredit: Number(values.pricePerCredit),
      };

      // Nếu là credit trong ví
      if (selectedCreditObj.type === "WALLET") {
        // Credit mua về: status === "TRADED" hoặc có sellerCompanyId khác null
        const isPurchased =
          selectedCreditObj.status === "TRADED" ||
          selectedCreditObj.sellerCompanyId !== null;

        if (isPurchased) {
          // credit mua: list theo carbonCreditId (1 credit duy nhất)
          payload.carbonCreditId = selectedCreditObj.creditIds[0];
        } else {
          // credit được cấp: list theo batchId (cho phép nhiều)
          payload.batchId = selectedCreditObj.batchId;
          // Thêm danh sách các creditIds tương ứng với quantity
          const selectedIds = selectedCreditObj.creditIds.slice(
            0,
            Number(values.quantity)
          );

          payload.carbonCreditIds = selectedIds;
        }
      }
      console.log("selectedCreditObj", selectedCreditObj);

      // --- Gửi lên backend ---
      if (editData) {
        await updateListing(editData.id, {
          pricePerCredit: payload.pricePerCredit,
        });
        showToast("Credit updated successfully!");
      } else {
        await apiFetch("/api/v1/marketplace", {
          method: "POST",
          body: { data: payload }, //gửi đúng payload, không ép batchId
        });
        showToast("Credit listed successfully!");
      }

      setShow(false);
      setEditData(null);
      await fetchCredits();
      await fetchUserCredits();
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
                <th>Price Per Credit ($)</th>
                <th>Total Price ($)</th>
                <th>Listed Quantity</th>
                <th>Sold Quantity</th>
                <th>Available Quantity</th>
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
                  <td>${row.totalPrice}</td>
                  <td>{row.listedQuantity}</td>
                  <td>{row.soldQuantity}</td>
                  <td>{row.availableQuantity}</td>
                  <td>{row.seller}</td>
                  <td>
                    {row.expiresAt
                      ? new Date(row.expiresAt).toLocaleDateString("en-GB")
                      : "N/A"}
                  </td>
                  <td>
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

// === Modal ===
function CreditModal({ show, onHide, onSubmit, userCredits, editData }) {
  const initialValues = editData
    ? {
        selectedCredit: userCredits[0]?.id || "",
        quantity: editData.quantity || "",
        pricePerCredit: editData.price || "",
        expirationDate: editData.expiresAt || "",
        maxAvailable: editData.quantity || 0,
        creditIds: editData.creditIds || [],
      }
    : {
        selectedCredit: "",
        quantity: "",
        pricePerCredit: "",
        expirationDate: "",
        maxAvailable: 0,
        creditIds: [],
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
                    setFieldValue("creditIds", selected?.creditIds || []);
                    setFieldValue("expirationDate", selected?.expiresAt || "");
                    setFieldValue("maxAvailable", selected?.balance ?? 0);
                  }}
                  disabled={!!editData}
                  isInvalid={touched.selectedCredit && !!errors.selectedCredit}
                >
                  <option value="">-- Select your credit --</option>
                  {userCredits
                    .filter((c) => c.balance > 0)
                    .map((credit) => (
                      <option key={credit.id} value={credit.id}>
                        {credit.title} (Available: {credit.balance})
                      </option>
                    ))}
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
                  disabled={!!editData}
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
                    values.expirationDate
                      ? new Date(values.expirationDate).toLocaleDateString(
                          "en-GB"
                        )
                      : "-"
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
