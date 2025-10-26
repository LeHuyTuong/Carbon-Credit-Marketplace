import { apiFetch } from "@/utils/apiFetch";
export const getAllCVAKYCProfiles = () => {
  return apiFetch("/api/v1/kyc/cva/list", {
    method: "GET",
    headers: {
      "X-Request-Trace": "admin-get-all-cva-kyc",
      "X-Request-DateTime": new Date().toISOString(),
    },
  });
};
export const  getKycProfileCVA = () => {
  return apiFetch("/api/v1/kyc/cva", {
    method: "GET",
    headers: {
      "X-Request-Trace": "get-current-cva-kyc",
      "X-Request-DateTime": new Date().toISOString(),
    },
  });
};

/**
 * Update KYC profile for CVA (current user)
 * @param {Object} data - Thông tin cập nhật KYC
 */
export const updateKycProfileCVA = (data) => {
  return apiFetch("api/v1/kyc/cva", {
    method: "PUT",
    headers: {
      "X-Request-Trace": "update-current-cva-kyc",
      "X-Request-DateTime": new Date().toISOString(),
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      requestTrace: "update-current-cva-kyc",
      requestDateTime: new Date().toISOString(),
      data,
    }),
  });
};