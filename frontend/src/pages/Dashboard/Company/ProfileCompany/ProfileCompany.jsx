import React, { useState, useRef } from "react";
import { Modal, Button, Form, Card } from "react-bootstrap";
import { Formik } from "formik";
import * as Yup from "yup";
import { apiFetch } from "../../../../utils/apiFetch";
import { useNavigate } from "react-router-dom";
import { useCompanyProfile } from "../../../../hooks/useCompanyProfile";
import { toast } from "react-toastify";
import useReveal from "../../../../hooks/useReveal";

export default function CompanyProfile() {
  const { company, loading, error } = useCompanyProfile();
  const [showModal, setShowModal] = useState(false);
  const [updatedCompany, setUpdatedCompany] = useState(company);
  const nav = useNavigate();
  const sectionRef = useRef(null);
  useReveal(sectionRef);

  //loading state
  if (loading)
    return (
      <div
        ref={sectionRef}
        className="reveal d-flex justify-content-center align-items-center vh-100"
      >
        <div className="spinner-border text-primary" />
      </div>
    );

  //error state
  if (error)
    return (
      <div className="text-center mt-5 text-danger">
        <p>{error}</p>
      </div>
    );

  //khi chưa có KYC
  if (!company)
    return (
      <div className="d-flex flex-column justify-content-center align-items-center text-center vh-100">
        <h4>Please complete your Company KYC to view your profile</h4>
        <Button onClick={() => nav("/kyc-company")}>Start Company KYC</Button>
      </div>
    );

  //khi update thành công
  const handleSuccess = (updated) => {
    setUpdatedCompany(updated);
    setShowModal(false);
    toast.success("Company profile updated successfully!");
  };

  //giao diện chính
  return (
    <div className="auth-hero d-flex justify-content-center align-items-center min-vh-100 bg-light">
      <Card
        className="shadow-lg border-0 rounded-4 p-4"
        style={{ maxWidth: "700px", width: "100%" }}
      >
        <h3 className="fw-bold mb-4 d-flex align-items-center text-dark">
          <i className="bi bi-building me-2"></i> Company Information
        </h3>

        <div className="row g-3 mt-2">
          {Object.entries(updatedCompany || company).map(([key, value]) => (
            <div key={key}>
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

        <UpdateCompanyModal
          show={showModal}
          onHide={() => setShowModal(false)}
          data={updatedCompany || company}
          onSuccess={handleSuccess}
        />
      </Card>
    </div>
  );
}

//modal update
function UpdateCompanyModal({ show, onHide, data, onSuccess }) {
  const schema = Yup.object().shape({
    businessLicense: Yup.string().required("Business license is required"),
    taxCode: Yup.string().required("Tax code is required"),
    companyName: Yup.string().required("Company name is required"),
    address: Yup.string().required("Address is required"),
  });

  const handleUpdate = async (values) => {
    try {
      await apiFetch("/api/v1/kyc/company", {
        method: "PUT",
        body: {
          requestTrace: crypto.randomUUID(),
          requestDateTime: new Date().toISOString(),
          data: {
            businessLicense: values.businessLicense,
            taxCode: values.taxCode,
            companyName: values.companyName,
            address: values.address,
          },
        },
      });

      onSuccess(values);
    } catch (err) {
      console.error("Error updating company:", err);
      toast.error(err.message || "Failed to update company info");
    }
  };

  return (
    <Modal show={show} onHide={onHide} centered>
      <Modal.Header closeButton>
        <Modal.Title>Update Company Info</Modal.Title>
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
              {["businessLicense", "taxCode", "companyName", "address"].map(
                (field) => (
                  <Form.Group className="mb-3" key={field}>
                    <Form.Label className="text-capitalize">{field}</Form.Label>
                    <Form.Control
                      name={field}
                      value={values[field]}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      isInvalid={touched[field] && !!errors[field]}
                      placeholder={`Enter ${field}`}
                    />
                    <Form.Control.Feedback type="invalid">
                      {errors[field]}
                    </Form.Control.Feedback>
                  </Form.Group>
                )
              )}
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
