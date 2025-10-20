import { apiFetch } from "@/utils/apiFetch";

/**
 * 🔹 Lấy danh sách tất cả project applications
 */
export const getProjectApplications = async () => {
  try {
    const res = await apiFetch("/api/v1/project-applications/pending-cva");
    console.log("Raw API response:", res);

    const code = res?.responseStatus?.responseCode;
    if (code === "200" || code === "00000000") {
      // ✅ Chọn đúng mảng data từ responseData hoặc response
      const data = res.responseData || res.response || [];
      console.log("✅ Project Applications API:", data);
      return data;
    } else {
      console.warn("⚠️ API returned non-success code:", code);
      return [];
    }
  } catch (error) {
    console.error("❌ Error fetching project applications:", error);
    return [];
  }
};

//lấy thông tin chi tiết của một project application
export async function getProjectApplicationByIdForCVA(id) {
  return apiFetch(`/api/v1/project-applications/${id}`, {
    method: "GET",
    headers: {
      "X-Request-Trace": crypto.randomUUID(),
      "X-Request-DateTime": new Date().toISOString(),
    },
  });
}

/**
 * 🔹 Cập nhật quyết định duyệt/không duyệt application
 */
export async function updateApplicationDecision(applicationId, payload) {
  const query = new URLSearchParams({
    approved: payload.approved,
    note: payload.note || "",
  });

  return apiFetch(
    `/api/v1/project-applications/${applicationId}/cva-decision?${query.toString()}`,
    {
      method: "PUT",
      headers: {
        "X-Request-Trace": crypto.randomUUID(),
        "X-Request-DateTime": new Date().toISOString(),
      },
    }
  );
}
