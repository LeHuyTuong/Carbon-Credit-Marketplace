import { apiFetch } from "@/utils/apiFetch";

/**
 * Gọi AI Core để phân tích báo cáo
 * @param {number|string} id - ID của report
 * @param {object} payload - Dữ liệu gửi lên AI Core
 * @returns {Promise<object>} Kết quả trả về từ backend
 */
export async function analyzeReportByAI(id, payload = {}) {
  if (!id) throw new Error("Report ID is required!");

  return await apiFetch(`/api/v1/reports/${id}/ai-score`, {
    method: "POST",
    body: payload,
  });
}
//AI for analysis Data
export const analyzeReportData = async (reportId) => {
  const trace = `trace_${Date.now()}`;
  const timestamp = new Date().toISOString();

  const res = await apiFetch(`/api/v1/reports/${reportId}/analyze`, {
    method: "POST",
    headers: {
      "X-Request-Trace": trace,
      "X-Request-DateTime": timestamp,
      "Content-Type": "application/json",
    },
    params: { persist: true },
  });

  return res.data || res;
};
