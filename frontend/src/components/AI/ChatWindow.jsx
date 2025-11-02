import React, { useState, useEffect, useRef } from "react";
import ChatInput from "./ChatInput";

export default function ChatWindow() {
  const [messages, setMessages] = useState([
    {
      sender: "ai",
      text: "Hello 👋 I am CarbonX Assistant. I can help you analyze credits, market prices, or carbon footprint reports.",
    },
  ]);
  const [typing, setTyping] = useState(false);
  const chatRef = useRef(null);

  const handleSend = (msg) => {
    if (!msg.trim()) return;

    setMessages((prev) => [...prev, { sender: "user", text: msg }]);
    setTyping(true);

    // giả lập AI phản hồi
    setTimeout(() => {
      setTyping(false);
      setMessages((prev) => [
        ...prev,
        {
          sender: "ai",
          text: `I've analyzed your input: “${msg}”. Estimated CO₂ deviation: +2.4%.`,
        },
      ]);
    }, 1200);
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
              //   <img
              //     src="/src/assets/logo.png"
              //     alt="AI"
              //     className="chat-avatar"
              //   />
              <i className="bi bi-robot chat-avatar text-info fs-4"></i>
            )}
            <div className={`chat-bubble ${m.sender}`}>{m.text}</div>
            {m.sender === "user" && (
              <i className="bi bi-person-circle chat-avatar text-success fs-4"></i>
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
