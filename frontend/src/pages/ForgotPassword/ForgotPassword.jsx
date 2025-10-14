import React, { useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Formik } from "formik";
import * as Yup from "yup";
import { toast } from "react-toastify";
import useRipple from "../../hooks/useRipple";
import { apiFetch } from "../../utils/apiFetch";

const schema = Yup.object().shape({
  email: Yup.string()
    .trim()
    .email("Enter a valid email")
    .required("Email is required"),
});

export default function ForgotPassword() {
  const nav = useNavigate();
  const ripple = useRipple();
  const btnRippleRef = useRef(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (values) => {
    setLoading(true);
    try {
      //kiểm tra user tồn tại
      await apiFetch("/api/v1/check-exists-user", {
        method: "POST",
        body: JSON.stringify({ email: values.email.trim() }),
      });

      //gửi mã otp
      await apiFetch("/api/v1/send-otp", {
        method: "POST",
        body: JSON.stringify({ email: values.email.trim() }),
      });

      toast.success("OTP sent successfully. Please check your email.");
      nav("/otp", {
        state: { email: values.email.trim(), from: "forgot" },
        replace: true,
      });
    } catch (err) {
      console.error("Forgot password error:", err);
      toast.error(err.message || "Failed to send reset OTP");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-hero min-vh-100 d-flex align-items-center justify-content-center">
      <div className="container auth-container" style={{ maxWidth: 500 }}>
        <div className="card shadow-sm">
          <div className="card-body p-4 p-md-5">
            <h1 className="h4 mb-4 text-center">Forgot Password?</h1>
            <p className="mb-4 text-center">
              Please enter the email associated with your account to receive an
              OTP and reset your password.
            </p>

            <Formik
              initialValues={{ email: "" }}
              validationSchema={schema}
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
                <form noValidate onSubmit={handleSubmit}>
                  {/*email */}
                  <div className="mb-3">
                    <label htmlFor="email" className="form-label">
                      Email
                    </label>
                    <input
                      id="email"
                      name="email"
                      type="email"
                      className={`form-control form-control-sm ${
                        touched.email && errors.email ? "is-invalid" : ""
                      }`}
                      value={values.email}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      placeholder="you@example.com"
                      autoComplete="email"
                      required
                    />
                    {touched.email && errors.email && (
                      <div className="invalid-feedback">{errors.email}</div>
                    )}
                  </div>

                  {/*submit button */}
                  <button
                    type="submit"
                    disabled={loading}
                    className="btn btn-primary w-100 position-relative overflow-hidden"
                    onClick={(e) => ripple(e, btnRippleRef.current)}
                  >
                    <span ref={btnRippleRef} className="ripple-host" />
                    {loading && (
                      <span
                        className="spinner-border spinner-border-sm me-2"
                        role="status"
                        aria-hidden="true"
                      />
                    )}
                    {loading ? "Sending OTP…" : "Continue"}
                  </button>
                </form>
              )}
            </Formik>

            <p className="mt-3 text-center mb-0">
              <Link to="/login">Back to login page</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
