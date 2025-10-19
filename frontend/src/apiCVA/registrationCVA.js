import { apiFetch } from "@/utils/apiFetch";

export async function updateApplicationDecision(applicationId, payload) {
  const query = new URLSearchParams({
    approved: payload.approved,
    note: payload.note || "",
  });

  return apiFetch(`/api/v1/project-applications/${applicationId}/cva-decision?${query.toString()}`, {
    method: "PUT",
    headers: {
      "X-Request-Trace": crypto.randomUUID(),
      "X-Request-DateTime": new Date().toISOString(),
    },
  });
}
