import { useNavigate } from "react-router-dom";

export default function QuickAccountSelect() {
  const nav = useNavigate();

  //map roles với url khác
  const presetAccounts = {
    ev: {
      email: "hihi@gmail.com",
      password: "Linh123!",
      route: "/login",
    },
    company: {
      email: "lehuytuong2005@gmail.com",
      password: "Tuong2005@",
      route: "/login",
    },
    cva: {
      email: "tinbaoblizard567@gmail.com",
      password: "Nguanhoc123456@",
      route: "/cva/carbonX/mkp/login",
    },
    admin: {
      email: "admin1@gmail.com",
      password: "Tuong2005@",
      route: "/admin/carbonX/mkp/login",
    },
  };

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
