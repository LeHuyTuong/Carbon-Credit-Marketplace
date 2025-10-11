import React from "react"
import bg from '../../assets/background.png';
import { useAuth } from "../../context/AuthContext.jsx"

export default function Hero() {
  const { isAuthenticated } = useAuth();//lấy trạng thái login
  
  return (
    <header id="top" className="auth-hero hero min-vh-100 d-flex align-items-center"
        style={{ '--bg-url': `url(${bg})` }}>
      <div className="container">
        <div className="row align-items-center">
          {/*detail */}
          <div className="col-lg-7 mb-5 mb-lg-0">
             <h4 className="text-welcome text-uppercase fw-semibold mb-2">
                Welcome to CarbonX
            </h4>

            <h1 className="display-4 fw-bold mb-3">
              Carbon credit exchange platform for electric vehicle owners
            </h1>

            <p className="lead text-body-secondary mb-4">
              Connecting EV owners and businesses — turning emission reductions into verified, tradable assets.
            </p>

            <div className="d-flex gap-3">
              {/*chỉ hiện khi chua login */}
              {!isAuthenticated && (
                <a href="/register" className="btn btn-brand btn-lg">Sign up now</a>
              )}
              <a href="#about" className="btn btn-outline-primary btn-lg">Learn more</a>
            </div>

            {/*badge benefits*/}
            <ul className="list-unstyled d-flex gap-4 flex-wrap small text-muted mt-4 mb-0" aria-label="Lợi ích chính">
              <li className="d-inline-flex align-items-center">⚡ Realtime pricing</li>
              <li className="d-inline-flex align-items-center">🌿 Verified impact</li>
              <li className="d-inline-flex align-items-center">💳 Quick settlement</li>
            </ul>
          </div>          
        </div>
      </div>
    </header>
  );
}
