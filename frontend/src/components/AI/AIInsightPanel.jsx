import React, { useState, useRef, useEffect } from "react";
import "./aipanel.css";

export default function AIInsightPanel() {
  const [expanded, setExpanded] = useState(false);
  const [messages, setMessages] = useState([
    {
      from: "ai",
      text: "Welcome, CVA. I'm ready to analyze any report you open.",
    },
  ]);
  const [input, setInput] = useState("");
  const [typing, setTyping] = useState(false);
  const chatRef = useRef(null);

  const handleSend = () => {
    if (!input.trim()) return;

    const newMsg = { from: "user", text: input.trim() };
    setMessages((prev) => [...prev, newMsg]);
    setInput("");
    setTyping(true);

    // mô phỏng phản hồi AI
    setTimeout(() => {
      const aiResponse = {
        from: "ai",
        text: `I’ve analyzed your question: “${newMsg.text}”. Estimated CO₂ deviation: +2.3%.`,
      };
      setMessages((prev) => [...prev, aiResponse]);
      setTyping(false);
    }, 1200);
  };

  // tự động scroll xuống cuối khi có tin nhắn mới
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

      {expanded && (
        <div className="ai-body">
          <h6>Quick Analysis</h6>

          <div className="chat-window" ref={chatRef}>
            {messages.map((msg, i) => (
              <div
                key={i}
                className={`chat-bubble ${msg.from === "ai" ? "ai" : "user"}`}
              >
                {msg.text}
              </div>
            ))}
            {typing && (
              <div className="chat-bubble ai typing">
                <span className="dot"></span>
                <span className="dot"></span>
                <span className="dot"></span>
              </div>
            )}
          </div>

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
