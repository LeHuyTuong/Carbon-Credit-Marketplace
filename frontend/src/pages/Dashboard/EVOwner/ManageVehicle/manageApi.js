import { apiFetch } from "../../../../utils/apiFetch";

export const getVehicles = async () => {
  return await apiFetch("/api/v1/vehicles");
};

export const createVehicle = async (data) => {
  return await apiFetch("/api/v1/vehicles", {
    method: "POST",
    body: JSON.stringify({
      requestTrace: crypto.randomUUID(),
      requestDateTime: new Date().toISOString(),
      data: {
        ownerId: data.ownerId,
        plateNumber: data.plateNumber,
        model: data.model,
        brand: data.brand,
        manufacturer: data.manufacturer,
        year: data.yearOfManufacture || 2025, //đổi key thành 'year'
      }, //bọc payload trong "data"
    }),
  });
};

export const updateVehicle = async (id, data) => {
  return await apiFetch(`/api/v1/vehicles/${id}`, {
    method: "PUT",
    body: JSON.stringify({
      requestTrace: crypto.randomUUID(),
      requestDateTime: new Date().toISOString(),
      data, //tương tự
    }),
  });
};

export const deleteVehicle = async (id) => {
  return await apiFetch(`/api/v1/vehicles/${id}`, {
    method: "DELETE",
  });
};
