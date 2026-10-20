import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

export default function QuickAccountSelect() {
  const nav = useNavigate();
  const [presetAccounts, setPresetAccounts] = useState(null);

  useEffect(() => {
    // Danh sách tài khoản demo chỉ được nạp ở môi trường dev. Nhánh này bị Vite thay
    // bằng false khi build production, nên dynamic import (và file quickAccounts.js) bị
    // loại khỏi bundle: mật khẩu demo — kể cả tài khoản admin — không được gửi cho
    // người dùng thật.
    if (import.meta.env.DEV) {
      import("./quickAccounts").then((module) => setPresetAccounts(module.default));
    }
  }, []);

  if (!presetAccounts) return null;

  const handleSelect = (key) => {
    const acc = presetAccounts[key];
    nav(acc.route, {
      state: {
        preset: {
          email: acc.email,
          password: acc.password,
        },
      },
    });
  };

  return (
    <div className="card p-3 mb-3">
      <h6>Quick Login</h6>
      <div className="d-flex flex-wrap gap-2">
        <button
          className="btn btn-outline-primary"
          onClick={() => handleSelect("ev")}
        >
          EV Owner
        </button>
        <button
          className="btn btn-outline-primary"
          onClick={() => handleSelect("company")}
        >
          Company
        </button>
        <button
          className="btn btn-outline-warning"
          onClick={() => handleSelect("cva")}
        >
          CVA
        </button>
        <button
          className="btn btn-outline-danger"
          onClick={() => handleSelect("admin")}
        >
          Admin
        </button>
      </div>
    </div>
  );
}
