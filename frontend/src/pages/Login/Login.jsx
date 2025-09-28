import React, { useRef, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import useRipple from '../../hooks/useRipple';

export default function Login() {
  const nav = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [remember, setRemember] = useState(false);

  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});
  const [submitted, setSubmitted] = useState(false);

  const [loading, setLoading] = useState(false);

  const ripple = useRipple();
  const btnRippleRef = useRef(null);

  const validateField = (name, val) => {
    switch (name) {
      case 'email':
        if (!val.trim()) return 'Email is required';
        if (!/^\S+@\S+\.\S+$/.test(val)) return 'Enter a valid email';
        return '';
      case 'password':
        if (!val) return 'Password is required';
        if (val.length < 6) return 'Min 6 characters';
        return '';
      default:
        return '';
    }
  };

  //bắt lỗi
  const validateForm = () => {
    const e = {
      email: validateField('email', email),
      password: validateField('password', password),
    };
    Object.keys(e).forEach(k => !e[k] && delete e[k]);
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  //field đã đc blur
  const markTouched = (n) => setTouched(p => ({ ...p, [n]: true }));

  //cập nhật lỗi
  const setErrorOf  = (n, msg) => setErrors(p => ({ ...p, [n]: msg || undefined }));

  //show khi có lỗi
  const show = (n) => !!errors[n] && (touched[n] || submitted);

  //submit gọi validateFrom, lỗi thì ko call API
  const submit = async (ev) => {
    ev.preventDefault();
    setSubmitted(true);
    if (!validateForm()) return;

    setLoading(true);
    await new Promise((r) => setTimeout(r, 1000));
    setLoading(false);
    nav('/home');
  };

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
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    if (touched.email) setErrorOf('email', validateField('email', e.target.value));
                  }}
                  onBlur={() => {
                    markTouched('email');
                    setErrorOf('email', validateField('email', email));
                  }}
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
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  if (touched.password) setErrorOf('password', validateField('password', e.target.value));
                }}
                onBlur={() => {
                  markTouched('password');
                  setErrorOf('password', validateField('password', password));
                }}
                placeholder="••••••"
                autoComplete="current-password"
                required
              />
              {show('password') && <div className="invalid-feedback d-block">{errors.password}</div>}
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

              {/**login btn */}
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
