import { apiFetch } from "@/utils/apiFetch";
//Lấy danh sách báo cáo cho admin (có thể lọc theo status, page, size)
export const getAllReportsAdmin = async ({ status = "", page = 0, size = 20 } = {}) => {
  const params = new URLSearchParams();
  if (status) params.append("status", status);
  params.append("page", page);
  params.append("size", size);

  return apiFetch(`/api/v1/reports/list?${params.toString()}`, {
    method: "GET",
  });
};

//  Admin final approval
export const approveReportByAdmin = async (reportId, approved, note = "") => {
  const params = new URLSearchParams();
  params.append("approved", approved);
  if (note) params.append("note", note);

  return apiFetch(`/api/v1/reports/${reportId}/approve?${params.toString()}`, {
    method: "PUT",
  });
};
// reportAdmin
export const getReportByIdAdmin = async (id) => {
  return await apiFetch(`/api/v1/reports/${id}`, {
    method: "GET",
  });
};
//lấy từng company kyc profile
export async function getCompanyKYCProfile(companyId) {
  return apiFetch(`/api/v1/kyc/${companyId}`, "GET");
}

// Xem trước thông tin tín chỉ (preview credits) theo reportId
export const getCreditPreviewByReportId = async (reportId) => {
  if (!reportId) throw new Error("reportId is required");

  return apiFetch(`/api/v1/credits/preview/${reportId}`, {
    method: "GET",
  });
};
