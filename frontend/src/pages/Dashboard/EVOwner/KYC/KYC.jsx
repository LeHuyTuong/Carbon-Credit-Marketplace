import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Formik } from "formik";
import * as Yup from "yup";
import { useAuth } from "../../../../context/AuthContext.jsx";
import { Form, Button } from "react-bootstrap";
import { toast } from "react-toastify";
import { apiFetch } from "../../../../utils/apiFetch.js";

const today = new Date();
const minAdultDate = new Date(
  today.getFullYear() - 18,
  today.getMonth(),
  today.getDate()
);
//validation schema
const KYC_SCHEMA = Yup.object().shape({
  name: Yup.string().required("Full name is required"),
  phone: Yup.string().required("Phone number is required"),
  country: Yup.string().required("Country is required"),
  address: Yup.string().required("Address is required"),
  birthDate: Yup.date()
    .required("Birth date is required")
    .max(minAdultDate, "You must be at least 18 years old"),
  documentType: Yup.string().required("Please select a document type"),
  documentNumber: Yup.string()
    .matches(
      /^(?!000)\d{3}[0-3]\d{2}(?!000000)\d{6}$/,
      "Document number must be 12 digits and follow CCCD format"
    )
    .required("Document number is required"),
  gender: Yup.string().required("Please select gender"),
});

export default function KYC() {
  const { user, token } = useAuth();
  const nav = useNavigate();
  //khởi tạo formik
  const [initialValues, setInitialValues] = useState({
    name: "",
    phone: "",
    country: "",
    address: "",
    birthDate: "",
    email: user?.email || "",
    documentType: "",
    documentNumber: "",
    gender: "",
  });
  const [loading, setLoading] = useState(false);
  const [hasExisting, setHasExisting] = useState(false);

  //fetch KYC
  useEffect(() => {
    const fetchKYC = async () => {
      //kiểm tra token
      if (!token) return;
      setLoading(true);
      try {
        const data = await apiFetch("/api/v1/kyc/user", {
          method: "GET",
        });

        //kiểm tra response và gán dữ liệu vào form
        const info = data?.response;
        if (info) {
          setHasExisting(true);
          //gán dữ liệu
          setInitialValues({
            id: info.id,
            name: info.name || "",
            phone: info.phone || "",
            country: info.country || "",
            address: info.address || "",
            birthDate: info.birthDate || "",
            email: info.email || user?.email || "",
            documentType: info.documentType || "",
            documentNumber: info.documentNumber || "",
            gender: info.gender === "MALE" ? "male" : "female",
          });
        } else {
          setHasExisting(false);
          toast.info("No KYC record found. Please create one.");
        }
      } catch (err) {
        console.error("Error fetching KYC:", err);
        //không báo lỗi nếu KYC chưa có hoặc backend không tìm thấy bản ghi
        const msg =
          err?.response?.responseStatus?.responseMessage?.toLowerCase() || "";

        if (
          err.status === 400 ||
          err.status === 404 ||
          msg.includes("kyc not found") ||
          msg.includes("no kyc") ||
          err.status === 500
        ) {
          setHasExisting(false);
        } else {
          toast.error(
            "Failed to load KYC information. Please try again later."
          );
        }
      } finally {
        setLoading(false);
      }
    };

    fetchKYC();
  }, [token, user]);

  //submit kyc
  const handleSubmit = async (values) => {
    setLoading(true);
    //chọn method phù hợp
    const method = hasExisting ? "PUT" : "POST";

    //gọi API lưu KYC
    try {
      await apiFetch("/api/v1/kyc/user", {
        method,
        body: JSON.stringify({
          requestTrace: crypto.randomUUID(),
          requestDateTime: new Date().toISOString(),
          data: {
            name: values.name,
            phone: values.phone,
            gender: values.gender === "male" ? "MALE" : "FEMALE",
            country: values.country,
            address: values.address,
            documentType: values.documentType,
            documentNumber: values.documentNumber,
            birthDate: values.birthDate.split("T")[0], //chỉ lấy phần date
          },
        }),
      });
      //thông báo thành công
      toast.success(
        hasExisting ? "KYC updated successfully!" : "KYC created successfully!"
      );

      //quay về profile
      nav("/profile", { replace: true });
    } catch (err) {
      console.error("Error submitting KYC:", err);
      toast.error(err.message || "Failed to save KYC");
    } finally {
      setLoading(false);
    }
  };

  if (loading)
    return (
      <div className="d-flex justify-content-center align-items-center vh-100">
        <div className="spinner-border text-primary" />
      </div>
    );

  return (
    <div className="auth-hero min-vh-100 d-flex align-items-center justify-content-center">
      <div className="card shadow container">
        <div className="card-body">
          <h3 className="mb-4 text-center fw-bold">
            Identity Verification (KYC)
          </h3>

          <Formik
            enableReinitialize
            validationSchema={KYC_SCHEMA}
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
                {/*full name, phone */}
                <div className="row mb-3">
                  <div className="col-md-6">
                    <Form.Label>Full Name</Form.Label>
                    <Form.Control
                      name="name"
                      value={values.name}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      isInvalid={touched.name && !!errors.name}
                      placeholder="Enter full name"
                    />
                    <Form.Control.Feedback type="invalid">
                      {errors.name}
                    </Form.Control.Feedback>
                  </div>
                  <div className="col-md-6">
                    <Form.Label>Phone</Form.Label>
                    <Form.Control
                      name="phone"
                      value={values.phone}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      isInvalid={touched.phone && !!errors.phone}
                      placeholder="Enter phone number"
                    />
                    <Form.Control.Feedback type="invalid">
                      {errors.phone}
                    </Form.Control.Feedback>
                  </div>
                </div>

                {/*country, address */}
                <div className="row mb-3">
                  <div className="col-md-6">
                    <Form.Label>Country</Form.Label>
                    <Form.Control
                      name="country"
                      value={values.country}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      isInvalid={touched.country && !!errors.country}
                      placeholder="Enter country"
                    />
                    <Form.Control.Feedback type="invalid">
                      {errors.country}
                    </Form.Control.Feedback>
                  </div>
                  <div className="col-md-6">
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
                  </div>
                </div>

                {/*birthDate, email */}
                <div className="row mb-3">
                  <div className="col-md-6">
                    <Form.Label>Birth Date</Form.Label>
                    <Form.Control
                      type="date"
                      name="birthDate"
                      value={values.birthDate}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      isInvalid={touched.birthDate && !!errors.birthDate}
                    />
                    <Form.Control.Feedback type="invalid">
                      {errors.birthDate}
                    </Form.Control.Feedback>
                  </div>
                  <div className="col-md-6">
                    <Form.Label>Email</Form.Label>
                    <Form.Control
                      type="email"
                      name="email"
                      value={values.email}
                      disabled
                    />
                  </div>
                </div>

                {/*document Type, number */}
                <div className="row mb-3">
                  <div className="col-md-6">
                    <Form.Label>Document Type</Form.Label>
                    <Form.Select
                      name="documentType"
                      value={values.documentType}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      isInvalid={touched.documentType && !!errors.documentType}
                    >
                      <option value="">Choose one type</option>
                      <option value="CCCD">Citizen Identification Card</option>
                      <option value="CMND">Identity Card</option>
                    </Form.Select>
                    <Form.Control.Feedback type="invalid">
                      {errors.documentType}
                    </Form.Control.Feedback>
                  </div>

                  <div className="col-md-6">
                    <Form.Label>Document Number</Form.Label>
                    <Form.Control
                      name="documentNumber"
                      value={values.documentNumber}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      isInvalid={
                        touched.documentNumber && !!errors.documentNumber
                      }
                      placeholder="Enter document number"
                    />
                    <Form.Control.Feedback type="invalid">
                      {errors.documentNumber}
                    </Form.Control.Feedback>
                  </div>
                </div>

                {/*gender */}
                <div className="mb-3">
                  <Form.Label>Gender</Form.Label>
                  <div className="d-flex gap-3">
                    <Form.Check
                      type="radio"
                      name="gender"
                      value="male"
                      label="Male"
                      checked={values.gender === "male"}
                      onChange={handleChange}
                      isInvalid={touched.gender && !!errors.gender}
                    />
                    <Form.Check
                      type="radio"
                      name="gender"
                      value="female"
                      label="Female"
                      checked={values.gender === "female"}
                      onChange={handleChange}
                      isInvalid={touched.gender && !!errors.gender}
                    />
                  </div>
                  <div className="text-danger small">
                    {touched.gender && errors.gender}
                  </div>
                </div>

                <Button type="submit" className="float-end">
                  Save KYC
                </Button>
              </Form>
            )}
          </Formik>
        </div>
      </div>
    </div>
  );
}
