import { apiFetch } from "../../../../utils/apiFetch";

export const getVehicles = async () => {
  return await apiFetch("/api/v1/vehicles");
};

export const createVehicle = async (data) => {
  return await apiFetch("/api/v1/vehicles", {
    method: "POST",
    body:{
      data: {
        ownerId: data.ownerId,
        plateNumber: data.plateNumber,
        model: data.model,
        brand: data.brand,
        companyId: data.companyId,
      }, //bọc payload trong "data"
    },
  });
};

export const updateVehicle = async (id, data) => {
  return await apiFetch(`/api/v1/vehicles/${id}`, {
    method: "PUT",
    body: {
      data, 
    },
  });
};

export const deleteVehicle = async (id) => {
  return await apiFetch(`/api/v1/vehicles/${id}`, {
    method: "DELETE",
  });
};
