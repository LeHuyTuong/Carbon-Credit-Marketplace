import { apiFetch } from "@/utils/apiFetch";

// ===================== GET ALL COMPANY KYC PROFILES =====================
export const getAllCompanyKYCProfiles = async () => {
  try {
    const res = await apiFetch("/api/v1/kyc/company/listKYCCompany", {
      method: "GET",
      headers: {
        "X-Request-Trace": "admin-get-all-company-kyc",
        "X-Request-DateTime": new Date().toISOString(),
      },
    });
    return res;
  } catch (error) {
    console.error("Error fetching company KYC profiles:", error);
    throw error;
  }
};
export async function getCompanyKYCProfile() {
  return apiFetch("/api/v1/kyc/company", "GET");
}

export async function updateCompanyKYCProfile(data) {
  return apiFetch("/api/v1/kyc/company", "PUT", {
    data,
  });
}
