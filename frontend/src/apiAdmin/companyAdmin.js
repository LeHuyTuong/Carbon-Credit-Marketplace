import { apiFetch } from "@/utils/apiFetch";

export const getProjectApplications = async () => {
  try {
    const res = await apiFetch("/api/v1/project-applications");
    console.log("Raw API response:", res);

    const code = res?.responseStatus?.responseCode;

    // ✅ chấp nhận cả "200" và "00000000" là thành công
    if (code === "200" || code === "00000000") {
      return res.response || [];
    } else {
      console.error("API returned an error:", res.responseStatus);
      return [];
    }
  } catch (error) {
    console.error("Network or parsing error:", error);
    return [];
  }
};
// 🟢 Lấy chi tiết theo ID
export const getProjectApplicationById = async (id) => {
  try {
    const res = await apiFetch(`/api/v1/project-applications/${id}`);
    console.log("Raw API response (detail):", res);

    const code = res?.responseStatus?.responseCode;
    if (code === "200" || code === "00000000") {
      return res.response || null;
    } else {
      console.error("API returned an error:", res.responseStatus);
      return null;
    }
  } catch (error) {
    console.error("Network or parsing error:", error);
    return null;
  }
};
export async function updateApplicationDecision(applicationId, payload) {
  const query = new URLSearchParams({
    approved: payload.approved,
    note: payload.note || "",
  });

  return apiFetch(`/api/v1/project-applications/${applicationId}/admin-decision?${query.toString()}`, {
    method: "PUT",
    headers: {
      "X-Request-Trace": crypto.randomUUID(),
      "X-Request-DateTime": new Date().toISOString(),
    },
  });
}



