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
