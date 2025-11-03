import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { toast } from "react-toastify";

export default function OAuth2Success() {
  const [params] = useSearchParams();
  const token = params.get("token");
  const role = params.get("role");
  const nav = useNavigate();
  const { login } = useAuth();

  useEffect(() => {
    if (token) {
      const user = { role };
      login(user, token, true);
      toast.success("Logged in with Google!");
      nav("/home", { replace: true });
    } else {
      toast.error("Google login failed.");
      nav("/login");
    }
  }, [token, role, login, nav]);

  return <div>Redirecting...</div>;
}
