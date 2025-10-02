import React, { useRef, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import useRipple from '../../hooks/useRipple';
import { useForm } from '../../hooks/useForm';
import { useAuth } from '../../context/AuthContext.jsx';

export default function Login() {
  const nav = useNavigate();
  const { login } = useAuth(); 
  const ripple = useRipple();
  const btnRippleRef = useRef(null);
  const [loading, setLoading] = useState(false);
  const [remember, setRemember] = useState(false); 

  //validator riêng cho login
  const validators = {
    email: (val) => {
      if (!val.trim()) return "Email is required";
      if (!/^\S+@\S+\.\S+$/.test(val)) return "Enter a valid email";
      return "";
    },
    password: (val) => {
      if (!val) return "Password is required";
      if (val.length < 6) return "Min 6 characters";
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
  } = useForm({ email: "", password: "" }, validators);

  const submit = async (ev) => {
    ev.preventDefault();
    setSubmitted(true);
    if (!validateForm()) return;

    setLoading(true);
    await new Promise((r) => setTimeout(r, 800));
    setLoading(false);

    //fake data để thấy navbar đổi
    const fakeUser = { id: 1, email: values.email};
    const fakeToken = 'fake-jwt';
    login(fakeUser, fakeToken, remember);  
    nav('/home', { replace: true });
  };

  // const submit = async (ev) => {
  //   ev.preventDefault();
  //   setSubmitted(true);
  //   if (!validateForm()) return;

  //   setLoading(true);
  //   try {
  //     const res = await fetch("http://localhost:8082/api/v1/auth/login", {
  //       method: "POST",
  //       headers: { "Content-Type": "application/json" },
  //       body: JSON.stringify({ email: values.email, password: values.password }),
  //     });

  //     if (!res.ok) {
  //       const errData = await res.json().catch(() => ({}));
  //       throw new Error(errData.message || "Login failed");
  //     }

  //     const data = await res.json();
  //     // Giả sử backend trả: { token, user }
  //     const token = data.token;
  //     const user  = data.user || { email: values.email };

  //     // Nếu backend chỉ trả token JWT mà không trả user, có thể decode nhanh (nếu là JWT chuẩn)
  //     // Cẩn trọng: chỉ fallback, không phụ thuộc hoàn toàn
  //     // try { const payload = JSON.parse(atob(token.split('.')[1])); user.name = payload.name || user.email; } catch {}

  //     login(user, token, remember);
  //     nav('/home', { replace: true });
  //   } catch (err) {
  //     console.error("Login error:", err.message);
  //     alert(err.message);
  //   } finally {
  //     setLoading(false);
  //   }
  // };



  return (
    <div className="auth-hero min-vh-100 d-flex align-items-center justify-content-center">
      <div className="container" style={{ maxWidth: 440 }}>
        <div className="card shadow-sm">
          <div className="card-body p-4 p-md-5">
            <h1 className="h4 mb-4 text-center">Login</h1>

            <form onSubmit={submit} noValidate>
              {/*email */}
              <div className="mb-3">
                <label htmlFor="email" className="form-label">Email</label>
                <input
                  id="email"
                  type="email"
                  className={`form-control ${show('email') ? 'is-invalid' : ''}`}
                  value={values.email}
                  onChange={(e) => setValue("email", e.target.value)}
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
                  className={`form-control ${show('password') ? 'is-invalid' : ''}`}
                  value={values.password}
                  onChange={(e) => setValue("password", e.target.value)}
                  onBlur={() => markTouched('password')}
                  placeholder="••••••"
                  autoComplete="current-password"
                  required
                />
                {show('password') && <div className="invalid-feedback">{errors.password}</div>}
              </div>

              {/*remember & link */}
              <div className="d-flex align-items-center mb-3">
                <div className="form-check">
                  <input
                    className="form-check-input"
                    type="checkbox"
                    id="remember"
                    checked={remember}
                    onChange={(e) => setRemember(e.target.checked)}
                  />
                  <label className="form-check-label" htmlFor="remember">Remember me</label>
                </div>
                <Link to="/forgot" className="ms-auto">Forgot password?</Link>
              </div>

              {/*login btn */}
              <button
                type="submit"
                disabled={loading}
                className="btn btn-primary w-100 position-relative overflow-hidden"
                onClick={(e) => ripple(e, btnRippleRef.current)}
              >
                <span ref={btnRippleRef} className="ripple-host" />
                {loading ? 'Loading…' : 'Login'}
              </button>
            </form>

            <div className="text-center text-muted my-3">— or —</div>
            <button type="button" className="btn btn-outline-secondary w-100">Login with Google</button>
            <p className="mt-3 text-center mb-0">
              Don't have an account? <Link to="/register">Sign up</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
