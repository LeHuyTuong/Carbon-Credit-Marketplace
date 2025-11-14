import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../../utils/apiFetch";
import { useAuth } from "../../context/AuthContext";
import { toast } from "react-toastify";

export default function OAuth2Success() {
  const nav = useNavigate();
  const { login } = useAuth();

  useEffect(() => {
    const fetchOAuthData = async () => {
      try {
        // FE gọi API BE để nhận token
        const res = await apiFetch("/api/oauth2/success", { method: "GET" });

        const token = res?.token;
        const email = res?.email;
        const role = res?.role;

        if (!token) throw new Error("Missing token");

        login({ email, role }, token, true);

        toast.success("Logged in with Google!");
        nav("/home", { replace: true });
      } catch (err) {
        console.error(err);
        toast.error("Google login failed");
        nav("/login");
      }
    };

    fetchOAuthData();
  }, [login, nav]);

  return <div>Redirecting...</div>;
}
