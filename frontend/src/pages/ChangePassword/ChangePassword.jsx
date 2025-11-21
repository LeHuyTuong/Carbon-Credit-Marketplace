import React, { useRef, useState, useEffect } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { Formik } from "formik";
import * as Yup from "yup";
import { toast } from "react-toastify";
import useRipple from "../../hooks/useRipple";
import { apiFetch } from "../../utils/apiFetch";
import { useAuth } from "../../context/AuthContext";

//validate
const schema = Yup.object().shape({
  password: Yup.string()
    .required("Password is required")
    .min(8, "At least 8 characters")
    .matches(/[a-z]/, "Must contain a lowercase letter")
    .matches(/[A-Z]/, "Must contain an uppercase letter")
    .matches(/\d/, "Must contain a number")
    .matches(/[^a-zA-Z0-9]/, "Must contain a special character"),
  confirm: Yup.string()
    .oneOf([Yup.ref("password"), null], "Passwords do not match")
    .required("Please re-enter your password"),
});

export default function ChangePassword() {
  const nav = useNavigate();
  const { state } = useLocation(); // nhận { email, otp } từ màn OTP
  const email = state?.email?.trim();
  const otp = state?.otp;
  const token = state?.token; //nhận jwt từ OTP
  const ripple = useRipple();
  const btnRippleRef = useRef(null);
  const [loading, setLoading] = useState(false);

  //nếu thiếu context => quay lại flow quên mật khẩu
  useEffect(() => {
    if (!email || !otp || !token) {
      toast.warn("Missing reset context. Please start again.");
      nav("/forgot-password", { replace: true });
    }
  }, [email, otp, nav]);

  //xử lý submit
  const handleSubmit = async (values) => {
    setLoading(true);
    try {
      await apiFetch("/api/v1/reset-password", {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
        body: JSON.stringify({
          password: values.password,
          confirmPassword: values.confirm,
        }),
      });

      toast.success("Password has been reset. Please log in.");
      nav("/login", { replace: true, state: { email } });
    } catch (err) {
      console.error("Reset error:", err);
      toast.error(err.message || "Reset password failed");
    } finally {
      setLoading(false);
    }
  };

  //UI
  return (
    <div className="auth-hero min-vh-100 d-flex align-items-center justify-content-center">
      <div className="container auth-container" style={{ maxWidth: 500 }}>
        <div className="card shadow-sm">
          <div className="card-body p-4 p-md-5">
            <h1 className="h4 mb-4 text-center">Reset Password</h1>

            <Formik
              initialValues={{ password: "", confirm: "" }}
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
                  {/*password */}
                  <div className="mb-3">
                    <label htmlFor="password" className="form-label">
                      New Password
                    </label>
                    <input
                      id="password"
                      name="password"
                      type="password"
                      className={`form-control form-control-sm ${
                        touched.password && errors.password ? "is-invalid" : ""
                      }`}
                      value={values.password}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      placeholder="••••••••"
                      autoComplete="new-password"
                      required
                    />
                    {touched.password && errors.password && (
                      <div className="invalid-feedback">{errors.password}</div>
                    )}
                  </div>

                  {/*confirm password */}
                  <div className="mb-3">
                    <label htmlFor="confirm" className="form-label">
                      Re-enter Password
                    </label>
                    <input
                      id="confirm"
                      name="confirm"
                      type="password"
                      className={`form-control form-control-sm ${
                        touched.confirm && errors.confirm ? "is-invalid" : ""
                      }`}
                      value={values.confirm}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      placeholder="••••••••"
                      autoComplete="new-password"
                      required
                    />
                    {touched.confirm && errors.confirm && (
                      <div className="invalid-feedback">{errors.confirm}</div>
                    )}
                  </div>

                  {/*action buttons */}
                  <div className="modal-footer gap-2">
                    <button
                      type="button"
                      className="btn btn-secondary"
                      onClick={() => nav("/login", { replace: true })}
                    >
                      Cancel
                    </button>

                    <button
                      type="submit"
                      disabled={loading}
                      className="btn btn-primary position-relative overflow-hidden"
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
                      {loading ? "Resetting..." : "Save changes"}
                    </button>
                  </div>
                </form>
              )}
            </Formik>
          </div>
        </div>
      </div>
    </div>
  );
}
