import { apiFetch } from "@/utils/apiFetch";

/**
 *  Lấy danh sách báo cáo CVA (list-cva-check)
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
 *  Lấy chi tiết 1 báo cáo cụ thể (dành cho CVA hoặc Admin)
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

  console.warn(" Unexpected response structure:", res);
  return null;
};

/**
 *  Cập nhật trạng thái report (Approved/Rejected)
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


/**
 *  NEW: Lấy chi tiết theo vehicle trong report
 *  API: GET /api/v1/reports/{id}/details
 *  Trả về mảng [{id, vehicleId, period, totalEnergy, co2Kg}, ...]
 */
export const getReportDetails = async (
  reportId,
  { page = 0, size = 20, sort = "id,asc", plateContains } = {}
) => {
  if (!reportId) throw new Error("Missing report ID");

  const qs = new URLSearchParams();
  qs.append("page", page);
  qs.append("size", size);
  if (sort) qs.append("sort", sort);
  if (plateContains) qs.append("plateContains", plateContains);

  const res = await apiFetch(`/api/v1/reports/${reportId}/details?${qs.toString()}`, {
    method: "GET",
  });

  // --- Chuẩn hóa mọi schema trả về thành Page object ---
  // TuongCommonResponse: { response: { content: [...] , ... } }
  if (res?.response?.content) return res.response;

  // CommonResponse: { responseData: { content: [...] , ... } }
  if (res?.responseData?.content) return res.responseData;

  // Spring Page thuần ở root
  if (res?.content) return res;

  // Fallback: nếu trả về mảng -> bọc thành Page
  if (Array.isArray(res)) {
    return {
      content: res,
      totalElements: res.length,
      number: 0,
      size: res.length,
      first: true,
      last: true,
      totalPages: 1,
      numberOfElements: res.length,
      sort: { empty: true, sorted: false, unsorted: true },
      pageable: {
        pageNumber: 0,
        pageSize: res.length,
        offset: 0,
        paged: false,
        unpaged: true,
        sort: { empty: true, sorted: false, unsorted: true },
      },
      empty: res.length === 0,
    };
  }

  // Không đoán được schema
  console.warn("Unexpected details response structure:", res);
  return {
    content: [],
    totalElements: 0,
    number: 0,
    size: size,
    first: true,
    last: true,
    totalPages: 0,
    numberOfElements: 0,
    empty: true,
  };
};

//lấy từng company kyc profile
export async function getCompanyKYCProfile(companyId) {
  return apiFetch(`/api/v1/kyc/${companyId}`, "GET");
}

/**
 *  Lấy danh sách rules với scoring guideline và evidence hint
 *  API: GET /api/v1/reports/rules
 *  Headers: X-Request-Trace, X-Request-DateTime
 */
export const getReportRules = async ({ traceId, traceDateTime } = {}) => {
  const headers = {};
  if (traceId) headers["X-Request-Trace"] = traceId;
  if (traceDateTime) headers["X-Request-DateTime"] = traceDateTime;

  const res = await apiFetch("/api/v1/reports/analysis/rules/rubric", {
    method: "GET",
    headers,
  });

  if (res?.response) return res.response;
  if (res?.responseData) return res.responseData;
  if (res?.data?.responseData) return res.data.responseData;

  console.warn("Unexpected response structure:", res);
  return [];
};