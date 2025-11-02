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
