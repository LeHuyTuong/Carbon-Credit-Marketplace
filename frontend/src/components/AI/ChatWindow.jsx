import React, { useState, useEffect, useRef } from "react";
import { devError } from "@/utils/devLog";
import ChatInput from "./ChatInput";
import { apiFetch } from "../../utils/apiFetch";
import "./aichat.css";

export default function ChatWindow() {
  const [messages, setMessages] = useState([
    {
      sender: "ai",
      text: "Hello 👋 I am CarbonX Assistant. I can help you analyze credits, market prices, or carbon footprint reports.",
    },
  ]);
  const [typing, setTyping] = useState(false);
  const chatRef = useRef(null);

  const handleSend = async (msg) => {
    if (!msg.trim()) return;
    setMessages((prev) => [...prev, { sender: "user", text: msg }]);
    setTyping(true);

    try {
      // lấy token
      let token = null;
      try {
        const authData =
          JSON.parse(sessionStorage.getItem("auth")) ||
          JSON.parse(localStorage.getItem("auth"));
        token = authData?.token;
      } catch {
        token = localStorage.getItem("token");
      }

      // Gọi fetch chuẩn với BE
      const res = await fetch("/api/v1/ai/chat", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ message: msg }),
      });

      if (!res.ok) throw new Error(`Server error: ${res.status}`);

      //be trả về String, nên đọc bằng .text()
      const aiText = await res.text();

      setMessages((prev) => [...prev, { sender: "ai", text: aiText }]);
    } catch (err) {
      devError("AI chat error:", err);
      setMessages((prev) => [
        ...prev,
        { sender: "ai", text: "Error: Unable to reach AI service." },
      ]);
    } finally {
      setTyping(false);
    }
  };

  // auto scroll xuống cuối khi có tin mới
  useEffect(() => {
    if (chatRef.current) {
      chatRef.current.scrollTop = chatRef.current.scrollHeight;
    }
  }, [messages, typing]);

  return (
    <div className="d-flex flex-column" style={{ height: "380px" }}>
      <div ref={chatRef} className="flex-grow-1 overflow-auto mb-2 chat-window">
        {messages.map((m, i) => (
          <div key={i} className={`chat-row ${m.sender}`}>
            {m.sender === "ai" && (
              <i className="bi bi-robot chat-avatar text-success fs-4"></i>
            )}
            <div className={`chat-bubble ${m.sender}`}>{m.text}</div>
            {m.sender === "user" && (
              <i className="bi bi-person-circle chat-avatar text-muted fs-4"></i>
            )}
          </div>
        ))}

        {/* hiệu ứng typing */}
        {typing && (
          <div className="chat-bubble ai typing">
            <span className="dot"></span>
            <span className="dot"></span>
            <span className="dot"></span>
          </div>
        )}
      </div>

      <ChatInput onSend={handleSend} />
    </div>
  );
}
