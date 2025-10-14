import React, { useEffect, useState } from "react";
import { Modal, Button, Form } from "react-bootstrap";
import { Formik } from "formik";
import * as Yup from "yup";
import { apiFetch } from "../../../../utils/apiFetch";
import { useNavigate } from "react-router-dom";

export default function CompanyProfile() {
  const [companyData, setCompanyData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showModal, setShowModal] = useState(false);
  const nav = useNavigate();

  // fetch KYC company info
  useEffect(() => {
    const fetchCompany = async () => {
      setLoading(true);
      try {
        const data = await apiFetch("/api/v1/kyc/company", { method: "GET" });
        const info = data.response;

        if (!info) {
          setCompanyData(null);
          return;
        }

        // map fields to match form
        const mapped = {
          businessLicense: info.businessLicense,
          taxCode: info.taxCode,
          companyName: info.companyName,
          address: info.address,
        };
        setCompanyData(mapped);
      } catch (err) {
        console.error("Error fetching company:", err);
        if (err.status === 404 || err.status === 400) {
          setCompanyData(null);
        } else {
          setError(err.message || "Failed to fetch company info");
        }
      } finally {
        setLoading(false);
      }
    };

    fetchCompany();
  }, []);

  if (loading)
    return (
      <div className="d-flex justify-content-center align-items-center vh-100">
        <div className="spinner-border text-primary" />
      </div>
    );

  // show error
  if (error)
    return (
      <div className="text-center mt-5 text-danger">
        <p>{error}</p>
      </div>
    );

  // if no company data, prompt to complete KYC
  if (!companyData) {
    return (
      <div className="d-flex flex-column justify-content-center align-items-center text-center vh-100">
        <h4>Please complete your Company KYC to view your profile</h4>
        <Button onClick={() => nav("/kyc-company")}>Start Company KYC</Button>
      </div>
    );
  }

  return (
    <div className="auth-hero d-flex justify-content-center align-items-center min-vh-100 bg-light">
      <div
        className="card shadow-lg border-0 rounded-4 p-4"
        style={{ maxWidth: "600px", margin: "0 auto" }}
      >
        <h3 className="text-center mb-4 fw-bold">Company Profile</h3>

        {/* display company info */}
        <div className="row g-3">
          {Object.entries(companyData).map(([key, value]) => (
            <div key={key}>
              <label className="fw-semibold text-muted text-capitalize">
                {key}
              </label>
              <input className="form-control" value={value || ""} disabled />
            </div>
          ))}
        </div>

        {/* update button */}
        <div className="text-end mt-4">
          <Button variant="primary" onClick={() => setShowModal(true)}>
            Update Profile
          </Button>
        </div>

        {/* update modal */}
        <UpdateCompanyModal
          show={showModal}
          onHide={() => setShowModal(false)}
          data={companyData}
          onSuccess={(updated) => {
            setCompanyData(updated);
            setShowModal(false);
          }}
        />
      </div>
    </div>
  );
}

// modal update
function UpdateCompanyModal({ show, onHide, data, onSuccess }) {
  const schema = Yup.object().shape({
    businessLicense: Yup.string().required("Business license is required"),
    taxCode: Yup.string().required("Tax code is required"),
    companyName: Yup.string().required("Company name is required"),
    address: Yup.string().required("Address is required"),
  });

  // handle form submission
  const handleUpdate = async (values) => {
    try {
      await apiFetch("/api/v1/kyc/company", {
        method: "PUT",
        body: {
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
      alert(err.message || "Failed to update company info");
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
