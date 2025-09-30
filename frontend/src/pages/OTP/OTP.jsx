import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import OTPInputs from '../../components/OTPInput/OTPInput';
import './otp.css';

export default function VerifyOtp() {
  const nav = useNavigate();
  const { state } = useLocation();
  const email = state?.email || 'user@example.com';

  const [otp, setOtp] = useState('');
  const [sec, setSec] = useState(60);

  //đếm ngược thời gian
  useEffect(() => {
    const t = setInterval(() => setSec(s => (s > 0 ? s - 1 : 0)), 1000);
    return () => clearInterval(t);
  }, []);

  const resend = async () => {
    //call API resend
    setSec(60);
  };

  const verify = async () => {
    //call API verify OTP
    nav('/login')
  };

  return (
    <header className="auth-hero min-vh-100 d-flex align-items-center">
      <div className="container d-flex justify-content-center">
        <div className="card otp-card shadow-lg border-0">
          <div className="card-body p-4 p-md-4">
            <h4 className="text-center fw-bold mb-1 text-uppercase">CarbonX</h4>
            <h5 className="text-center mb-4 text-uppercase">OTP VERIFICATION</h5>

            <div className="alert text-center otp-alert mb-3">
              <div className="small mt-1">
                Please enter OTP (one-time password) has sent to <strong>{email}</strong> to complete verification.
              </div>
            </div>

            <OTPInputs length={6} onComplete={setOtp} />

            <div className="d-flex justify-content-between align-items-center small text-muted mb-3">
              <span>
                Remaining time:{" "}
                <span className="text-success fw-semibold">{String(sec).padStart(2, '0')}s</span>
              </span>
              <button
                type="button"
                className="btn btn-link p-0 small"
                disabled={sec > 0}
                onClick={resend}
              >
                Resend
              </button>
            </div>

            <div className="d-grid gap-2">
              <button
                className="btn btn-primary btn-lg"
                disabled={otp.length !== 6}
                onClick={verify}
              >
                Verify
              </button>
              <button className="btn btn-secondary" onClick={() => nav(-1)}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}
