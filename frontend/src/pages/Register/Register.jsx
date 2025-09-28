import React, { useRef, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import useRipple from '../../hooks/useRipple';  
import bg from '../../assets/background.png';


export default function Register() {
  const nav = useNavigate();

  const [fullName, setFullName] = useState('');
  const [email, setEmail]       = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm]   = useState('');
  const [agree, setAgree]       = useState(false);

  const [showPwd,  setShowPwd]  = useState(false);
  const [showPwd2, setShowPwd2] = useState(false);

  const [errors, setErrors]     = useState({});
  const [loading, setLoading]   = useState(false);
  const [success, setSuccess]   = useState(false);

  const ripple = useRipple();
  const btnRippleRef = useRef(null);

  const validate = () => {
    const e = {};
    if (!email.trim()) e.email = 'Email is required';
    else if (!/^\S+@\S+\.\S+$/.test(email)) e.email = 'Email is invalid';

    if (!password) e.password = 'Password is required';
    else if (password.length < 6) e.password = 'At least 6 characters';

    if (!confirm) e.confirm = 'Please re-enter your password';
    else if (confirm !== password) e.confirm = 'The re-entered password does not match';

    if (!agree) e.agree = 'You need to agree to the Terms of Use & Privacy Policy';

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
    <div className="auth-hero min-vh-100 d-flex align-items-center justify-content-center">
      <div className="container auth-container" style={{ maxWidth: 500 }}>
        <div className="card shadow-sm">
          <div className="card-body p-4 p-md-5">
            <h1 className="h4 mb-4 text-center">Sign up</h1>

            {!success ? (
              <form onSubmit={submit} noValidate>
                {/*email */}
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
                  {errors.email && <div className="invalid-feedback">{errors.email}</div>}
                </div>

                {/*password*/}
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
                      autoComplete="new-password"
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
                  </div>
                  {errors.password && <div className="invalid-feedback d-block">{errors.password}</div>}
                </div>

                {/*re-enter pass*/}
                <div className="mb-3">
                  <label htmlFor="confirm" className="form-label">Re-enter Password</label>
                  <div className="input-group">
                    <input
                      id="confirm"
                      type={showPwd2 ? 'text' : 'password'}
                      className={`form-control ${errors.confirm ? 'is-invalid' : ''}`}
                      value={confirm}
                      onChange={(e) => setConfirm(e.target.value)}
                      onBlur={validate}
                      placeholder="••••••"
                      autoComplete="new-password"
                      required
                    />
                    <button
                      type="button"
                      className="btn btn-outline-secondary"
                      onClick={() => setShowPwd2((s) => !s)}
                      aria-label={showPwd2 ? 'Hide password' : 'Show password'}
                    >
                      {showPwd2 ? 'Hide' : 'Show'}
                    </button>
                  </div>
                  {errors.confirm && <div className="invalid-feedback d-block">{errors.confirm}</div>}
                </div>

                {/*điều khoản*/}
                <div className="d-flex align-items-center mb-3">
                  <div className="form-check">
                    <input
                      className={`form-check-input ${errors.agree ? 'is-invalid' : ''}`}
                      type="checkbox"
                      id="agree"
                      checked={agree}
                      onChange={(e) => setAgree(e.target.checked)}
                    />
                    <label className="form-check-label" htmlFor="agree">
                      I agree with <Link to="/terms">Terms of Use</Link> & <Link to="/privacy">Privacy Policy</Link>
                    </label>
                    {errors.agree && <div className="invalid-feedback d-block">{errors.agree}</div>}
                  </div>
                </div>

                {/*submit */}
                <button
                  type="submit"
                  disabled={loading}
                  className="btn btn-primary w-100 position-relative overflow-hidden"
                  onClick={(e) => ripple(e, btnRippleRef.current)}
                >
                  <span ref={btnRippleRef} className="ripple-host" />
                  {loading && <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />}
                  {loading ? 'Signing you in…' : 'Sign up'}
                </button>
              </form>
            ) : (
              <div className="alert alert-success mb-0">
                <h5 className="alert-heading mb-1">Create account successfully</h5>
                <p className="mb-0">Redirecting…</p>
              </div>
            )}

            <div className="text-center text-muted my-3">— or —</div>

            {/*sign up google*/}
            <button type="button" className="btn btn-outline-secondary w-100">
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
