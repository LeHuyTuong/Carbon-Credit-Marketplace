import React, { useRef, useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import useRipple from "../../hooks/useRipple";
import { useAuth } from "../../context/AuthContext.jsx";
import { toast } from "react-toastify";
import { Formik } from "formik";
import * as Yup from "yup";

// validation schema
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
});

export default function Login() {
  const nav = useNavigate();
  const { login } = useAuth();
  const ripple = useRipple();
  const btnRippleRef = useRef(null);
  const [loading, setLoading] = useState(false);
  const [remember, setRemember] = useState(false);

  // gửi request login
  const handleLogin = async (values) => {
    setLoading(true);
    try {
      const API = import.meta.env.VITE_API_BASE;
      const res = await fetch(`${API}/api/v1/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(values),
      });

      // parse json response
      const data = await res.json().catch(() => ({}));

      // lỗi từ server
      if (!res.ok) {
        const message =
          data?.responseStatus?.responseMessage ||
          data?.message ||
          (res.status === 400 ? "Invalid email or password" : "Login failed");
        throw new Error(message);
      }

      const token = data?.responseData?.jwt;
      const roles = data?.responseData?.roles;
      if (!token) throw new Error("Missing token from server");

      const user = { email: values.email, role: roles?.[0] || "USER" };
      login(user, token, remember);

      nav("/home", { replace: true });
      toast.success("Login successful!");
    } catch (err) {
      console.error("Login error:", err.message);
      toast.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-hero min-vh-100 d-flex align-items-center justify-content-center">
      <div className="container" style={{ maxWidth: 440 }}>
        <div className="card shadow-sm">
          <div className="card-body p-4 p-md-5">
            <h1 className="h4 mb-4 text-center">Login</h1>

            <Formik
              validationSchema={schema}
              initialValues={{ email: "", password: "" }}
              onSubmit={handleLogin}
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
                      type="email"
                      name="email"
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

                  {/*password */}
                  <div className="mb-3">
                    <label htmlFor="password" className="form-label">
                      Password
                    </label>
                    <input
                      id="password"
                      type="password"
                      name="password"
                      className={`form-control ${
                        touched.password && errors.password ? "is-invalid" : ""
                      }`}
                      value={values.password}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      placeholder="••••••••"
                      autoComplete="current-password"
                      required
                    />
                    {touched.password && errors.password && (
                      <div className="invalid-feedback">{errors.password}</div>
                    )}
                  </div>

                  {/*remember me & forgot */}
                  <div className="d-flex align-items-center mb-3">
                    <div className="form-check">
                      <input
                        className="form-check-input"
                        type="checkbox"
                        id="remember"
                        checked={remember}
                        onChange={(e) => setRemember(e.target.checked)}
                      />
                      <label className="form-check-label" htmlFor="remember">
                        Remember me
                      </label>
                    </div>
                    <Link to="/forgot" className="ms-auto">
                      Forgot password?
                    </Link>
                  </div>

                  {/*submit button */}
                  <button
                    type="submit"
                    disabled={loading}
                    className="btn btn-primary w-100 position-relative overflow-hidden"
                    onClick={(e) => ripple(e, btnRippleRef.current)}
                  >
                    <span ref={btnRippleRef} className="ripple-host" />
                    {loading ? "Loading…" : "Login"}
                  </button>
                </form>
              )}
            </Formik>

            <div className="text-center text-muted my-3">— or —</div>
            <button type="button" className="btn btn-outline-secondary w-100">
              Login with Google
            </button>

            <p className="mt-3 text-center mb-0">
              Don’t have an account? <Link to="/register">Sign up</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
