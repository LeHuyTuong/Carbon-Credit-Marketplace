import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";

//chặn trang theo role
export default function RoleRoute({ children, allowedRoles }) {
  const { role, isAuthenticated } = useAuth();
  console.log("Current role:", role);

   // role có thể là string hoặc array
  const roles = Array.isArray(role) ? role : [role];
  const hasPermission = roles.some((r) => allowedRoles.includes(r));


  //chưa login
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  //không có role hợp lệ
  if (!allowedRoles.includes(role))
    return <Navigate to="/unauthorized" replace />;

  return children;
}
