import { useState } from "react";
import "./manage.css";
import { Button, Modal, Form } from "react-bootstrap";
import { Formik } from "formik";
import * as Yup from "yup";

const schema = Yup.object().shape({
  plate: Yup.string().required("License plate is required"),
  brand: Yup.string().required("Brand is required"),
  model: Yup.string().required("Model is required"),
  company: Yup.string().required("Company is required"),
});

export default function Manage() {
  const [vehicles, setVehicles] = useState([
    { id: 1, code: "EV-0001", plate: "30H-123.45", brand: "VinFast", model: "VF e34", company: "Vinfast", credits: "0.2" },
    { id: 2, code: "EV-0002", plate: "30H-999.99", brand: "Tesla", model: "Model 3", company: "Tesla", credits: "0.4" },
  ]);

  const [show, setShow] = useState(false);
  const [editData, setEditData] = useState(null);

  const handleClose = () => setShow(false);
  const handleAdd = () => {
    setEditData(null);
    setShow(true);
  };

  const handleDelete = (id) => {
    setVehicles((prev) => prev.filter((v) => v.id !== id));
  };

  const handleEdit = (vehicle) => {
    setEditData(vehicle);
    setShow(true);
  };

  const handleSubmit = (values) => {
    if (editData) {
      //edit xe
      setVehicles((prev) =>
        prev.map((v) => (v.id === editData.id ? { ...v, ...values } : v))
      );
    } else {
      //add xe
      const newVehicle = {
        ...values,
        id: Date.now(),
        code: "EV-" + String(Math.floor(Math.random() * 10000)).padStart(4, "0"),
      };
      setVehicles((prev) => [...prev, newVehicle]);
    }
    setShow(false);
    setEditData(null);
  };

  return (
    <>
      <div className="vehicle-search-section">
        <h1 className="title">Your vehicles</h1>
        <Button className="mb-3" onClick={handleAdd}>
          Add Vehicle
        </Button>
      </div>

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
              <th>Vehicle ID</th>
              <th>License Plate</th>
              <th>Brand</th>
              <th>Model</th>
              <th>Company</th>
              <th>Credits</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {vehicles.length > 0 ? (
              vehicles.map((row) => (
                <tr key={row.id}>
                  <td>{row.code}</td>
                  <td>{row.plate}</td>
                  <td>{row.brand}</td>
                  <td>{row.model}</td>
                  <td>{row.company}</td>
                  <td>{row.credits}</td>
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
              ))
            ) : (
              <tr>
                <td colSpan="10" className="no-data">
                  <h5>No vehicles yet</h5>
                  <p>Add your vehicle to get started.</p>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  );
}

function VehicleModal({ show, onHide, data, onSubmit }) {
  const initialValues = {
    plate: data?.plate ?? "",
    brand: data?.brand ?? "",
    model: data?.model ?? "",
    company: data?.company ?? "",
  };

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
                <Form.Label>Company</Form.Label>
                <Form.Select
                  name="company"
                  value={values.company}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.company && !!errors.company}
                >
                  <option value="">Choose one company</option>
                  <option value="Vinfast">Vinfast</option>
                  <option value="Tesla">Tesla</option>
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