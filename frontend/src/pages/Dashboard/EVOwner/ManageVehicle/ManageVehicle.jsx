import { useState, useEffect, useRef } from "react";
import "./manage.css";
import {
  Button,
  Modal,
  Form,
  Toast,
  ToastContainer,
  OverlayTrigger,
  Tooltip,
} from "react-bootstrap";
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
  acceptRules: Yup.boolean().oneOf(
    [true],
    "You must accept the credit-sharing rules"
  ),
});

export default function Manage() {
  const [vehicles, setVehicles] = useState([]);
  const [show, setShow] = useState(false);
  const [editData, setEditData] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null); // chứa vehicle muốn xóa
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [loading, setLoading] = useState(true);

  //state hiển thị thông báo (Toast)
  const [toast, setToast] = useState({
    show: false,
    message: "",
    variant: "success",
  });
  //ref để áp dụng hiệu ứng xuất hiện
  const sectionRef = useRef(null);
  useReveal(sectionRef);

  //lấy danh sách xe và gắn tên công ty tương ứng
  const fetchVehicles = async () => {
    try {
      setLoading(true);
      //lấy song song danh sách xe và danh sách công ty được duyệt
      const [vehicleRes, companies] = await Promise.all([
        getVehicles(),
        getApprovedCompanies(),
      ]);

      const vehiclesData = vehicleRes.response || [];

      //map companyId của mỗi xe sang tên công ty
      const mappedVehicles = vehiclesData.map((v) => {
        const company = companies.find((c) => c.id === v.companyId);
        return {
          ...v,
          companyName: company ? company.name : "Unknown",
        };
      });

      setVehicles(mappedVehicles);
    } catch (err) {
      console.error("Error loading list vehicles:", err.message);
      setVehicles([]);
    } finally {
      setLoading(false);
    }
  };

  //lấy danh sách xe khi component được render lần đầu
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

  //xác nhận xóa xe trong modal
  const confirmDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteVehicle(deleteTarget.id);
      await fetchVehicles();
      showToast("Vehicle deleted successfully");
    } catch (err) {
      showToast("Cannot delete vehicle: " + err.message, "danger");
    } finally {
      setShowDeleteModal(false);
      setDeleteTarget(null);
    }
  };

  //mở modal xác nhận xóa
  const handleDelete = (vehicle) => {
    setDeleteTarget(vehicle);
    setShowDeleteModal(true);
  };

  //submit (thêm hoặc sửa)
  const handleSubmit = async (values) => {
    try {
      //chuẩn hóa dữ liệu trước khi gửi lên API
      const payload = {
        plateNumber: values.plate,
        model: values.model,
        brand: values.brand,
        companyId: Number(values.company), //đổi từ string sang số
        documentFile: values.image,
      };

      //nếu có editData -> cập nhật, ngược lại -> tạo mới
      if (editData) {
        await updateVehicle(editData.id, payload);
      } else {
        await createVehicle(payload);
      }

      //cập nhật lại danh sách xe
      await fetchVehicles();
      showToast("Vehicle saved successfully");
      setShow(false);
      setEditData(null);
    } catch (err) {
      //xử lý lỗi
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

  //dóng modal thêm/sửa
  const handleClose = () => {
    setShow(false);
    setEditData(null);
  };

  //hiển thị thông báo
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

      {/*bảng hiển thị danh sách xe */}
      <div className="table-wrapper">
        <table className="vehicle-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>License Plate</th>
              <th>Brand</th>
              <th>Model</th>
              <th>Company</th>
              <th>Document</th>
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
                  <td>{row.companyName}</td>
                  <td>
                    {row.documentUrl ? (
                      <a
                        href={row.documentUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        <img
                          src={row.documentUrl}
                          alt="doc"
                          style={{ width: "60px", borderRadius: "4px" }}
                        />
                      </a>
                    ) : (
                      "—"
                    )}
                  </td>
                  <td className="action-buttons">
                    <button
                      className="action-btn edit"
                      onClick={() => handleEdit(row)}
                    >
                      <i className="bi bi-pencil"></i>
                    </button>
                    <button
                      className="action-btn delete"
                      onClick={() => handleDelete(row)}
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

      {/*modal xác nhận xóa xe */}
      <Modal show={showDeleteModal} onHide={() => setShowDeleteModal(false)}>
        <Modal.Header closeButton>
          <Modal.Title>Confirm Delete</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          Are you sure you want to delete vehicle{" "}
          <strong>{deleteTarget?.plateNumber}</strong>?
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowDeleteModal(false)}>
            Cancel
          </Button>
          <Button variant="danger" onClick={confirmDelete}>
            Delete
          </Button>
        </Modal.Footer>
      </Modal>

      {/* thông báo popup */}
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
  const [preview, setPreview] = useState(null);

  //giá trị khởi tạo cho form
  const initialValues = {
    plate: data?.plateNumber ?? "",
    brand: data?.brand ?? "",
    model: data?.model ?? "",
    company: data?.companyId ?? "",
    image: null,
    acceptRules: false,
  };

  //lấy danh sách công ty được duyệt khi mở modal
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

  useEffect(() => {
    return () => {
      if (preview) {
        URL.revokeObjectURL(preview);
      }
    };
  }, [preview]);

  useEffect(() => {
    if (!show) setPreview(null);
  }, [show]);

  return (
    <Modal show={show} onHide={onHide}>
      <Modal.Header closeButton>
        <Modal.Title>
          {data ? "Edit Vehicle" : "Register New Vehicle"}
        </Modal.Title>
      </Modal.Header>

      {/*formik quản lý form và validation */}
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

              {/*trường nhập hãng xe */}
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

              {/* trường nhập model xe */}
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

              {/* trường chọn công ty */}
              <Form.Group className="mb-3" controlId="formCompany">
                <Form.Label>Company</Form.Label>
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

              {/* trường upload ảnh/tài liệu xe */}
              <Form.Group className="mb-3" controlId="formDocument">
                <Form.Label>Vehicle Document / Image</Form.Label>
                <Form.Control
                  type="file"
                  name="image"
                  accept="image/*,application/pdf"
                  onChange={(e) => {
                    const file = e.currentTarget.files[0];
                    if (file) {
                      // Lưu file vào formik
                      handleChange({
                        target: { name: "image", value: file },
                      });

                      // Tạo URL preview ảnh
                      setPreview(URL.createObjectURL(file));
                    } else {
                      setPreview(null);
                    }
                  }}
                />
                {preview && (
                  <div className="mt-2">
                    {values.image?.type === "application/pdf" ? (
                      <a
                        href={preview}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        View PDF
                      </a>
                    ) : (
                      <img
                        src={preview}
                        alt="Preview"
                        style={{
                          width: "100px",
                          height: "auto",
                          borderRadius: "4px",
                          border: "1px solid #ccc",
                        }}
                      />
                    )}
                  </div>
                )}

                {!preview && data?.documentUrl && (
                  <div className="mt-2">
                    <a
                      href={data.documentUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      <img
                        src={data.documentUrl}
                        alt="Vehicle Document"
                        style={{
                          width: "100px",
                          height: "auto",
                          borderRadius: "4px",
                          border: "1px solid #ccc",
                        }}
                      />
                    </a>
                  </div>
                )}
              </Form.Group>
            </Modal.Body>

            {/* checkbox chấp nhận quy tắc chia tiền */}
            <Form.Group
              className="mb-3"
              controlId="formTerms"
              style={{ marginLeft: "17px" }}
            >
              <Form.Check
                type="checkbox"
                name="acceptRules"
                checked={values.acceptRules || false}
                onChange={handleChange}
                isInvalid={touched.acceptRules && !!errors.acceptRules}
                label={
                  <>
                    I accept the{" "}
                    <OverlayTrigger
                      placement="top"
                      overlay={
                        <Tooltip id="tooltip-rules">
                          When you agree with the company's credit-sharing
                          policy, you’ll receive{" "}
                          <strong>≈ $0.019 (₫500)</strong> for each kWh you
                          contribute.
                        </Tooltip>
                      }
                    >
                      <span
                        className="text-primary"
                        style={{
                          textDecoration: "underline",
                          cursor: "pointer",
                        }}
                      >
                        credit-sharing rules
                      </span>
                    </OverlayTrigger>
                  </>
                }
              />

              {/*hiển thị lỗi*/}
              {touched.acceptRules && errors.acceptRules && (
                <div
                  className="text-danger mt-1"
                  style={{ fontSize: "0.875rem", marginLeft: "1.8rem" }}
                >
                  {errors.acceptRules}
                </div>
              )}
            </Form.Group>

            {/* nút hành động */}
            <Modal.Footer>
              <Button
                variant="secondary"
                onClick={() => {
                  setPreview(null);
                  onHide();
                }}
              >
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
