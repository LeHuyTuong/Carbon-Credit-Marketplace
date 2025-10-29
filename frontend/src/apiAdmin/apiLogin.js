import { apiFetch } from "@/utils/apiFetch";

/** 🔹 Admin Login API */
export const apiLogin = async (email, password) => {
  try {
    const data = await apiFetch("/api/v1/auth/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ email, password }),
    });

    if (!data || !data.responseStatus) {
      throw new Error("Invalid API response!");
    }

    const code = data.responseStatus?.responseCode || "";
    const message = data.responseStatus?.responseMessage || "Unknown error";

    if (code !== "200" && code !== "SUCCESS") {
      throw new Error(message);
    }

    const resData = data.responseData || {};
    const token = resData.jwt;

    // 💾 Lưu token & email RIÊNG CHO ADMIN
    if (token) {
      localStorage.setItem("admin_token", token);
      sessionStorage.setItem("admin_token", token);
    }

    if (email) {
      localStorage.setItem("admin_email", email);
      sessionStorage.setItem("admin_email", email);
    }

    return resData;
  } catch (error) {
    console.error("❌ Login API Error:", error.message);
    throw error;
  }
};

/** 🔹 Admin KYC API */
export const apiKYCAdmin = async (formData) => {
  try {
    const data = await apiFetch("/api/v1/kyc/admin", {
      method: "POST",
      body: formData,
    });

    if (!data || !data.responseStatus) {
      throw new Error("Invalid KYC API response!");
    }

    const code = data.responseStatus?.responseCode || "";
    const message = data.responseStatus?.responseMessage || "Unknown KYC error";

    if (code !== "200" && code !== "SUCCESS") {
      throw new Error(message);
    }

    return data;
  } catch (error) {
    console.error("❌ KYC API Error:", error.message);
    throw error;
  }
};
