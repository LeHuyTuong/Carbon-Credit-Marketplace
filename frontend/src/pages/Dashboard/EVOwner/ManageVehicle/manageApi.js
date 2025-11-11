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
  return companies;
};


export const getVehicles = async () => {
  return await apiFetch("/api/v1/vehicles", { method: "GET"});
};

export const createVehicle = async (data) => {
  const formData = new FormData();
  formData.append("plateNumber", data.plateNumber);
  formData.append("brand", data.brand);
  formData.append("model", data.model);
  formData.append("companyId", data.companyId);

  if (data.documentFile) {
    formData.append("documentFile", data.documentFile);
  }

  return await apiFetch("/api/v1/vehicles", {
    method: "POST",
    body: formData, // apiFetch sẽ tự nhận là FormData, không stringify
  });
};

export const updateVehicle = async (id, data) => {
  const formData = new FormData();
  formData.append("plateNumber", data.plateNumber);
  formData.append("brand", data.brand);
  formData.append("model", data.model);
  formData.append("companyId", data.companyId);

  if (data.documentFile) {
    formData.append("documentFile", data.documentFile);
  }

  return await apiFetch(`/api/v1/vehicles/${id}`, {
    method: "PUT",
    body: formData,
  });
};

export const deleteVehicle = async (id) => {
  return await apiFetch(`/api/v1/vehicles/${id}`, {
    method: "DELETE",
  });
};
