import { apiFetch } from "@/utils/apiFetch";

/**
 * Đếm số lượng phương tiện (vehicle) của user hoặc company
 * @param {Object} params - Các header hoặc query cần thiết
 * @param {string} params.requestTrace - Giá trị của header X-Request-Trace
 * @param {string} params.requestDateTime - Giá trị của header X-Request-DateTime
 * @returns {Promise<Object>} Kết quả response từ API
 */
export const countVehicle = async (params = {}) => {
  const { requestTrace, requestDateTime } = params;

  return apiFetch("/api/v1/vehicles/count", {
    method: "GET",
    headers: {
      "X-Request-Trace": requestTrace || "trace-id",
      "X-Request-DateTime": requestDateTime || new Date().toISOString(),
    },
  });
};



/**
 * Lấy lịch sử rút tiền (withdrawal history) dành cho admin
 * @param {Object} params - Tham số truyền vào (nếu có)
 * @param {string} [params.requestTrace] - Header X-Request-Trace (tùy chọn)
 * @param {string} [params.requestDateTime] - Header X-Request-DateTime (tùy chọn)
 * @returns {Promise<Object>} Dữ liệu lịch sử rút tiền
 */
export const getWithdrawlHistoryByAdmin = async (params = {}) => {
  const { requestTrace, requestDateTime } = params;

  return apiFetch("/api/v1/withdrawal/admin", {
    method: "GET",
    headers: {
      "X-Request-Trace": requestTrace || "withdrawl-history-admin",
      "X-Request-DateTime": requestDateTime || new Date().toISOString(),
    },
  });
};


