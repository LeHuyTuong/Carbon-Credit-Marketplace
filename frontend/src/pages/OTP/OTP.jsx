import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import OTPInputs from "../../components/OTPInput/OTPInput";
import "./otp.css";
import { apiFetch } from "../../utils/apiFetch";
import { toast } from "react-toastify";

export default function VerifyOtp() {
  const nav = useNavigate();
  const { state } = useLocation();
  const email = state?.email || "user@example.com";
  const [otp, setOtp] = useState("");
  const [sec, setSec] = useState(60);
  const [loading, setLoading] = useState(false);

  //nếu truy cập thẳng, điều hướng về login
  useEffect(() => {
    if (!state?.from) {
      toast.error("Invalid access");
      nav("/login", { replace: true });
    }
  }, [state, nav]);

  //đếm ngược thời gian
  useEffect(() => {
    const t = setInterval(() => setSec((s) => (s > 0 ? s - 1 : 0)), 1000);
    return () => clearInterval(t);
  }, []);

  //gửi mã otp
  const verify = async () => {
    if (otp.length !== 6) return toast.warn("Please enter 6-digit OTP");
    setLoading(true);
    try {
      //gọi API verify OTP chung cho cả register và forgot
      const data = await apiFetch("/api/v1/auth/verify-otp", {
        method: "POST",
        body: JSON.stringify({ email, otpCode: otp }),
      });

      //kiểm tra responseCode từ server
      const resStatus = data?.responseStatus?.responseCode;
      const message = data?.responseStatus?.responseMessage || "";
      const jwt = data?.responseData?.jwt;

      //kiểm tra responseCode
      const isSuccess =
        resStatus === "200" || message?.toUpperCase() === "SUCCESS";

      if (!isSuccess) {
        throw new Error(message || "Invalid or expired OTP");
      }

      //điều hướng dựa trên luồng
      if (state?.from === "register") {
        toast.success("Account verified successfully!");
        nav("/login", { replace: true });
      } else if (state?.from === "forgot") {
        if (!jwt) {
          toast.error("Missing reset jwt from server");
          return;
        }
        toast.success("OTP verified! You can now reset your password.");
        nav("/change", { state: { email, otp, token: jwt } });
      } else {
        toast.info("Unknown flow");
        nav("/home", { replace: true });
      }
    } catch (err) {
      console.error("Verify OTP error:", err);
      toast.error(err.message || "Failed to verify OTP");
    } finally {
      setLoading(false);
    }
  };

  //gửi lại mã otp
  const resend = async (ev) => {
    //chặn spam nút gửi lại
    ev?.preventDefault?.();
    if (sec > 0 || loading) return;
    setLoading(true);
    try {
      await apiFetch("/api/v1/forgot-password/resend-otp", {
        method: "POST",
        body: JSON.stringify({ email: email.trim() }),
      });
      setSec(60);
      toast.success("OTP resent successfully");
    } catch (err) {
      toast.error(err.message || "Failed to resend OTP");
    } finally {
      setLoading(false);
    }
  };

  return (
    <header className="auth-hero min-vh-100 d-flex align-items-center">
      <div className="container d-flex justify-content-center">
        <div className="card otp-card shadow-lg border-0">
          <div className="card-body p-4 p-md-4">
            <h4 className="text-center fw-bold mb-1 text-uppercase">CarbonX</h4>
            <h5 className="text-center mb-4 text-uppercase">
              OTP VERIFICATION
            </h5>

            <div className="alert text-center otp-alert mb-3">
              <div className="small mt-1">
                Please enter OTP sent to <strong>{email}</strong> to complete
                verification.
              </div>
            </div>

            <OTPInputs length={6} onComplete={setOtp} />

            <div className="d-flex justify-content-between align-items-center small text-muted mb-3">
              <span>
                Remaining time:{" "}
                <span className="text-success fw-semibold">
                  {String(sec).padStart(2, "0")}s
                </span>
              </span>
              <button
                type="button"
                className="btn btn-link p-0 small"
                disabled={sec > 0 || loading}
                onClick={resend}
              >
                {sec > 0 ? `Resend in ${sec}s` : "Resend"}
              </button>
            </div>

            <div className="d-grid gap-2">
              <button
                className="btn btn-primary btn-lg"
                disabled={otp.length !== 6 || loading}
                onClick={verify}
              >
                {loading ? "Processing..." : "Verify"}
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
