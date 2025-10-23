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

/**
 * Update a vehicle by ID
 * @param {number} id - Vehicle ID
 * @param {object} data - Vehicle data: { plateNumber, model, brand, companyId }
 * @returns {Promise<object>}
 */
export const updateVehicleById = async (id, data) => {
  try {
    const payload = {
      requestTrace: `trace-update-vehicle-${Date.now()}`,
      requestDateTime: new Date().toISOString(),
      data: {
        plateNumber: data.plateNumber,
        model: data.model,
        brand: data.brand,
        companyId: data.companyId,
      },
    };

    const res = await apiFetch(`/api/v1/vehicles/${id}`, {
      method: "PUT",
      headers: {
        "X-Request-Trace": payload.requestTrace,
        "X-Request-DateTime": payload.requestDateTime,
        "Content-Type": "application/json",
        "Accept": "application/json",
      },
      body: JSON.stringify(payload),
    });

    return res;
  } catch (error) {
    console.error("Error in updateVehicleById:", error);
    throw error;
  }
};
