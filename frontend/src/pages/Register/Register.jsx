import React, { useRef, useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import useRipple from "../../hooks/useRipple";
import { toast } from "react-toastify";
import { Formik } from "formik";
import * as Yup from "yup";

//validate
const schema = Yup.object().shape({
  email: Yup.string()
    .email("Enter a valid email")
    .required("Email is required"),
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
  role: Yup.string().required("Please choose your role"),
  agree: Yup.boolean().oneOf(
    [true],
    "You must agree to the Terms of Use & Privacy Policy"
  ),
});

//map role from frontend to backend
const mapRoleToBackend = (r) => {
  switch (r) {
    case "ev":
      return "EV_OWNER";
    case "bis":
      return "COMPANY";
    case "cv":
      return "CVA";
    default:
      return "";
  }
};

export default function Register() {
  const nav = useNavigate();
  const ripple = useRipple();
  const btnRippleRef = useRef(null);
  const [loading, setLoading] = useState(false);

  //xử lý đăng ký
  const handleRegister = async (values) => {
    const roleBackend = mapRoleToBackend(values.role);
    if (!roleBackend) {
      toast.warn("Please choose a valid role!");
      return;
    }

    setLoading(true);
    try {
      const API = import.meta.env.VITE_API_BASE;
      const res = await fetch(`${API}/api/v1/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: values.email.trim(),
          password: values.password,
          confirmPassword: values.confirm,
          roleName: roleBackend,
        }),
      });

      //chỉ parse json khi response có body
      const data = await res.json().catch(() => ({}));

      if (!res.ok) {
        const message =
          data?.responseStatus?.responseMessage ||
          data?.message ||
          (res.status === 409 ? "Email already registered" : "Register failed");
        throw new Error(message);
      }

      toast.success("Registration successful! Please verify your email.");
      nav("/otp", {
        replace: true,
        state: { email: values.email, from: "register" },
      });
    } catch (err) {
      console.error("Register error:", err.message);
      toast.error(err.message || "Registration failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-hero min-vh-100 d-flex align-items-center justify-content-center">
      <div className="container" style={{ maxWidth: 500 }}>
        <div className="card shadow-sm">
          <div className="card-body p-2 p-md-4">
            <h1 className="h4 mb-2 text-center">Sign up</h1>

            <Formik
              validationSchema={schema}
              initialValues={{
                email: "",
                password: "",
                confirm: "",
                role: "",
                agree: false,
              }}
              onSubmit={handleRegister}
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
                  {/* email */}
                  <div className="mb-3">
                    <label htmlFor="email" className="form-label">
                      Email
                    </label>
                    <input
                      id="email"
                      name="email"
                      type="email"
                      className={`form-control ${
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

                  {/* password */}
                  <div className="mb-3">
                    <label htmlFor="password" className="form-label">
                      Password
                    </label>
                    <input
                      id="password"
                      name="password"
                      type="password"
                      className={`form-control ${
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

                  {/* confirm password */}
                  <div className="mb-3">
                    <label htmlFor="confirm" className="form-label">
                      Re-enter Password
                    </label>
                    <input
                      id="confirm"
                      name="confirm"
                      type="password"
                      className={`form-control ${
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

                  {/* role */}
                  <div className="mb-3">
                    <label htmlFor="role" className="form-label">
                      Role
                    </label>
                    <select
                      id="role"
                      name="role"
                      className={`form-select ${
                        touched.role && errors.role ? "is-invalid" : ""
                      }`}
                      value={values.role}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      required
                    >
                      <option value="">Choose your role</option>
                      <option value="ev">
                        Electric Vehicle Owner (EV Owner)
                      </option>
                      <option value="bis">Company</option>
                      <option value="cv">
                        Carbon Verification & Audit (CVA)
                      </option>
                    </select>
                    {touched.role && errors.role && (
                      <div className="invalid-feedback">{errors.role}</div>
                    )}
                  </div>

                  {/* agree */}
                  <div className="form-check mb-3">
                    <input
                      className={`form-check-input ${
                        touched.agree && errors.agree ? "is-invalid" : ""
                      }`}
                      type="checkbox"
                      id="agree"
                      name="agree"
                      checked={values.agree}
                      onChange={handleChange}
                      onBlur={handleBlur}
                    />
                    <label className="form-check-label" htmlFor="agree">
                      I agree with <Link to="/terms">Terms of Use</Link> &{" "}
                      <Link to="/privacy">Privacy Policy</Link>
                    </label>
                    {touched.agree && errors.agree && (
                      <div className="invalid-feedback d-block">
                        {errors.agree}
                      </div>
                    )}
                  </div>

                  {/* submit */}
                  <button
                    type="submit"
                    disabled={loading}
                    className="btn btn-primary w-100 position-relative overflow-hidden"
                    onClick={(e) => ripple(e, btnRippleRef.current)}
                  >
                    <span ref={btnRippleRef} className="ripple-host" />
                    {loading ? "Registering..." : "Sign up"}
                  </button>
                </form>
              )}
            </Formik>

            <div className="text-center text-muted my-3">— or —</div>

            <button type="button" className="btn btn-outline-secondary w-100">
              Sign up with Google
            </button>

            <p className="mt-3 text-center mb-0">
              Already have an account? <Link to="/login">Login</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
