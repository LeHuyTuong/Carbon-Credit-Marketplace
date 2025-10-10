import { useEffect, useRef, useState } from 'react';
import './otpinput.css';

export default function OTPCode({ length = 6, autoFocus = true, onComplete }) {
  const [code, setCode] = useState('');
  const inputRef = useRef(null);

  useEffect(() => {
    if (autoFocus) setTimeout(() => inputRef.current?.focus(), 0);
  }, [autoFocus]);

  const sanitize = (val) => val.replace(/\D/g, '').slice(0, length);

  const handleChange = (e) => setCode(sanitize(e.target.value));
  const handlePaste  = (e) => {
    const text = (e.clipboardData.getData('text') || '');
    setCode(sanitize(text));
    e.preventDefault();
  };

  useEffect(() => {
    if (code.length === length) onComplete?.(code);
  }, [code, length, onComplete]);

  const focusHidden = () => inputRef.current?.focus();

  return (
    <div className="otp-wrap" onClick={focusHidden}>
      {/*input ẩn nhận toàn bộ số */}
      <input
        ref={inputRef}
        value={code}
        onChange={handleChange}
        onPaste={handlePaste}
        inputMode="numeric"
        autoComplete="one-time-code"
        autoCorrect="off"
        spellCheck={false}
        className="otp-hidden-input"
        aria-label="One-time passcode"
      />

      {/*6 ô hiển thị */}
      <div className="d-flex justify-content-center gap-3">
        {Array.from({ length }).map((_, i) => (
          <div
            key={i}
            className={`otp-box ${i === code.length ? 'otp-focus' : ''}`}
            role="presentation"
          >
            {code[i] || ''}
          </div>
        ))}
      </div>
    </div>
  );
}
