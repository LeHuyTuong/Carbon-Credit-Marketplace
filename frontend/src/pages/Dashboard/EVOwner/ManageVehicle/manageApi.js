import { apiFetch } from "../../../../utils/apiFetch";

const buildRequestBody = (data) => ({
  requestTrace: crypto.randomUUID(),
  requestDateTime: new Date().toISOString(),
  data,
});

export const getApprovedCompanies = async () => {
  const res = await apiFetch("/api/v1/project-applications", { method: "GET" });
  const approved = (res.response || []).filter(
    (item) => item.status?.toUpperCase() === "ADMIN_APPROVED"
  );

  const uniqueCompanies = Array.from(
    new Map(approved.map((c) => [c.companyId, c])).values()
  );

  let companies = uniqueCompanies.map((c) => ({
    id: c.companyId,
    name: c.companyName,
  }));

  // //fallback tạm cho test
  // if (companies.length === 0) {
  //   companies = [{ id: 1, name: "Admin Test Corp (Seed Data)" }];
  // }

  return companies;
};


export const getVehicles = async () => {
  return await apiFetch("/api/v1/vehicles", { method: "GET"});
};

export const createVehicle = async (data) => {
  return await apiFetch("/api/v1/vehicles", {
    method: "POST",
    body: buildRequestBody({
        plateNumber: data.plateNumber,
        model: data.model,
        brand: data.brand,
        companyId: data.companyId,
    }),
  });
};

export const updateVehicle = async (id, data) => {
  return await apiFetch(`/api/v1/vehicles/${id}`, {
    method: "PUT",
    body: buildRequestBody({
      plateNumber: data.plateNumber,
      model: data.model,
      brand: data.brand,
      companyId: data.companyId,
    }),
  });
};

export const deleteVehicle = async (id) => {
  return await apiFetch(`/api/v1/vehicles/${id}`, {
    method: "DELETE",
  });
};
