import React, { useState } from "react";
import ChatWindow from "./ChatWindow";
import "./aichat.css";

export default function AIChatWidget() {
  const [open, setOpen] = useState(false);

  return (
    <>
      {/*nút bật chat */}
      <button
        className="ai-chat-btn btn btn-success rounded-circle shadow-lg"
        onClick={() => setOpen(!open)}
      >
        <i className="bi bi-robot fs-4"></i>
      </button>

      {/*cửa sổ chat */}
      {open && (
        <div className="ai-chat-popup glass-card shadow-lg">
          <div className="d-flex justify-content-between align-items-center border-bottom pb-2 mb-2">
            <h6 className="text-accent fw-bold mb-0">CarbonX AI Assistant</h6>
            <button
              className="btn-close btn-close-muted"
              onClick={() => setOpen(false)}
            ></button>
          </div>
          <ChatWindow />
        </div>
      )}
    </>
  );
}
