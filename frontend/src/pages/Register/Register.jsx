import React, { useRef, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import useRipple from '../../hooks/useRipple';

export default function Register() {
  const nav = useNavigate();

  const [email, setEmail]       = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm]   = useState('');
  const [agree, setAgree]       = useState(false);

  const [errors, setErrors]     = useState({});
  const [touched, setTouched]   = useState({});
  const [submitted, setSubmitted] = useState(false);

  const [loading, setLoading]   = useState(false);

  const ripple = useRipple();
  const btnRippleRef = useRef(null);
  const [role, setRole] = useState('');

  //field-level validator
  const validateField = (name, val) => {
    switch (name) {
      case 'email':
        if (!val.trim()) return 'Email is required';
        if (!/^\S+@\S+\.\S+$/.test(val)) return 'Email is invalid';
        return '';
      case 'password':
        if (!val) return 'Password is required';
        if (val.length < 6) return 'At least 6 characters';
        return '';
      case 'confirm':
        if (!val) return 'Please re-enter your password';
        if (val !== password) return 'The re-entered password does not match';
        return '';
      case 'agree':
        if (!val) return 'You need to agree to the Terms of Use & Privacy Policy';
        return '';
      case 'role':
        if (!val) return 'Please choose your role';
        return '';
      default:
        return '';
    }
  };

  //form-level validator (chỉ dùng lúc submit)
  const validateForm = () => {
    const e = {
      email:    validateField('email', email),
      password: validateField('password', password),
      confirm:  validateField('confirm', confirm),
      agree:    validateField('agree', agree),
      role: validateField('role', role),
    };
    //lọc rỗng
    Object.keys(e).forEach(k => !e[k] && delete e[k]);
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  //lưu field name đã được blur
  const markTouched = (name) =>
    setTouched(prev => ({ ...prev, [name]: true }));

  const setErrorOf = (name, msg) =>
    setErrors(prev => ({ ...prev, [name]: msg || undefined }));

  //show lỗi khi đã blur hoặc submit
  const show = (n) => !!errors[n] && (touched[n] || submitted);

  //submit gọi validateFrom, lỗi thì ko call API
  const submit = async (ev) => {
    ev.preventDefault();
    setSubmitted(true);
    if (!validateForm()) return;

    setLoading(true);
    await new Promise((r) => setTimeout(r, 800)); // giả lập API
    setLoading(false);

    nav('/otp', { replace: true, state: { email } }); //chuyển sang otp
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
                    className={`form-control form-control-sm ${show('password') ? 'is-invalid' : ''}`}
                    value={password}
                    onChange={(e) => {
                      setPassword(e.target.value);
                      if (touched.password) setErrorOf('password', validateField('password', e.target.value));
                      if (touched.confirm) setErrorOf('confirm', validateField('confirm', confirm));
                    }}
                    onBlur={() => {
                      markTouched('password');
                      setErrorOf('password', validateField('password', password));
                    }}
                    placeholder="••••••"
                    autoComplete="new-password"
                    required
                  />
                  {show('password') && <div className="invalid-feedback d-block">{errors.password}</div>}
                </div>

                {/*confirm */}
                <div className="mb-3">
                <label htmlFor="confirm" className="form-label">Re-enter Password</label>
                <input
                  id="confirm"
                  type="password"
                  className={`form-control form-control-sm ${show('confirm') ? 'is-invalid' : ''}`}
                  value={confirm}
                  onChange={(e) => {
                    setConfirm(e.target.value);
                    if (touched.confirm) setErrorOf('confirm', validateField('confirm', e.target.value));
                  }}
                  onBlur={() => {
                    markTouched('confirm');
                    setErrorOf('confirm', validateField('confirm', confirm));
                  }}
                  placeholder="••••••"
                  autoComplete="new-password"
                  required
                />
                {show('confirm') && <div className="invalid-feedback d-block">{errors.confirm}</div>}
              </div>
              
              {/**choose role */}
              <div className="mb-3">
                <label htmlFor="role" className="form-label">Role</label>
                <select
                  id="role"
                  className={`form-select form-select-sm ${show('role') ? 'is-invalid' : ''}`}
                  value={role}
                  onChange={(e) => {
                    setRole(e.target.value);
                    if (touched.role) setErrorOf('role', validateField('role', e.target.value));
                  }}
                  onBlur={() => {
                    markTouched('role');
                    setErrorOf('role', validateField('role', role));
                  }}
                  required
                >
                  <option value="">Choose your role</option>
                  <option value="ev">Electric Vehicle Owner</option>
                  <option value="biz">Business</option>
                </select>
                {show('role') && <div className="invalid-feedback">Please choose your role</div>}
              </div>

              {/*agree */}
              <div className="d-flex align-items-center mb-3">
                <div className="form-check">
                  <input
                    className={`form-check-input ${show('agree') ? 'is-invalid' : ''}`}
                    type="checkbox"
                    id="agree"
                    checked={agree}
                    onChange={(e) => {
                      setAgree(e.target.checked);
                      if (touched.agree || submitted) setErrorOf('agree', validateField('agree', e.target.checked));
                    }}
                    onBlur={() => {
                      markTouched('agree');
                      setErrorOf('agree', validateField('agree', agree));
                    }}
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
                {loading && <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />}
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
