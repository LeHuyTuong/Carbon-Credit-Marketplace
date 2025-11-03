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
  Lấy lịch sử rút tiền (withdrawal history) dành cho admin
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

/**
 * Count all wallet transactions in the system (Admin)
 * @param {Object} params - { requestTrace: string, requestDateTime: string }
 * @returns {Promise<number>} - response count
 */
export const countWalletTransactions = async (params = {}) => {
  const { requestTrace, requestDateTime } = params;

  try {
    const res = await apiFetch("/api/v1/wallet/transactions/count", {
      method: "GET",
      headers: {
        "X-Request-Trace": requestTrace || "wallet-transaction-count",
        "X-Request-DateTime": requestDateTime || new Date().toISOString(),
      },
    });

    console.log("countWalletTransactions() full API response:", res);

    const count = res.response ?? res?.response?.response ?? 0;
    console.log("countWalletTransactions() normalized count:", count);

    return count;
  } catch (error) {
    console.error("Error counting wallet transactions:", error);
    throw error;
  }
};




/*
  Đếm tổng số người dùng trong hệ thống (Admin)
 */
export const countUsers = async (params = {}) => {
  try {
    const res = await apiFetch("/api/v1/users/count", {
      method: "GET",
      params,
    });

    // Chuẩn hóa dữ liệu trả về
    const count = res?.response ?? res?.data ?? res ?? 0;
    console.log("countUsers() API response:", res);
    console.log("countUsers() normalized count:", count);
    return count;
  } catch (error) {
    console.error("Error in countUsers():", error);
    return 0;
  }
};





