import { apiFetch } from "@/utils/apiFetch";

//  Lấy danh sách credits (có phân trang)
export async function getCredits(page = 0, size = 10) {
  const url = `/api/v1/credits/batches?page=${page}&size=${size}`;
  return await apiFetch(url, {
    method: "GET",
    headers: {
      "X-Request-Trace": new Date().toISOString(),
      "X-Request-DateTime": new Date().toISOString(),
    },
  });
}
// Issue credits theo reportId
export async function issueCredits(reportId) {
  if (!reportId) throw new Error("reportId is required");

  const url = `/api/v1/credits/issue?reportId=${reportId}`; // <-- chuyển thành query param
  return await apiFetch(url, {
    method: "POST",
    headers: {
      "X-Request-Trace": new Date().toISOString(),
      "X-Request-DateTime": new Date().toISOString(),
      "Accept": "*/*",
    },
  });
}
// Lấy chi tiết credit đã issue theo batchId
export async function getIssuedCreditByBatchId(batchId) {
  if (!batchId) throw new Error("batchId is required");

  const url = `/api/v1/credits/batches/${batchId}`;
  return await apiFetch(url, {
    method: "GET",
    headers: {
      "X-Request-Trace": new Date().toISOString(),
      "X-Request-DateTime": new Date().toISOString(),
      "Accept": "*/*",
    },
  });
}

