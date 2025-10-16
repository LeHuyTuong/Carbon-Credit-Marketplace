import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Formik } from "formik";
import * as Yup from "yup";
import { Form, Button } from "react-bootstrap";
import { toast } from "react-toastify";
import { apiFetch } from "../../../../utils/apiFetch";
import { useAuth } from "../../../../context/AuthContext";
import { FaArrowLeft } from "react-icons/fa";
import { useCompanyProfile } from "../../../../hooks/useCompanyProfile";

// validation schema
const PROJECT_SCHEMA = Yup.object().shape({
  companyName: Yup.string().required("Company name title is required"),
  businessLicense: Yup.string().required("Business license is required"),
  taxCode: Yup.string().required("Tax code are required"),
  address: Yup.string().required("Address are required"),
  documents: Yup.mixed().required("Legal document is required"),
});

export default function RegisterProject() {
  const nav = useNavigate();
  const { user } = useAuth();
  const { company, loading: companyLoading } = useCompanyProfile();
  const [loading, setLoading] = useState(false);

  //initial form values
  const initialValues = {
    companyName: company.companyName || "",
    businessLicense: company.businessLicense || "",
    taxCode: company.taxCode || "",
    address: company.address || "",
    documents: "",
  };

  //handle form submission to submit new project
  const handleSubmit = async (values) => {
    setLoading(true);
    try {
      const res = await apiFetch("/api/v1/projects/submit", {
        method: "POST",
        body: {
          data: {
            companyId: user?.companyId ?? user?.id ?? 0,
            companyName: values.companyName,
            businessLicense: values.businessLicense,
            taxCode: values.taxCode,
            address: values.address,
            documents: values.documents,
          },
        },
      });

      const code =
        res?.responseStatus?.responseCode?.trim?.().toUpperCase?.() || "";

      if (code !== "SUCCESS" && code !== "00000000") {
        throw new Error(
          res?.responseStatus?.responseMessage || "Submission failed"
        );
      }

      toast.success("Project submitted successfully!");
      nav("/list-projects", { replace: true });
    } catch (err) {
      console.error("Error submitting project:", err);
      toast.error(err.message || "Failed to submit project");
    } finally {
      setLoading(false);
    }
  };

  if (loading || companyLoading)
    return (
      <div className="d-flex justify-content-center align-items-center vh-100">
        <div className="spinner-border text-primary" />
      </div>
    );

  return (
    <div className="auth-hero min-vh-100 d-flex align-items-center justify-content-center">
      <div
        className="position-absolute top-0 start-0 m-3"
        style={{ zIndex: 10 }}
      >
        {/*button back */}
        <Button
          variant="outline-info"
          size="lg"
          className="d-flex align-items-center gap-2"
          onClick={() => nav("/home")}
          style={{ marginLeft: "10px" }}
        >
          <FaArrowLeft /> Back to Home
        </Button>
      </div>
      <div className="card shadow" style={{ maxWidth: "600px", width: "100%" }}>
        <div className="card-body">
          <h2 className="mb-4 text-center">Register Project</h2>

          {/*form register project */}
          <Formik
            validationSchema={PROJECT_SCHEMA}
            initialValues={initialValues}
            onSubmit={handleSubmit}
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
                  <Button type="submit" className="btn btn-primary">
                    Submit
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
