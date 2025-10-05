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

  //gửi mã otp
  const verify = async () => {
    if (otp.length !== 6) return alert('Please enter 6-digit OTP');

    try {
      const API = import.meta.env.VITE_API_BASE; //lấy biến môi trường
      const res = await fetch(`${API}/api/v1/auth/verify-otp`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: email.trim(),
          otpCode: otp,
        }),
      });

      const data = await res.json().catch(() => ({})); //đợi api trả kết quả

      if (!res.ok) {
        const message =
          data?.responseStatus?.responseMessage ||
          data?.message ||
          "OTP verification failed"; //status ko phải 200 thì báo lỗi
        throw new Error(message);
      }

      //xác thực thành công, be trả jwt token & list roles
      const token = data?.responseData?.jwt;
      const roles = data?.responseData?.roles || [];
      if (!token) throw new Error("Missing token from server");

      console.log("OTP verified:", { token, roles });

      //điều hướng sau khi xác thực thành công
      if (state?.from === "register") {
        nav("/kyc", {
          replace: true,
          state: { email, msg: "Account verified. Please fill in KYC form." },
        });
      } else {
        nav("/change", { replace: true });
      }
    } catch (err) {
      console.error("Verify OTP error:", err.message);
      alert(err.message);
    }
  };

  //gửi lại mã otp
  const resend = async () => {
    try {
      const API = import.meta.env.VITE_API_BASE;

      const res = await fetch(`${API}/api/v1/send-otp`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
      });

      const data = await res.json().catch(() => ({}));

      if (!res.ok) {
        const message =
          data?.responseStatus?.responseMessage ||
          data?.message ||
          "Failed to resend OTP";
        throw new Error(message);
      }

      //thành công thì reset time và thông báo
      setSec(60);
      alert("OTP has been resent to your email.");
      console.log("Resend OTP success:", data);
    } catch (err) {
      console.error("Resend OTP error:", err.message);
      alert(err.message);
    }
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
