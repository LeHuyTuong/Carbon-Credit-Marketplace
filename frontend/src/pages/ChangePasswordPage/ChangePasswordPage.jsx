import React, { useRef } from "react";
import { Card } from "react-bootstrap";
import { useNavigate } from "react-router-dom";
import useReveal from "../../hooks/useReveal";
import ChangePasswordForm from "./ChangePasswordForm";

export default function ChangePasswordPage() {
  const nav = useNavigate();
  const sectionRef = useRef(null);
  useReveal(sectionRef);

  return (
    <div
      ref={sectionRef}
      className="auth-hero d-flex justify-content-center align-items-center min-vh-100 bg-light reveal"
    >
      <Card
        className="shadow-lg border-0 rounded-4 p-4"
        style={{ maxWidth: "700px", width: "100%" }}
      >
        <h3 className="fw-bold mb-4 d-flex align-items-center text-dark text-center">
          <i className="bi bi-shield-lock me-2"></i> Change Password
        </h3>

        <div className="mt-3">
          <ChangePasswordForm />
        </div>
      </Card>
    </div>
  );
}
