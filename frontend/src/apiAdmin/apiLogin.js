import { apiFetch } from "@/utils/apiFetch";

/**  Admin Login API */
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

    //  Lưu token & email RIÊNG CHO ADMIN
    if (token) {
      localStorage.setItem("admin_token", token);
      sessionStorage.setItem("admin_token", token);
    }

    if (email) {
      localStorage.setItem("admin_email", email);
      sessionStorage.setItem("admin_email", email);
    }
    if (resData.role) {
      localStorage.setItem("admin_role", resData.role);
      sessionStorage.setItem("admin_role", resData.role);
    }
    return resData;
  } catch (error) {
    console.error(" Login API Error:", error.message);
    throw error;
  }
};

/**  Admin KYC API */
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
    console.error(" KYC API Error:", error.message);
    throw error;
  }
};
/** Check Admin KYC API */
export const checkKYCAdmin = async () => {
  try {
    const token = localStorage.getItem("admin_token");
    if (!token) throw new Error("No token found!");

    const data = await apiFetch("/api/v1/kyc/admin", {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    console.log(" Full KYC check response:", data);

    if (
      data?.responseStatus?.responseCode === "00000000" &&
      data?.response &&
      Object.keys(data.response).length > 0
    ) {
      return data.response; // Trả về object response thực
    }

    return null;
  } catch (error) {
    console.error("Check KYC API Error:", error.message);
    return null;
  }
};

export const updateKYCAdmin = async (formData) => {
  try {
    const token = localStorage.getItem("admin_token");
    if (!token) throw new Error("No token found!");

    const data = await apiFetch("/api/v1/kyc/admin", {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`,
        // Không set 'Content-Type' khi dùng FormData, browser sẽ tự thêm
      },
      body: formData,
    });

    if (!data?.responseStatus) {
      throw new Error("Invalid KYC API response!");
    }

    const code = data.responseStatus.responseCode || "";
    const message = data.responseStatus.responseMessage || "Unknown KYC error";

    if (code !== "00000000") {
      throw new Error(message);
    }

    console.log("KYC updated successfully:", data.response);
    return data.response;
  } catch (error) {
    console.error("Update KYC API Error:", error.message);
    throw error;
  }
};

//logout

export const apiLogout = async () => {
  try {
    const token = localStorage.getItem("admin_token");
    if (!token) throw new Error("No admin token found!");

    const data = await apiFetch("/api/v1/auth/logout", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: "*/*",
      },
    });

    if (!data?.responseStatus) {
      throw new Error("Invalid API response format!");
    }

    const code = data.responseStatus.responseCode || "";
    const message = data.responseStatus.responseMessage || "Unknown logout error";

    if (code !== "200" && code !== "SUCCESS") {
      throw new Error(message);
    }

    console.log(" Admin logout successful:", data.responseData);

    //  Dọn token & thông tin admin sau khi logout
    localStorage.removeItem("admin_token");
    localStorage.removeItem("admin_role");
    localStorage.removeItem("admin_id");

    return data.responseData.message || "Logout successful";
  } catch (error) {
    console.error(" Admin logout API Error:", error.message);
    throw error;
  }
};

//create user
export const registerUser = async (data) => {
  try {
    const response = await apiFetch("/api/v1/auth/register", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        email: data.email,
        password: data.password,
        confirmPassword: data.confirmPassword,
        roleName: data.roleName,
      }),
    });

    return response;
  } catch (error) {
    console.error("Register user failed:", error);
    throw error;
  }
};

/**
 * Xác thực OTP sau khi đăng ký
 */
export const verifyOtp = async (data) => {
  try {
    const response = await apiFetch("/api/v1/auth/verify-otp", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        email: data.email,
        otpCode: data.otpCode,
      }),
    });

    return response;
  } catch (error) {
    console.error("Verify OTP failed:", error);
    throw error;
  }
};
