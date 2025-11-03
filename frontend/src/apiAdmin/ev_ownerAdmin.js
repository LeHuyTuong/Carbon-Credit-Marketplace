import { apiFetch } from "@/utils/apiFetch";

// ... api lấy list ev owner
export async function getCurrentKycProfile() {
  return apiFetch("/api/v1/kyc/user", {
    method: "GET",
    headers: {
      "X-Request-Trace": "trace-id-example",
      "X-Request-DateTime": new Date().toISOString(),
    },
  });
}