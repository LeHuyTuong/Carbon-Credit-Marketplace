import { useState, useEffect } from "react";
import "./manage.css";
import { Button, Modal, Form } from "react-bootstrap";
import { Formik } from "formik";
import * as Yup from "yup";
import {
  getVehicles,
  createVehicle,
  updateVehicle,
  deleteVehicle,
} from "../ManageVehicle/manageApi";

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
    if (!window.confirm("Bạn có chắc muốn xóa xe này?")) return;
    try {
      await deleteVehicle(id);
      await fetchVehicles();
    } catch (err) {
      alert("Không thể xóa xe: " + err.message);
    }
  };

  //submit (thêm hoặc sửa)
  const handleSubmit = async (values) => {
    try {
      const payload = {
        plateNumber: values.plate,
        model: values.model,
        brand: values.brand,
        manufacturer: values.company,
        yearOfManufacture: 2025, //hoặc có thể thêm field cho người dùng nhập
      };

      if (editData) {
        await updateVehicle(editData.id, payload);
      } else {
        await createVehicle({ ownerId: 1, ...payload });
      }

      await fetchVehicles();
      setShow(false);
      setEditData(null);
    } catch (err) {
      alert("Lỗi khi lưu xe: " + err.message);
    }
  };

  const handleClose = () => {
    setShow(false);
    setEditData(null);
  };

  return (
    <>
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
              <th>Manufacturer</th>
              <th>Year</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {vehicles.length > 0 ? (
              vehicles.map((row) => (
                <tr key={row.id}>
                  <td>{row.id}</td>
                  <td>{row.plateNumber}</td>
                  <td>{row.brand}</td>
                  <td>{row.model}</td>
                  <td>{row.manufacturer}</td>
                  <td>{row.yearOfManufacture}</td>
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
                <td colSpan="7" className="no-data">
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

//modal thêm/sửa xe
function VehicleModal({ show, onHide, data, onSubmit }) {
  const initialValues = {
    plate: data?.plateNumber ?? "",
    brand: data?.brand ?? "",
    model: data?.model ?? "",
    company: data?.manufacturer ?? "",
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
                <Form.Label>Manufacturer</Form.Label>
                <Form.Select
                  name="company"
                  value={values.company}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.company && !!errors.company}
                >
                  <option value="">Choose one manufacturer</option>
                  <option value="Vinfast">Vinfast</option>
                  <option value="Tesla">Tesla</option>
                  <option value="Toyota">Toyota</option>
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
