import React, { useRef, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
//hiệu ứng ripple 
import useRipple from '../../hooks/useRipple'; 
import bg from '../../assets/background.png';


export default function Login() {
  const nav = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [remember, setRemember] = useState(false);
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [showPwd, setShowPwd] = useState(false);

  const ripple = useRipple();
  const btnRippleRef = useRef(null);

  const validate = () => {
    const e = {};
    if (!email.trim()) e.email = 'Email is required';
    else if (!/^\S+@\S+\.\S+$/.test(email)) e.email = 'Enter a valid email';

    if (!password) e.password = 'Password is required';
    else if (password.length < 6) e.password = 'Min 6 characters';

    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const submit = async (ev) => {
    ev.preventDefault();
    if (!validate()) return;
    setLoading(true);
    //giả lập call API
    await new Promise((r) => setTimeout(r, 1000));
    setLoading(false);
    setSuccess(true);
    setTimeout(() => nav('/'), 1200);
  };

  return (
    <div className=" auth-hero min-vh-100 d-flex align-items-center justify-content-center"
      style={{ '--bg-url': `url(${bg})` }}
    >
      <div className="container" style={{ maxWidth: 440 }}>
        <div className="card shadow-sm">
          <div className="card-body p-4 p-md-5">
            <h1 className="h4 mb-4">Login</h1>

            {!success ? (
              <form onSubmit={submit} noValidate>
                {/*email*/}
                <div className="mb-3">
                  <label htmlFor="email" className="form-label">Email</label>
                  <input
                    id="email"
                    type="email"
                    className={`form-control ${errors.email ? 'is-invalid' : ''}`}
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    onBlur={validate}
                    placeholder="you@example.com"
                    autoComplete="email"
                    required
                  />
                  {errors.email && (
                    <div className="invalid-feedback">{errors.email}</div>
                  )}
                </div>

                {/*password, toggle */}
                <div className="mb-3">
                  <label htmlFor="password" className="form-label">Password</label>
                  <div className="input-group">
                    <input
                      id="password"
                      type={showPwd ? 'text' : 'password'}
                      className={`form-control ${errors.password ? 'is-invalid' : ''}`}
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      onBlur={validate}
                      placeholder="••••••"
                      autoComplete="current-password"
                      required
                    />
                    <button
                      type="button"
                      className="btn btn-outline-secondary"
                      onClick={() => setShowPwd((s) => !s)}
                      aria-label={showPwd ? 'Hide password' : 'Show password'}
                    >
                      {showPwd ? 'Hide' : 'Show'}
                    </button>
                    {errors.password && (
                      <div className="invalid-feedback d-block">{errors.password}</div>
                    )}
                  </div>
                </div>

                {/*remember, forgot*/}
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
                  <Link to="/forgot" className="ms-auto">Forgot password?</Link>
                </div>

                {/*submit */}
                <button
                  type="submit"
                  disabled={loading}
                  className="btn btn-primary w-100 position-relative overflow-hidden"
                  onClick={(e) => ripple(e, btnRippleRef.current)}
                >
                  {/*host cho ripple*/}
                  <span ref={btnRippleRef} className="ripple-host" />
                  {loading && (
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      role="status"
                      aria-hidden="true"
                    />
                  )}
                  {loading ? 'Loading…' : 'Login'}
                </button>
              </form>
            ) : (
              <div className="alert alert-success mb-0">
                <h5 className="alert-heading mb-1">Welcome back!</h5>
                <p className="mb-0">Signing you in…</p>
              </div>
            )}

            <div className="text-center text-muted my-3">— or —</div>

            {/*login google*/}
            <button type="button" className="btn btn-outline-secondary w-100">
              Login with Google
            </button>

            <p className="mt-3 text-center mb-0">
              Don't have an account? <Link to="/register">Sign up</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
