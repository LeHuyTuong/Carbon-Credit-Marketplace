import React from "react";
import "./footer.css";
import logo from "/src/assets/logo.png";

export default function Footer() {
  const year = new Date().getFullYear();
  return (
    <footer className="site-footer">
      <div className="container">
        <div className="row g-4 align-items-start pb-3">
          <div className="col-12 col-md-5">
            <a href="/" className="footer-brand">
              {logo && <img src={logo} alt="CarbonX" />}
              <span>CarbonX</span>
            </a>
            <p className="footer-copy">
              Turning EV emission reductions into verified, tradable carbon
              credits.
            </p>
            <p className="footer-copy" style={{ marginTop: "10px" }}>
              Address: 7 D1 Street, Long Thanh My, Thu Duc, Ho Chi Minh
            </p>
            <p className="footer-copy" style={{ marginTop: "10px" }}>
              Email: carbonX@gmail.com
            </p>
          </div>

          <div className="col-6 col-md-3">
            <h6 className="footer-title">Product</h6>
            <ul className="footer-links">
              <li>
                <a href="/marketplace">Marketplace</a>
              </li>
              <li>
                <a href="/e-wallet">E-Wallet</a>
              </li>
              <li>
                <a href="/credits">Credits</a>
              </li>
            </ul>
          </div>

          <div className="col-6 col-md-3">
            <h6 className="footer-title">Resources</h6>
            <ul className="footer-links">
              <li>
                <a href="/report">Reports</a>
              </li>
              <li>
                <a href="/support">Support</a>
              </li>
            </ul>
          </div>
        </div>

        <div className="footer-divider" />

        <div className="footer-bottom">
          <small>© {year} CarbonX. All rights reserved.</small>
          <ul className="footer-legal">
            <li>
              <a href="/terms">Terms</a>
            </li>
            <li>
              <a href="/privacy">Privacy</a>
            </li>
          </ul>
        </div>
      </div>
    </footer>
  );
}
