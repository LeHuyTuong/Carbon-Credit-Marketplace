import { apiFetch } from "@/utils/apiFetch";

/**
 *  Lấy danh sách yêu cầu rút tiền (cho admin)
 */
export const getWithdrawalsAdmin = async () => {
  try {
    const res = await apiFetch("/api/v1/withdrawal/admin", { method: "GET" });
    return res?.response || [];
  } catch (error) {
    console.error("Error fetching withdrawal admin list:", error);
    throw error;
  }
};




/**
 *  Duyệt hoặc từ chối yêu cầu rút tiền
 * @param {number} id - ID của yêu cầu
 * @param {boolean} accept - true = duyệt, false = từ chối
 */
export const processWithdrawal = async (id, accept) => {
  try {
    const res = await apiFetch(
      `/api/v1/withdrawal/admin/${id}/process/${accept}`,
      { method: "PATCH" }
    );
    return res?.response || null;
  } catch (error) {
    console.error("Error processing withdrawal:", error);
    throw error;
  }
};

export const getPaymentDetails = async () => {
  const res = await apiFetch("/api/v1/paymentDetails", { method: "GET" });
  return res?.response || null; // vì response là object chứ không phải mảng
};
