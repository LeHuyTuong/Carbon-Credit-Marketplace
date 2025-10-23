// src/api/reportCVA.js
import { apiFetch } from "@/utils/apiFetch";

/**
 * 📋 Lấy danh sách báo cáo CVA (list-cva-check)
 */
export const getReportCVAList = async ({
  status,
  page = 0,
  size = 20,
  traceId,
  traceDateTime,
} = {}) => {
  const query = new URLSearchParams();
  if (status) query.append("status", status);
  query.append("page", page);
  query.append("size", size);

  const headers = {};
  if (traceId) headers["X-Request-Trace"] = traceId;
  if (traceDateTime) headers["X-Request-DateTime"] = traceDateTime;

  return apiFetch(`/api/v1/reports/list-cva-check?${query.toString()}`, {
    method: "GET",
    headers,
  });
};

/**
 * ✅ Lấy chi tiết 1 báo cáo cụ thể (dành cho CVA hoặc Admin)
 * API backend: GET /api/v1/reports/{id}
 */
export const getReportById = async (reportId) => {
  if (!reportId) throw new Error("Missing report ID");

  const res = await apiFetch(`/api/v1/reports/${reportId}`, {
    method: "GET",
  });

  if (res?.response) return res.response;
  if (res?.responseData) return res.responseData;
  if (res?.data?.responseData) return res.data.responseData;

  console.warn("⚠️ Unexpected response structure:", res);
  return null;
};


/**
 * 🧾 Cập nhật trạng thái report (Approved/Rejected)
 * Backend: PUT /api/v1/reports/{id}/verify?approved=true|false&comment=...
 */
export const verifyReportCVA = async (reportId, { approved, comment = "" }) => {
  if (!reportId) throw new Error("Missing report ID");

  const params = new URLSearchParams();
  params.append("approved", approved ? "true" : "false");
  if (!approved && comment) {
    params.append("comment", comment);
  }

  return apiFetch(`/api/v1/reports/${reportId}/verify?${params.toString()}`, {
    method: "PUT",
  });
};