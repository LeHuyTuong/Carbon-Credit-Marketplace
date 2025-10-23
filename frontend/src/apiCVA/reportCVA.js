// src/api/reportCVA.js
import { apiFetch } from "@/utils/apiFetch";

/**
 * Lấy danh sách báo cáo CVA (list-cva-check)
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
 * Cập nhật trạng thái report (Approved/Rejected)
 */
/**
 * @param {number} reportId
 * @param {boolean} approved
 * @param {string} comment (optional, required if rejected)
 */
export const verifyReportCVA = async (reportId, approved, comment = "") => {
  const params = new URLSearchParams();
  params.append("approved", approved);
  if (!approved && comment) params.append("comment", comment);

  return apiFetch(`/api/v1/reports/${reportId}/verify?${params.toString()}`, {
    method: "PUT",
  });
};