// src/pages/KYCCompany/KYCCompany.jsx
import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Formik } from "formik";
import * as Yup from "yup";
import { Form, Button } from "react-bootstrap";
import { toast } from "react-toastify";
import { apiFetch } from "../../../../utils/apiFetch";

// validation schema
const KYC_COMPANY_SCHEMA = Yup.object().shape({
  businessLicense: Yup.string().required("Business license is required"),
  taxCode: Yup.string().required("Tax code is required"),
  companyName: Yup.string().required("Company name is required"),
  address: Yup.string().required("Address is required"),
});

export default function KYCCompany() {
  const nav = useNavigate();
  const [initialValues, setInitialValues] = useState({
    businessLicense: "",
    taxCode: "",
    companyName: "",
    address: "",
  });
  const [loading, setLoading] = useState(false);

  // fetch KYC company info
  useEffect(() => {
    const fetchCompanyKYC = async () => {
      setLoading(true);
      try {
        const data = await apiFetch("/api/v1/kyc/company", { method: "GET" });
        const info = data?.response;

        if (!info) {
          toast.info("No company KYC record found. Please create one.");
          return;
        }

        setInitialValues({
          businessLicense: info.businessLicense || "",
          taxCode: info.taxCode || "",
          companyName: info.companyName || "",
          address: info.address || "",
        });
      } catch (err) {
        console.error("Error fetching company KYC:", err);
        // 400 hoặc 404 không hiển thị lỗi
        if (err.status === 400 || err.status === 404) {
          // không làm gì cả, chỉ giữ form trống
        } else {
          toast.error("Failed to load company KYC. Please try again later.");
        }
      } finally {
        setLoading(false);
      }
    };

    fetchCompanyKYC();
  }, []);

  // submit create/update
  const handleSubmit = async (values) => {
    setLoading(true);
    const hasExisting = !!initialValues.companyName;
    const method = hasExisting ? "PUT" : "POST";

    try {
      await apiFetch("/api/v1/kyc/company", {
        method,
        body: {
          data: {
            businessLicense: values.businessLicense,
            taxCode: values.taxCode,
            companyName: values.companyName,
            address: values.address,
          },
        },
      });

      toast.success(
        hasExisting
          ? "Company KYC updated successfully!"
          : "Company KYC created successfully!"
      );
      nav("/profile-company", { replace: true });
    } catch (err) {
      console.error("Error submitting company KYC:", err);
      toast.error(err.message || "Failed to save company KYC");
    } finally {
      setLoading(false);
    }
  };

  // loading state
  if (loading)
    return (
      <div className="d-flex justify-content-center align-items-center vh-100">
        <div className="spinner-border text-primary" />
      </div>
    );

  return (
    <div className="auth-hero min-vh-100 d-flex align-items-center justify-content-center">
      <div className="card shadow" style={{ maxWidth: "500px", width: "100%" }}>
        <div className="card-body">
          <h2 className="mb-4 text-center">Company KYC</h2>

          <Formik
            enableReinitialize
            validationSchema={KYC_COMPANY_SCHEMA}
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
            }) => (
              <Form noValidate onSubmit={handleSubmit}>
                <Form.Group className="mb-3">
                  <Form.Label>Business License</Form.Label>
                  <Form.Control
                    name="businessLicense"
                    value={values.businessLicense}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    isInvalid={
                      touched.businessLicense && !!errors.businessLicense
                    }
                    placeholder="Enter business license number"
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

                <div className="text-end">
                  <Button type="submit" className="btn btn-primary">
                    Save KYC
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
