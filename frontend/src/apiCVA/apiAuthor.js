import { apiFetch } from "@/utils/apiFetch"

export const apiLogin = async (email, password) => {
  try {
    const data = await apiFetch("/api/v1/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password })
    })
    if (!data || !data.responseStatus) throw new Error("Invalid API response")
    const code = data.responseStatus.responseCode || ""
    const message = data.responseStatus.responseMessage || "Unknown error"
    if (code !== "200" && code !== "SUCCESS") throw new Error(message)
    const resData = data.responseData || {}
    const token = resData.jwt
    if (token) {
      localStorage.setItem("cva_token", token)
      sessionStorage.setItem("cva_token", token)
    }
    if (email) {
      localStorage.setItem("cva_email", email)
      sessionStorage.setItem("cva_email", email)
    }
    if (resData.role) {
      localStorage.setItem("cva_role", resData.role)
      sessionStorage.setItem("cva_role", resData.role)
    }
    return resData
  } catch (error) {
    throw error
  }
}

export const apiRegister = async (email, password, confirmPassword, roleName) => {
  try {
    const data = await apiFetch("/api/v1/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password, confirmPassword, roleName })
    })
    return data
  } catch (error) {
    throw error
  }
}

export const verifyOTP = async ({ email, otpCode }) => {
  try {
    const data = await apiFetch("/api/v1/auth/verify-otp", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, otpCode })
    })
    if (!data || !data.responseStatus) throw new Error("Invalid verify OTP response")
    const code = data.responseStatus.responseCode || ""
    const message = data.responseStatus.responseMessage || "Unknown error"
    if (code !== "200" && code !== "SUCCESS") throw new Error(message)
    const resData = data.responseData || {}
    const token = resData.jwt
    if (token) {
      localStorage.setItem("cva_token", token)
      sessionStorage.setItem("cva_token", token)
    }
    if (email) {
      localStorage.setItem("cva_email", email)
      sessionStorage.setItem("cva_email", email)
    }
    if (resData.role) {
      localStorage.setItem("cva_role", resData.role)
      sessionStorage.setItem("cva_role", resData.role)
    }
    return resData
  } catch (error) {
    throw error
  }
}

export const checkKYCCVA = async () => {
  try {
    const token = localStorage.getItem("cva_token")
    if (!token) throw new Error("No token found")
    const data = await apiFetch("/api/v1/kyc/cva", {
      method: "GET",
      headers: { Authorization: `Bearer ${token}` }
    })
    if (data && data.responseStatus && data.responseStatus.responseCode === "00000000" && data.response) {
      return data.response
    }
    return null
  } catch (error) {
    return null
  }
}
export const apiKYCCVA = async (formData) => {
  try {
    const token = localStorage.getItem("cva_token")
    if (!token) throw new Error("No token found")

    const data = await apiFetch("/api/v1/kyc/cva/create", {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
      body: formData
    })

    if (!data || !data.responseStatus) {
      throw new Error("Invalid KYC API response")
    }

    const code = data.responseStatus.responseCode || ""
    const message = data.responseStatus.responseMessage || "Unknown KYC error"

    if (code !== "00000000") {
      return data.responseData || {}
    }

    throw new Error(message)
  } catch (error) {
    throw error
  }
}

export const updateKYCCVA = async (formData) => {
  try {
    const token = localStorage.getItem("cva_token")
    if (!token) throw new Error("No token found")
    const data = await apiFetch("/api/v1/kyc/cva", {
      method: "PUT",
      headers: { Authorization: `Bearer ${token}` },
      body: formData
    })
    if (!data || !data.responseStatus) throw new Error("Invalid KYC response")
    const code = data.responseStatus.responseCode || ""
    const message = data.responseStatus.responseMessage || "Unknown error"
    if (code !== "00000000") throw new Error(message)
    return data.response
  } catch (error) {
    throw error
  }
}

export const apiLogout = async () => {
  try {
    const token = localStorage.getItem("cva_token")
    if (!token) throw new Error("No cva token found")
    const data = await apiFetch("/api/v1/auth/logout", {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, Accept: "*/*" }
    })
    if (!data || !data.responseStatus) throw new Error("Invalid API response")
    const code = data.responseStatus.responseCode || ""
    const message = data.responseStatus.responseMessage || "Unknown error"
    if (code !== "200" && code !== "SUCCESS") throw new Error(message)
    localStorage.removeItem("cva_token")
    localStorage.removeItem("cva_role")
    localStorage.removeItem("cva_id")
    return data.responseData ? data.responseData.message : "Logout successful"
  } catch (error) {
    throw error
  }
}