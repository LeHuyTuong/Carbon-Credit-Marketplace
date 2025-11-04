import React from "react";
import "./footer.css";
import logo from "/src/assets/logo.png";

export default function Footer() {
  // lấy năm hiện tại để hiển thị tự động trong phần bản quyền
  const year = new Date().getFullYear();
  return (
    // thẻ bao ngoài của footer toàn trang
    <footer className="site-footer">
      <div className="container">
        {/* khu vực chính gồm 3 cột nội dung */}
        <div className="row g-4 align-items-start pb-3">
          {/* cột đầu tiên: logo + mô tả ngắn + thông tin liên hệ */}
          <div className="col-12 col-md-5">
            <a href="/" className="footer-brand">
              {logo && <img src={logo} alt="CarbonX" />}
              <span>CarbonX</span>
            </a>

            {/* mô tả ngắn về dự án */}
            <p className="footer-copy">
              Turning EV emission reductions into verified, tradable carbon
              credits.
            </p>
            {/* địa chỉ liên hệ */}
            <p className="footer-copy" style={{ marginTop: "10px" }}>
              Address: 7 D1 Street, Long Thanh My, Thu Duc, Ho Chi Minh
            </p>
            {/* email liên hệ */}
            <p className="footer-copy" style={{ marginTop: "10px" }}>
              Email: carbonX@gmail.com
            </p>
          </div>

          {/* cột thứ hai: liên kết nhanh đến các sản phẩm chính */}
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

          {/* cột thứ ba: liên kết đến các tài nguyên hỗ trợ */}
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

        {/* đường phân cách giữa phần link và phần bản quyền */}
        <div className="footer-divider" />

        {/* phần dưới cùng: bản quyền + link điều khoản */}
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
