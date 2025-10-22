import React from "react";
import { Toast, ToastContainer, Button } from "react-bootstrap";

export default function WalletToast({
  toast,
  setToast,
  showPaymentToast,
  setShowPaymentToast,
  nav,
}) {
  return (
    <ToastContainer position="top-center" className="p-3">
      <Toast
        bg={toast.type}
        show={toast.show}
        onClose={() => setToast({ ...toast, show: false })}
        delay={4000}
        autohide
      >
        <Toast.Header>
          <strong className="me-auto text-capitalize">
            {toast.type === "success"
              ? "Success"
              : toast.type === "danger"
              ? "Error"
              : "Notice"}
          </strong>
        </Toast.Header>
        <Toast.Body className="text-light">{toast.msg}</Toast.Body>
      </Toast>

      <Toast
        show={showPaymentToast}
        onClose={() => setShowPaymentToast(false)}
        autohide={false}
        bg="success"
      >
        <Toast.Header closeButton={true}>
          <strong className="me-auto text-success">
            Payment Detail Required
          </strong>
        </Toast.Header>
        <Toast.Body className="text-light">
          <p className="mb-3">
            You need to add your payment details before proceeding with a
            withdrawal.
          </p>
          <div className="d-flex justify-content-end gap-2">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setShowPaymentToast(false)}
            >
              Close
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={() => {
                setShowPaymentToast(false);
                nav("/payment-detail?create=true");
              }}
            >
              Add Now
            </Button>
          </div>
        </Toast.Body>
      </Toast>
    </ToastContainer>
  );
}
