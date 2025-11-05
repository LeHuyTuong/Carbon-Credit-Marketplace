import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { toast } from "react-toastify";

export default function OAuth2Success() {
  // lấy token và role từ query string sau khi đăng nhập google
  const [params] = useSearchParams();
  const token = params.get("token");
  const role = params.get("role");
  //điều hướng
  const nav = useNavigate();
  // lấy hàm login từ context
  const { login } = useAuth();

  // tự động xử lý khi token có giá trị
  useEffect(() => {
    if (token) {
      // tạo user object tối thiểu để lưu vào context
      const user = { role };
      // thực hiện login với token nhận được từ backend
      login(user, token, true);
      // thông báo thành công và điều hướng về trang home
      toast.success("Logged in with Google!");
      nav("/home", { replace: true });
    } else {
      // nếu không có token, báo lỗi và quay về trang login
      toast.error("Google login failed.");
      nav("/login");
    }
  }, [token, role, login, nav]);

  // hiển thị nội dung tạm trong lúc điều hướng
  return <div>Redirecting...</div>;
}
