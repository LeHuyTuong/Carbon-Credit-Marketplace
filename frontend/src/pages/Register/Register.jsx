import React, { useRef, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import useRipple from '../../hooks/useRipple';
import { useForm } from '../../hooks/useForm';

export default function Register() {
  const nav = useNavigate();
  const ripple = useRipple();
  const btnRippleRef = useRef(null);
  const [loading, setLoading] = useState(false);

  //validator riêng cho Register
  const validators = {
    email: (val) => {
      if (!val.trim()) return 'Email is required';
      if (!/^\S+@\S+\.\S+$/.test(val)) return 'Email is invalid';
      return '';
    },
    password: (val) => {
      if (!val) return 'Password is required';
      if (val.length < 6) return 'At least 6 characters';
      return '';
    },
    confirm: (val, values) => {
      if (!val) return 'Please re-enter your password';
      if (val !== values.password) return 'Passwords do not match';
      return '';
    },
    role: (val) => {
      if (!val) return 'Please choose your role';
      return '';
    },
    agree: (val) => {
      if (!val) return 'You need to agree to the Terms of Use & Privacy Policy';
      return '';
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
  } = useForm(
    { email: '', password: '', confirm: '', role: '', agree: false },
    validators
  );

  const mapRoleToBackend = (r) => {
    switch (r) {
      case "ev":  return "EV_OWNER";
      case "bis": return "COMPANY";
      case "cv":  return "CVA";
      default:    return "";
    }
  };


  // const submit = async (ev) => {
  //   ev.preventDefault();
  //   setSubmitted(true);
  //   if (!validateForm()) return;

  //   setLoading(true);
  //   await new Promise((r) => setTimeout(r, 800)); //giả lập API
  //   setLoading(false);

  //   nav('/otp', { replace: true, state: { email: values.email, from: 'register' } });
  // };

  const submit = async (ev) => {
  ev.preventDefault();
  setSubmitted(true);
  if (!validateForm()) return;

  const roleBackend = mapRoleToBackend(values.role);
  if (!roleBackend) {
    alert("Invalid role");
    return;
  }

  setLoading(true);
  try {
    const API = import.meta.env.VITE_API_BASE;

    const res = await fetch('/api/v1/auth/register', {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: values.email.trim(),
        password: values.password,
        confirmPassword: values.confirm,
        fullName: values.email.split("@")[0], // hoặc thêm input fullName
        role: roleBackend,
      }),
    });

    if (!res.ok) {
      let message = "Register failed";
      try {
        const err = await res.json();
        message = err?.responseStatus?.responseMessage || err?.message || message;
      } catch {}
      if (res.status === 409) message = "Email already registered";
      throw new Error(message);
    }

    const data = await res.json();
    console.log("Register success:", data);

    // Sau khi đăng ký xong, đi đến OTP
    nav("/otp", { replace: true, state: { email: values.email, from: "register" } });
  } catch (err) {
    console.error("Register error:", err.message);
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
            <h1 className="h4 mb-4 text-center">Sign up</h1>

            <form onSubmit={submit} noValidate>
              {/*email */}
              <div className="mb-3">
                <label htmlFor="email" className="form-label">Email</label>
                <input
                  id="email"
                  type="email"
                  className={`form-control form-control-sm ${show('email') ? 'is-invalid' : ''}`}
                  value={values.email}
                  onChange={(e) => setValue('email', e.target.value)}
                  onBlur={() => markTouched('email')}
                  placeholder="you@example.com"
                  autoComplete="email"
                  required
                />
                {show('email') && <div className="invalid-feedback">{errors.email}</div>}
              </div>

              {/*password */}
              <div className="mb-3">
                <label htmlFor="password" className="form-label">Password</label>
                <input
                  id="password"
                  type="password"
                  className={`form-control form-control-sm ${show('password') ? 'is-invalid' : ''}`}
                  value={values.password}
                  onChange={(e) => setValue('password', e.target.value)}
                  onBlur={() => markTouched('password')}
                  placeholder="••••••"
                  autoComplete="new-password"
                  required
                />
                {show('password') && <div className="invalid-feedback">{errors.password}</div>}
              </div>

              {/*confirm password */}
              <div className="mb-3">
                <label htmlFor="confirm" className="form-label">Re-enter Password</label>
                <input
                  id="confirm"
                  type="password"
                  className={`form-control form-control-sm ${show('confirm') ? 'is-invalid' : ''}`}
                  value={values.confirm}
                  onChange={(e) => setValue('confirm', e.target.value)}
                  onBlur={() => markTouched('confirm')}
                  placeholder="••••••"
                  autoComplete="new-password"
                  required
                />
                {show('confirm') && <div className="invalid-feedback">{errors.confirm}</div>}
              </div>

              {/*role */}
              <div className="mb-3">
                <label htmlFor="role" className="form-label">Role</label>
                <select
                  id="role"
                  className={`form-select form-select-sm ${show('role') ? 'is-invalid' : ''}`}
                  value={values.role}
                  onChange={(e) => setValue('role', e.target.value)}
                  onBlur={() => markTouched('role')}
                  required
                >
                  <option value="">Choose your role</option>
                  <option value="ev">Electric Vehicle Owner (EV Owner)</option>
                  <option value="bis">Company</option>
                  <option value="cv">Carbon Verification & Audit (CVA)</option>
                </select>
                {show('role') && <div className="invalid-feedback">{errors.role}</div>}
              </div>

              {/*agree */}
              <div className="d-flex align-items-center mb-3">
                <div className="form-check">
                  <input
                    className={`form-check-input ${show('agree') ? 'is-invalid' : ''}`}
                    type="checkbox"
                    id="agree"
                    checked={values.agree}
                    onChange={(e) => setValue('agree', e.target.checked)}
                    onBlur={() => markTouched('agree')}
                  />
                  <label className="form-check-label" htmlFor="agree">
                    I agree with <Link to="/terms">Terms of Use</Link> & <Link to="/privacy">Privacy Policy</Link>
                  </label>
                  {show('agree') && <div className="invalid-feedback d-block">{errors.agree}</div>}
                </div>
              </div>

              {/*sign up btn */}
              <button
                type="submit"
                disabled={loading}
                className="btn btn-primary btn-sm w-100 position-relative overflow-hidden"
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
                {loading ? 'Signing you in…' : 'Sign up'}
              </button>
            </form>

            <div className="text-center text-muted my-3">— or —</div>

            <button type="button" className="btn btn-outline-secondary btn-sm w-100">
              Sign in with Google
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
