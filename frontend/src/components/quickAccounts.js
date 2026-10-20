// Tài khoản demo dùng cho nút "Quick Login" ở môi trường DEV.
//
// File này được nạp bằng dynamic import bên trong nhánh `import.meta.env.DEV`:
// Vite thay nhánh đó bằng false khi build production nên dynamic import (và file này)
// bị loại khỏi bundle. Nhờ vậy mật khẩu demo — kể cả tài khoản admin — không nằm trong
// JavaScript mà mọi người dùng tải về.
export default {
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
