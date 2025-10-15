import React, { useEffect, useState } from "react";
import { useAuth } from "../../context/AuthContext";
import { Modal, Button, Form } from "react-bootstrap";
import { Formik } from "formik";
import * as Yup from "yup";
import { apiFetch } from "../../utils/apiFetch";
import { useNavigate } from "react-router-dom";

export default function Profile() {
  const { user, token } = useAuth();
  const [kycData, setKycData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showModal, setShowModal] = useState(false);
  const nav = useNavigate();

  //lấy dữ liệu KYC từ backend
  useEffect(() => {
    if (!user) return;

    const fetchKYC = async () => {
      setLoading(true);
      try {
        const data = await apiFetch("/api/v1/kyc/user", {
          method: "GET",
          headers: {},
        });

        const info = data.response;
        //nếu chưa có KYC thì chuyển sang màn KYC
        if (!info) {
          console.warn("User has no KYC yet");
          setKycData(null);
          return;
        }

        //map dữ liệu từ backend sang form
        const mappedData = {
          fullName: info.name,
          documentNumber: info.documentNumber,
          birthday: info.birthDate,
          gender: info.gender === "MALE" ? "Male" : "Female",
          country: info.country,
          address: info.address,
          phone: info.phone,
          documentType: info.documentType,
          email: info.email,
        };

        setKycData(mappedData);
        // } catch (err) {
        //   //nếu 404 thì chưa có KYC
        //   if (err.message.includes("404") ) setKycData(null);
        //   else setError("Error fetching KYC");
      } catch (err) {
        console.error("Error fetching KYC:", err);

        // xử lý trường hợp chưa có KYC
        if (
          err.status === 404 ||
          err.status === 500 ||
          err.message?.toLowerCase()?.includes("kyc not found")
        ) {
          setKycData(null);
        } else {
          setError(err.message || "Error fetching KYC");
        }
      } finally {
        setLoading(false);
      }
    };

    fetchKYC();
  }, [token, user]);

  //nếu chưa đăng nhập
  if (!user) {
    return (
      <div className="d-flex justify-content-center align-items-center vh-100 text-center">
        <div>
          <h4>You are not logged in</h4>
          <p>Please log in to access your profile.</p>
        </div>
      </div>
    );
  }

  if (loading)
    return (
      <div className="d-flex justify-content-center align-items-center vh-100">
        <div className="spinner-border text-primary" />
      </div>
    );

  if (error)
    return (
      <div className="text-center mt-5 text-danger">
        <p>{error}</p>
      </div>
    );

  if (!kycData) {
    return (
      <div className="d-flex flex-column justify-content-center align-items-center text-center vh-100">
        <h4>Please complete your KYC to view your profile</h4>
        <Button onClick={() => nav("/kyc")}>Start KYC</Button>
      </div>
    );
  }

  // profile info
  return (
    <div className="auth-hero d-flex justify-content-center align-items-center min-vh-100 bg-light">
      <div
        className="card shadow-lg border-0 rounded-4 p-4"
        style={{ maxWidth: "800px", margin: "0 auto" }}
      >
        <h3 className="text-center mb-4 fw-bold">Profile Information</h3>

        <div className="row g-3">
          {Object.entries(kycData).map(([key, value]) => (
            <div key={key} className="col-md-6">
              <label className="fw-semibold text-muted text-capitalize">
                {key}
              </label>
              <input className="form-control" value={value || ""} disabled />
            </div>
          ))}
        </div>

        <div className="text-end mt-4">
          <Button variant="primary" onClick={() => setShowModal(true)}>
            Update Profile
          </Button>
        </div>

        <UpdateModal
          show={showModal}
          onHide={() => setShowModal(false)}
          data={kycData}
          token={token}
          onSuccess={(updated) => {
            setKycData(updated);
            setShowModal(false);
          }}
        />
      </div>
    </div>
  );
}

//update profile modal
function UpdateModal({ show, onHide, data, token, onSuccess }) {
  //validate form
  const schema = Yup.object().shape({
    fullName: Yup.string().required("Full name is required"),
    phone: Yup.string().required("Phone is required"),
    address: Yup.string().required("Address is required"),
  });

  //gửi cập nhật lên backend
  const handleUpdate = async (values) => {
    try {
      await apiFetch("/api/v1/kyc/user", {
        method: "PUT",
        body: JSON.stringify({
          requestTrace: crypto.randomUUID(),
          requestDateTime: new Date().toISOString(),
          data: {
            name: values.fullName,
            phone: values.phone,
            gender: values.gender === "Male" ? "MALE" : "FEMALE",
            country: values.country,
            address: values.address,
            documentType: values.documentType,
            documentNumber: values.documentNumber,
            birthDate: values.birthday?.split("T")[0], //chỉ lấy phần date
          },
        }),
      });

      onSuccess(values);
    } catch (err) {
      console.error("Error updating KYC:", err);
      alert(err.message || "Failed to update profile");
    }
  };

  return (
    <Modal show={show} onHide={onHide} centered>
      <Modal.Header closeButton>
        <Modal.Title>Update Profile</Modal.Title>
      </Modal.Header>

      <Formik
        enableReinitialize
        validationSchema={schema}
        initialValues={data}
        onSubmit={handleUpdate}
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
              <Form.Group className="mb-3">
                <Form.Label>Full Name</Form.Label>
                <Form.Control
                  name="fullName"
                  value={values.fullName}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.fullName && !!errors.fullName}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.fullName}
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Document Number</Form.Label>
                <Form.Control
                  name="documentNumber"
                  value={values.documentNumber}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.documentNumber && !!errors.documentNumber}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.documentNumber}
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Birthday</Form.Label>
                <Form.Control
                  type="date"
                  name="birthday"
                  value={values.birthday}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.birthday && !!errors.birthday}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.birthday}
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Gender</Form.Label>
                <Form.Select
                  name="gender"
                  value={values.gender}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.gender && !!errors.gender}
                >
                  <option value="Male">Male</option>
                  <option value="Female">Female</option>
                </Form.Select>
                <Form.Control.Feedback type="invalid">
                  {errors.gender}
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Country</Form.Label>
                <Form.Control
                  name="country"
                  value={values.country}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.country && !!errors.country}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.country}
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Address</Form.Label>
                <Form.Control
                  name="address"
                  value={values.address}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.address && !!errors.address}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.address}
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Phone</Form.Label>
                <Form.Control
                  name="phone"
                  value={values.phone}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.phone && !!errors.phone}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.phone}
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Document Type</Form.Label>
                <Form.Select
                  name="documentType"
                  value={values.documentType}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.documentType && !!errors.documentType}
                >
                  <option value="CCCD">Citizen Identification Card</option>
                  <option value="CMND">Identity Card</option>
                </Form.Select>
                <Form.Control.Feedback type="invalid">
                  {errors.documentType}
                </Form.Control.Feedback>
              </Form.Group>
            </Modal.Body>

            <Modal.Footer>
              <Button variant="secondary" onClick={onHide}>
                Close
              </Button>
              <Button type="submit" variant="primary">
                Save Changes
              </Button>
            </Modal.Footer>
          </Form>
        )}
      </Formik>
    </Modal>
  );
}
