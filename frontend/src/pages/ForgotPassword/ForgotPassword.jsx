import { useState, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import useRipple from "../../hooks/useRipple";
import { useForm } from "../../hooks/useForm";

export default function ForgotPassword() {
  const nav = useNavigate();
  const ripple = useRipple();
  const btnRippleRef = useRef(null);
  const [loading, setLoading] = useState(false);

  const validators = {
    email: (val) => {
      if (!val.trim()) return "Email is required";
      if (!/^\S+@\S+\.\S+$/.test(val)) return "Enter a valid email";
      return "";
    },
  };

  const {
    values,
    setValue,
    errors,
    show,
    validateForm,
    markTouched,
    setSubmitted,
  } = useForm({ email: "" }, validators);

  // const submit = async (ev) => {
  //   ev.preventDefault();
  //   setSubmitted(true);
  //   if (!validateForm()) return;

  //   setLoading(true);
  //   await new Promise((r) => setTimeout(r, 1000)); //giả lập API
  //   setLoading(false);
  //   nav("/otp", { replace: true, state: { email: values.email, from: 'forgot' } });
  // };

  //call api
  const submit = async (ev) => {
    ev.preventDefault(); //chặn reload trang
    setSubmitted(true);
    if (!validateForm()) return;

    setLoading(true);
    try {
      const API = import.meta.env.VITE_API_BASE;
      const res = await fetch(`${API}/api/v1/auth/reset-password-request`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
      });

      const data = await res.json(); //request thành công, đọc json từ be
      
      if (!res.ok) {
        const message = data?.responseStatus?.responseMessage ||
                        data?.message || 
                        "Failed to send reset OTP"
        throw new Error(message);
      }

      alert("OTP has been sent to your email. Please check your inbox.");

      //sang otp
      nav('/otp', { replace: true, state: {email: values.email, from: "forgot"} });
    } catch (err) {
      console.error("Forgot password error:", err.message);
      alert(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-hero min-vh-100 d-flex align-items-center justify-content-center">
      <div className="container auth-container" style={{ maxWidth: 500 }}>
        <div className="card shadow-sm">
          <div className="card-body p-4 p-md-5">
            <h1 className="h4 mb-4 text-center">Forgot password?</h1>
            <p className="mb-4 text-center">
              Please enter the email associated with your account to receive an
              OTP and reset your password.
            </p>

            <form onSubmit={submit} noValidate>
              {/*email */}
              <div className="mb-3">
                <label htmlFor="email" className="form-label">
                  Email
                </label>
                <input
                  id="email"
                  type="email"
                  className={`form-control form-control-sm ${
                    show("email") ? "is-invalid" : ""
                  }`}
                  value={values.email}
                  onChange={(e) => setValue("email", e.target.value)}
                  onBlur={() => markTouched("email")}
                  placeholder="you@example.com"
                  autoComplete="email"
                  required
                />
                {show("email") && (
                  <div className="invalid-feedback">{errors.email}</div>
                )}
              </div>

              <button
                type="submit"
                disabled={loading}
                className="btn btn-primary w-100 position-relative overflow-hidden"
                onClick={(e) => ripple(e, btnRippleRef.current)}
              >
                <span ref={btnRippleRef} className="ripple-host" />
                {loading ? (
                  <span className="spinner-border spinner-border-sm"></span>
                ) : (
                  "Continue"
                )}
              </button>
            </form>

            <p className="mt-3 text-center mb-0">
              <Link to="/login">Back to login page</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
