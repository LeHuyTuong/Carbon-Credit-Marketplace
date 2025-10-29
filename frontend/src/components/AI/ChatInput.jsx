import { useState } from "react";

export default function ChatInput({ onSend }) {
  const [msg, setMsg] = useState("");

  const handleSubmit = () => {
    if (!msg.trim()) return;
    onSend(msg);
    setMsg("");
  };

  return (
    <div className="d-flex">
      <input
        type="text"
        className="form-control form-control-sm me-2"
        placeholder="Ask something..."
        value={msg}
        onChange={(e) => setMsg(e.target.value)}
        onKeyDown={(e) => e.key === "Enter" && handleSubmit()}
      />
      <button className="btn btn-sm btn-accent" onClick={handleSubmit}>
        <i className="bi bi-send-fill"></i>
      </button>
    </div>
  );
}
