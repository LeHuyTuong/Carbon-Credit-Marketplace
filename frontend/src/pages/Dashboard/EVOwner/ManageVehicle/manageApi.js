import { apiFetch } from "../../../../utils/apiFetch";

//Lấy danh sách công ty đã được admin phê duyệt
export const getApprovedCompanies = async () => {
  const res = await apiFetch("/api/v1/project-applications", { method: "GET" });
  // Lọc các project có status ADMIN_APPROVED
  const approved = (res.response || []).filter(
    (item) => item.status?.toUpperCase() === "ADMIN_APPROVED"
  );

  // Loại trùng bằng Map: mỗi key là companyId
  const uniqueCompanies = Array.from(
    new Map(approved.map((c) => [c.companyId, c])).values()
  );

  // Chuẩn hóa format trả ra: {id, name}
  let companies = uniqueCompanies.map((c) => ({
    id: c.companyId,
    name: c.companyName,
  }));
  return companies;
};

//lấy danh sách xe của user
export const getVehicles = async () => {
  return await apiFetch("/api/v1/vehicles", { method: "GET"});
};

//tạo mới xe
export const createVehicle = async (data) => {
  const formData = new FormData();
  formData.append("plateNumber", data.plateNumber);
  formData.append("brand", data.brand);
  formData.append("model", data.model);
  formData.append("companyId", data.companyId);

  // File upload (nếu có)
  if (data.documentFile) {
    formData.append("documentFile", data.documentFile);
  }

  return await apiFetch("/api/v1/vehicles", {
    method: "POST",
    body: formData, // apiFetch sẽ tự nhận là FormData, không stringify
  });
};

//Cập nhật xe theo ID
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

//Xóa xe
export const deleteVehicle = async (id) => {
  return await apiFetch(`/api/v1/vehicles/${id}`, {
    method: "DELETE",
  });
};
