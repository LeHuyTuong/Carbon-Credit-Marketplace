import { apiFetch } from "@/utils/apiFetch";

export const getVehicles = async (pageNo = 0, pageSize = 20, sort = "") => {
  try {
    let url = `/api/v1/vehicles/list?pageNo=${pageNo}&pageSize=${pageSize}`;
    if (sort) url += `&sort=${encodeURIComponent(sort)}`;

    const res = await apiFetch(url, {
      method: "GET",
      headers: {
        "X-Request-Trace": "trace-get-vehicles",
        "X-Request-DateTime": new Date().toISOString().split(".")[0] + "Z",
        "Accept": "application/json",
      },
    });

    const code = res?.responseStatus?.responseCode;
    if (code === "200" || code === "00000000") {
      const responseData = res.response || {};
      return {
        data: Array.isArray(responseData.items) ? responseData.items : [],
        pageNo: responseData.pageNo ?? 0,
        pageSize: responseData.pageSize ?? 20,
        totalPages: responseData.totalPages ?? 0,
      };
    }

    console.error("Unexpected response code:", code, res);
    return { data: [], pageNo: 0, pageSize: 20, totalPages: 0 };
  } catch (error) {
    console.error("Error in getVehicles:", error);
    return { data: [], pageNo: 0, pageSize: 20, totalPages: 0 };
  }
};
