import { apiFetch } from "@/utils/apiFetch";

/**  CVA Login API */
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

    //  Lưu token & email RIÊNG CHO CVA
    if (token) {
      localStorage.setItem("cva_token", token);
      sessionStorage.setItem("cva_token", token);
    }

    if (email) {
      localStorage.setItem("cva_email", email);
      sessionStorage.setItem("cva_email", email);
    }
    if (resData.role) {
      localStorage.setItem("cva_role", resData.role);
      sessionStorage.setItem("cva_role", resData.role);
    }
    return resData;
  } catch (error) {
    console.error(" Login API Error:", error.message);
    throw error;
  }
};

/**  CVA KYC API */
export const apiKYCCVA = async (formData) => {
  try {
    const data = await apiFetch("/api/v1/kyc/cva/create", {
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
export const checkKYCCVA = async () => {
  try {
    const token = localStorage.getItem("cva_token");
    if (!token) throw new Error("No token found!");

    const data = await apiFetch("/api/v1/kyc/cva", {
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

export const updateKYCCVA = async (formData) => {
  try {
    const token = localStorage.getItem("cva_token");
    if (!token) throw new Error("No token found!");

    const data = await apiFetch("/api/v1/kyc/cva", {
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
    const token = localStorage.getItem("cva_token");
    if (!token) throw new Error("No cva token found!");

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

    console.log(" CVA logout successful:", data.responseData);

    //  Dọn token & thông tin admin sau khi logout
    localStorage.removeItem("cva_token");
    localStorage.removeItem("cva_role");
    localStorage.removeItem("cva_id");

    return data.responseData.message || "Logout successful";
  } catch (error) {
    console.error(" CVA logout API Error:", error.message);
    throw error;
  }
};
