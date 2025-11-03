import React, { useState, useRef, useEffect } from "react";
import "./aipanel.css";

export default function AIInsightPanel() {
  const [expanded, setExpanded] = useState(false); // mở/thu gọn panel
  const [messages, setMessages] = useState([
    {
      from: "ai",
      text: "Welcome, CVA. I'm ready to analyze any report you open.",
    },
  ]);
  const [input, setInput] = useState("");
  const [typing, setTyping] = useState(false);
  const chatRef = useRef(null);

  // gửi tin nhắn và phản hồi mô phỏng từ AI
  const handleSend = () => {
    if (!input.trim()) return;

    const newMsg = { from: "user", text: input.trim() };
    setMessages((prev) => [...prev, newMsg]);
    setInput("");
    setTyping(true);

    setTimeout(() => {
      const aiResponse = {
        from: "ai",
        text: `I’ve analyzed your question: “${newMsg.text}”. Estimated CO₂ deviation: +2.3%.`,
      };
      setMessages((prev) => [...prev, aiResponse]);
      setTyping(false);
    }, 1200);
  };

  // cuộn xuống khi có tin nhắn mới
  useEffect(() => {
    if (chatRef.current) {
      chatRef.current.scrollTop = chatRef.current.scrollHeight;
    }
  }, [messages, typing]);

  return (
    <div
      className={`ai-insight-panel shadow-lg ${
        expanded ? "expanded" : "collapsed"
      }`}
    >
      {/* header panel */}
      <div className="panel-header" onClick={() => setExpanded(!expanded)}>
        {expanded ? (
          <>
            <span>CarbonX AI</span>
            <button
              className="btn btn-sm btn-outline-success"
              style={{ borderRadius: "6px" }}
            >
              {expanded ? "−" : "+"}
            </button>
          </>
        ) : (
          <span>CarbonX AI</span>
        )}
      </div>

      {/* phần nội dung */}
      {expanded && (
        <div className="ai-body">
          <h6>Quick Analysis</h6>

          {/* cửa sổ chat */}
          <div className="chat-window" ref={chatRef}>
            {messages.map((msg, i) => (
              <div
                key={i}
                className={`chat-row ${msg.from === "ai" ? "ai" : "user"}`}
              >
                {/* avatar AI hoặc user */}
                <div className="chat-avatar">
                  {msg.from === "ai" ? (
                    <i className="bi bi-robot fs-5 text-success"></i>
                  ) : (
                    <i className="bi bi-person-circle fs-5 text-primary"></i>
                  )}
                </div>

                {/* bong bóng chat */}
                <div className={`chat-bubble ${msg.from}`}>{msg.text}</div>
              </div>
            ))}

            {/* hiệu ứng AI đang gõ */}
            {typing && (
              <div className="chat-row ai">
                <div className="chat-avatar">
                  <i className="bi bi-robot fs-5 text-success"></i>
                </div>
                <div className="chat-bubble ai typing">
                  <span className="dot"></span>
                  <span className="dot"></span>
                  <span className="dot"></span>
                </div>
              </div>
            )}
          </div>

          {/* ô nhập chat */}
          <div className="chat-section">
            <input
              className="chat-input"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Ask about CO₂ data..."
              onKeyDown={(e) => e.key === "Enter" && handleSend()}
            />
            <button className="btn btn-success btn-sm" onClick={handleSend}>
              Send
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
