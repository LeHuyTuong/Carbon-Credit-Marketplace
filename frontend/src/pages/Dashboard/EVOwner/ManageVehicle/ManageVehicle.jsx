import { useState, useEffect, useRef } from "react";
import "./manage.css";
import { Button, Modal, Form, Toast, ToastContainer } from "react-bootstrap";
import { Formik } from "formik";
import * as Yup from "yup";
import useReveal from "../../../../hooks/useReveal";
import { getApprovedCompanies } from "../ManageVehicle/manageApi";

import {
  getVehicles,
  createVehicle,
  updateVehicle,
  deleteVehicle,
} from "../ManageVehicle/manageApi";
import PaginatedTable from "../../../../components/Pagination/PaginatedTable";

//validation schema
const schema = Yup.object().shape({
  plate: Yup.string().required("License plate is required"),
  brand: Yup.string().required("Brand is required"),
  model: Yup.string().required("Model is required"),
  company: Yup.string().required("Company is required"),
});

export default function Manage() {
  const [vehicles, setVehicles] = useState([]);
  const [show, setShow] = useState(false);
  const [editData, setEditData] = useState(null);
  const [toast, setToast] = useState({
    show: false,
    message: "",
    variant: "success",
  });
  const sectionRef = useRef(null);
  useReveal(sectionRef);

  //lấy danh sách xe
  const fetchVehicles = async () => {
    try {
      const res = await getVehicles();
      setVehicles(res.response || []);
    } catch (err) {
      console.error("Lỗi khi tải danh sách xe:", err.message);
    }
  };

  useEffect(() => {
    fetchVehicles();
  }, []);

  //mở modal thêm xe
  const handleAdd = () => {
    setEditData(null);
    setShow(true);
  };

  //mở modal update
  const handleEdit = (vehicle) => {
    setEditData(vehicle);
    setShow(true);
  };

  //xóa xe
  const handleDelete = async (id) => {
    if (!window.confirm("Are you sure to delete this vehicle?")) return;
    try {
      await deleteVehicle(id);
      await fetchVehicles();
      showToast("Vehicle deleted successfully");
    } catch (err) {
      showToast("Cannot delete vehicle: " + err.message, "danger");
    }
  };

  //submit (thêm hoặc sửa)
  const handleSubmit = async (values) => {
    try {
      const payload = {
        plateNumber: values.plate,
        model: values.model,
        brand: values.brand,
        companyId: Number(values.company), //đổi từ string sang số
      };

      if (editData) {
        await updateVehicle(editData.id, payload);
      } else {
        await createVehicle(payload);
      }

      await fetchVehicles();
      showToast("Vehicle saved successfully");
      setShow(false);
      setEditData(null);
    } catch (err) {
      // Handle BE logical errors or HTTP errors
      if (
        err.code === "409" ||
        err.message.includes("Vehicle plate already exists")
      ) {
        showToast(
          "This license plate already exists. Please use a unique plate.",
          "danger"
        );
      } else {
        showToast("Failed to save vehicle: " + err.message, "danger");
      }
      console.error("Vehicle save error:", err);
    }
  };

  const handleClose = () => {
    setShow(false);
    setEditData(null);
  };

  const showToast = (message, variant = "success") => {
    setToast({ show: true, message, variant });
  };

  return (
    <div ref={sectionRef} className="reveal">
      <div className="vehicle-search-section">
        <h1 className="title">Your Vehicles</h1>
        <Button className="mb-3" onClick={handleAdd}>
          Add Vehicle
        </Button>
      </div>

      {/*modal thêm/sửa */}
      <VehicleModal
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
              <th>License Plate</th>
              <th>Brand</th>
              <th>Model</th>
              <th>Company ID</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <PaginatedTable
              items={vehicles}
              itemsPerPage={5}
              renderRow={(row, index) => (
                <tr key={row.id}>
                  <td>{index + 1}</td>
                  <td>{row.plateNumber}</td>
                  <td>{row.brand}</td>
                  <td>{row.model}</td>
                  <td>{row.companyId}</td>
                  <td className="action-buttons">
                    <button
                      className="action-btn edit"
                      onClick={() => handleEdit(row)}
                    >
                      <i className="bi bi-pencil"></i>
                    </button>
                    <button
                      className="action-btn delete"
                      onClick={() => handleDelete(row.id)}
                    >
                      <i className="bi bi-trash"></i>
                    </button>
                  </td>
                </tr>
              )}
              renderEmpty={() => (
                <>
                  <i className="bi bi-car-front text-accent fs-3 d-block mb-2"></i>
                  <h5 className="text-dark">No vehicles yet</h5>
                  <p className="text-muted">Add your vehicle to get started.</p>
                </>
              )}
            />
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
    </div>
  );
}

//modal thêm/sửa xe
function VehicleModal({ show, onHide, data, onSubmit }) {
  const [companies, setCompanies] = useState([]);

  const initialValues = {
    plate: data?.plateNumber ?? "",
    brand: data?.brand ?? "",
    model: data?.model ?? "",
    company: data?.companyId ?? "",
  };

  useEffect(() => {
    const fetchCompanies = async () => {
      try {
        const list = await getApprovedCompanies();
        setCompanies(list);
      } catch (err) {
        console.error("Error when loading list company:", err.message);
      }
    };
    fetchCompanies();
  }, []);

  return (
    <Modal show={show} onHide={onHide}>
      <Modal.Header closeButton>
        <Modal.Title>
          {data ? "Edit Vehicle" : "Register New Vehicle"}
        </Modal.Title>
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
              <Form.Group className="mb-3" controlId="formPlate">
                <Form.Label>License Plate</Form.Label>
                <Form.Control
                  name="plate"
                  placeholder="Enter license plate"
                  value={values.plate}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.plate && !!errors.plate}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.plate}
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-3" controlId="formBrand">
                <Form.Label>Brand</Form.Label>
                <Form.Control
                  name="brand"
                  placeholder="Enter brand"
                  value={values.brand}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.brand && !!errors.brand}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.brand}
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-3" controlId="formModel">
                <Form.Label>Model</Form.Label>
                <Form.Control
                  name="model"
                  placeholder="Enter model"
                  value={values.model}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.model && !!errors.model}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.model}
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-3" controlId="formCompany">
                <Form.Label>Company ID</Form.Label>
                <Form.Select
                  name="company"
                  value={values.company}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.company && !!errors.company}
                >
                  {companies.length > 0 ? (
                    <>
                      <option value="">Choose one company</option>
                      {companies.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.name}
                        </option>
                      ))}
                    </>
                  ) : (
                    <option value="">No company available</option>
                  )}
                </Form.Select>
                <Form.Control.Feedback type="invalid">
                  {errors.company}
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
