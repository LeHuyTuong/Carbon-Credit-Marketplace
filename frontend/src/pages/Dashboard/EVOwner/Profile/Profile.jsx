import React, { useEffect, useState, useRef } from "react";
import { Modal, Button, Form, Card } from "react-bootstrap";
import { Formik } from "formik";
import * as Yup from "yup";
import { apiFetch } from "../../../../utils/apiFetch";
import { useAuth } from "../../../../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import useReveal from "../../../../hooks/useReveal";

export default function Profile() {
  const { user, token } = useAuth();
  const [kycData, setKycData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showModal, setShowModal] = useState(false);
  const nav = useNavigate();
  const sectionRef = useRef(null);
  useReveal(sectionRef);

  //fetch KYC
  useEffect(() => {
    if (!user) return; // nếu chưa đăng nhập thì không fetch

    const fetchKYC = async () => {
      setLoading(true);
      try {
        const data = await apiFetch("/api/v1/kyc/user", { method: "GET" });
        const info = data.response;

        // nếu chưa có KYC -> set null để hiện prompt “Start KYC”
        if (!info) {
          setKycData(null);
          return;
        }

        // map lại dữ liệu BE trả về sang dạng FE dễ đọc
        const mapped = {
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
        setKycData(mapped);
      } catch (err) {
        console.error("Error fetching KYC:", err);
        // nếu BE trả 404 hoặc không có dữ liệu thì xem như user chưa KYC
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

  //loading and error states
  if (!user) return;

  if (loading)
    return (
      <div
        ref={sectionRef}
        className="d-flex justify-content-center align-items-center vh-100 reveal"
      >
        <div className="spinner-border text-primary" />
      </div>
    );

  if (error)
    return (
      <div className="text-center mt-5 text-danger">
        <p>{error}</p>
      </div>
    );

  if (!kycData)
    return (
      <div className="d-flex flex-column justify-content-center align-items-center text-center vh-100">
        <h4>Please complete your KYC to view your profile</h4>
        <Button onClick={() => nav("/kyc")}>Start KYC</Button>
      </div>
    );

  //callback khi cập nhật KYC thành công
  const handleSuccess = (updated) => {
    setKycData(updated);
    setShowModal(false);
    toast.success("Profile updated successfully!");
  };

  return (
    <div className="auth-hero d-flex justify-content-center align-items-center min-vh-100 bg-light">
      <Card
        className="shadow-lg border-0 rounded-4 p-4"
        style={{ maxWidth: "700px", width: "100%" }}
      >
        <h3 className="fw-bold mb-4 d-flex align-items-center text-dark">
          <i className="bi bi-person-circle me-2"></i> Profile Information
        </h3>

        <div className="row g-3 mt-2">
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

        <UpdateEvModal
          show={showModal}
          onHide={() => setShowModal(false)}
          data={kycData}
          onSuccess={handleSuccess}
        />
      </Card>
    </div>
  );
}

// modal update
function UpdateEvModal({ show, onHide, data, onSuccess }) {
  const today = new Date();
  const minAdultDate = new Date(
    today.getFullYear() - 18,
    today.getMonth(),
    today.getDate()
  );

  const schema = Yup.object().shape({
    fullName: Yup.string().required("Full name is required"),
    phone: Yup.string().required("Phone is required"),
    address: Yup.string().required("Address is required"),
    country: Yup.string().required("Country is required"),
    birthday: Yup.date()
      .required("Birth date is required")
      .max(minAdultDate, "You must be at least 18 years old"),
  });

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
            birthDate: values.birthday?.split("T")[0],
          },
        }),
      });
      onSuccess(values); // callback sang parent khi update thành công
    } catch (err) {
      console.error("Error updating KYC:", err);
      toast.error(err.message || "Failed to update profile");
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
              {[
                { name: "fullName", label: "Full Name" },
                { name: "documentNumber", label: "Document Number" },
                { name: "birthday", label: "Birthday", type: "date" },
                { name: "gender", label: "Gender", isSelect: true },
                { name: "country", label: "Country" },
                { name: "address", label: "Address" },
                { name: "phone", label: "Phone" },
                {
                  name: "documentType",
                  label: "Document Type",
                  isSelect: true,
                },
              ].map((field) => (
                <Form.Group className="mb-3" key={field.name}>
                  <Form.Label>{field.label}</Form.Label>

                  {field.isSelect ? (
                    <Form.Select
                      name={field.name}
                      value={values[field.name]}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      isInvalid={touched[field.name] && !!errors[field.name]}
                    >
                      {field.name === "gender" ? (
                        <>
                          <option value="Male">Male</option>
                          <option value="Female">Female</option>
                        </>
                      ) : (
                        <>
                          <option value="CCCD">Citizen ID Card</option>
                          <option value="CMND">Identity Card</option>
                        </>
                      )}
                    </Form.Select>
                  ) : (
                    <Form.Control
                      type={field.type || "text"}
                      name={field.name}
                      value={values[field.name]}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      isInvalid={touched[field.name] && !!errors[field.name]}
                    />
                  )}

                  <Form.Control.Feedback type="invalid">
                    {errors[field.name]}
                  </Form.Control.Feedback>
                </Form.Group>
              ))}
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
