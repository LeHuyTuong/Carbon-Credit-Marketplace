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
  const [userCredits, setUserCredits] = useState([]);
  const [show, setShow] = useState(false);
  const [loading, setLoading] = useState(true);
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
         const walletRes = await apiFetch("/api/v1/wallet", {
          method: "GET",
        });

        const wallet = walletRes?.response;

        const walletCredits = (wallet?.carbonCredits || [])
          .filter((credit) => {
            if (!credit?.creditId) {
              return false;
            }
            const availableRaw =
              credit.availableQuantity ?? credit.ownedQuantity ?? 0;
            const available = Number(availableRaw);
            return !Number.isNaN(available) && available > 0;
          })
          .map((credit) => {
            const availableRaw =
              credit.availableQuantity ?? credit.ownedQuantity ?? 0;
            const available = Number(availableRaw);
            const labelParts = [
              credit.creditCode || credit.batchCode || "Carbon Credit",
            ];
            if (credit.batchCode) {
              labelParts.push(`Batch ${credit.batchCode}`);
            }

            return {
              id: credit.creditId,
              title: labelParts.join(" · "),
              balance: available,
              type: "WALLET",
            };
          });

        setUserCredits(walletCredits);
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
      const res = await apiFetch("/api/v1/marketplace", { method: "GET" });
      const list = res?.response || [];
      const mapped = list.map((item) => ({
        id: item.listingId,
        title: item.projectTitle || "Unnamed Project",
        price: item.pricePerCredit,
        quantity: item.quantity,
        seller: item.sellerCompanyName,
        expiresAt:
          item.expiresAt && !isNaN(new Date(item.expiresAt))
            ? new Date(item.expiresAt).toLocaleDateString("en-GB")
            : "N/A",
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

  //handle submit
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
        <Button className="mb-3" onClick={() => setShow(true)}>
          List Credit
        </Button>
      </div>

      {userCredits.length === 0 && (
        <p className="text-warning small">
          You don't have any issued credits yet. Wait for admin approval.
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
                  <td>{row.expiresAt}</td>
                  <td>
                    <button
                      className="btn-detail w-90"
                      onClick={() => nav(`/credit-detail/${row.id}`)}
                    >
                      View Detail
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

//modal up credit
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
