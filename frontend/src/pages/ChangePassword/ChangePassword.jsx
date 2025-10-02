import { useState, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import useRipple from "../../hooks/useRipple";
import { useForm } from "../../hooks/useForm";

export default function ChangePassword() {
  const nav = useNavigate();
  const ripple = useRipple();
  const btnRippleRef = useRef(null);
  const [loading, setLoading] = useState(false);

  //validator riêng cho change pass
  const validators = {
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
  };

  const {
    values,
    setValue,
    errors,
    show,
    validateForm,
    markTouched,
    setSubmitted,
  } = useForm({password: '', confirm: ''}, validators);

  const submit = async (ev) => {
    ev.preventDefault();
    setSubmitted(true);
    if (!validateForm()) return;

    setLoading(true);
    await new Promise((r) => setTimeout(r, 800)); //giả lập API
    setLoading(false);

    nav('/login', { replace: true, state: { email: values.email } });
  };

  return (
    <div className="auth-hero min-vh-100 d-flex align-items-center justify-content-center">
      <div className="container auth-container" style={{ maxWidth: 500 }}>
        <div className="card shadow-sm">
          <div className="card-body p-4 p-md-5">
            <h1 className="h4 mb-4 text-center">Reset Password</h1>

            <form onSubmit={submit} noValidate>
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

              {/*change btn */}
                <div class="modal-footer gap-2">
                  <button onClick={() => nav(-2)} //quay lại trang login
                          type="button" 
                          className="btn btn-secondary" 
                          data-bs-dismiss="modal"
                  >
                    Cancel
                  </button>
                  <button 
                          type="submit" 
                          disabled={loading}
                          className="btn btn-primary"
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
                            {loading ? 'Reset your password…' : 'Save changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
