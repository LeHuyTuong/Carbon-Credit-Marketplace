import React, { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Formik } from "formik";
import * as Yup from "yup";
import { Form, Button } from "react-bootstrap";
import { toast } from "react-toastify";
import { apiFetch } from "../../../../utils/apiFetch";
import { useAuth } from "../../../../context/AuthContext";
import { FaArrowLeft } from "react-icons/fa";
import { useCompanyProfile } from "../../../../hooks/useCompanyProfile";

//Validation
const PROJECT_SCHEMA = Yup.object().shape({
  companyName: Yup.string().required("Company name is required"),
  businessLicense: Yup.string().required("Business license is required"),
  taxCode: Yup.string().required("Tax code is required"),
  address: Yup.string().required("Address is required"),
  documents: Yup.mixed().required("Legal document is required"),
});

export default function RegisterProject() {
  const nav = useNavigate();
  const { id: projectId } = useParams(); // lấy id project từ URL
  const { user } = useAuth();
  const { company, loading: companyLoading } = useCompanyProfile();
  const [loading, setLoading] = useState(false);

  //dữ liệu khởi tạo
  const initialValues = {
    companyName: company?.companyName || "",
    businessLicense: company?.businessLicense || "",
    taxCode: company?.taxCode || "",
    address: company?.address || "",
    documents: "",
  };

  //nộp đki project
  const handleSubmit = async (values) => {
    setLoading(true);
    try {
      const formData = new FormData();
      if (values.documents && values.documents.length > 0) {
        formData.append("file", values.documents[0]);
      }

      const res = await apiFetch(
        `/api/v1/project-applications?projectId=${projectId}`,
        {
          method: "POST",
          body: formData,
        }
      );

      const code =
        res?.responseStatus?.responseCode?.trim?.().toUpperCase?.() || "";

      if (code !== "SUCCESS" && code !== "00000000") {
        throw new Error(
          res?.responseStatus?.responseMessage ||
            "Application submission failed"
        );
      }

      toast.success("Application submitted successfully!");
      nav("/list-projects", { replace: true });
    } catch (err) {
      console.error("Error submitting project application:", err);

      if (err.status === 404) {
        toast.warn(
          "Please complete your company KYC before registering a project."
        );
        nav("/kyc-company", { replace: true });
        return;
      }
      if (err.status === 409) {
        toast.warn("You’ve already registered this project.");
      } else {
        toast.error(err.message || "Failed to submit application");
      }
    } finally {
      setLoading(false);
    }
  };

  if (companyLoading)
    return (
      <div className="d-flex justify-content-center align-items-center vh-100">
        <div className="spinner-border text-primary" />
      </div>
    );

  return (
    <div className="d-flex align-items-center justify-content-center">
      <div className="card shadow" style={{ maxWidth: "600px", width: "100%" }}>
        <div className="card-body">
          <h2 className="mb-4 text-center">Register Project</h2>

          <Formik
            validationSchema={PROJECT_SCHEMA}
            initialValues={initialValues}
            onSubmit={handleSubmit}
            enableReinitialize
          >
            {({
              handleSubmit,
              handleChange,
              handleBlur,
              values,
              errors,
              touched,
              setFieldValue,
            }) => (
              <Form noValidate onSubmit={handleSubmit}>
                <Form.Group className="mb-3">
                  <Form.Label>Company Name</Form.Label>
                  <Form.Control
                    name="companyName"
                    value={values.companyName}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    isInvalid={touched.companyName && !!errors.companyName}
                    placeholder="Enter company name"
                  />
                  <Form.Control.Feedback type="invalid">
                    {errors.companyName}
                  </Form.Control.Feedback>
                </Form.Group>

                <Form.Group className="mb-3">
                  <Form.Label>Business Licence</Form.Label>
                  <Form.Control
                    name="businessLicense"
                    value={values.businessLicense}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    isInvalid={
                      touched.businessLicense && !!errors.businessLicense
                    }
                    placeholder="Enter business license"
                  />
                  <Form.Control.Feedback type="invalid">
                    {errors.businessLicense}
                  </Form.Control.Feedback>
                </Form.Group>

                <Form.Group className="mb-3">
                  <Form.Label>Tax Code</Form.Label>
                  <Form.Control
                    name="taxCode"
                    value={values.taxCode}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    isInvalid={touched.taxCode && !!errors.taxCode}
                    placeholder="Enter tax code"
                  />
                  <Form.Control.Feedback type="invalid">
                    {errors.taxCode}
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
                    placeholder="Enter address"
                  />
                  <Form.Control.Feedback type="invalid">
                    {errors.address}
                  </Form.Control.Feedback>
                </Form.Group>

                <Form.Group className="mb-3">
                  <Form.Label>Legal Documents</Form.Label>
                  <Form.Control
                    type="file"
                    name="documents"
                    multiple
                    onChange={(event) => {
                      const files = Array.from(event.currentTarget.files);
                      setFieldValue("documents", files);
                    }}
                    onBlur={handleBlur}
                    isInvalid={touched.documents && !!errors.documents}
                  />
                  <Form.Control.Feedback type="invalid">
                    {errors.documents}
                  </Form.Control.Feedback>
                </Form.Group>

                <div className="text-end">
                  <Button
                    type="submit"
                    className="btn btn-primary"
                    disabled={loading}
                  >
                    {loading ? "Submitting..." : "Submit"}
                  </Button>
                </div>
              </Form>
            )}
          </Formik>
        </div>
      </div>
    </div>
  );
}
