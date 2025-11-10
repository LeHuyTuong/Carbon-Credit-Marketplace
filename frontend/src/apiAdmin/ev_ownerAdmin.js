import { apiFetch } from "@/utils/apiFetch";

// ... api lấy list ev owner


export async function listEvOwners() {
  const res = await apiFetch("/api/v1/kyc/listEvowner", {
    method: "GET",
    headers: {
      "X-Request-Trace": `trace_${Date.now()}`,
      "X-Request-DateTime": new Date().toISOString(),
    },
  });

  return res?.response ?? [];
}