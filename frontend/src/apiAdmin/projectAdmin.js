import { apiFetch } from "@/utils/apiFetch";

//  Tạo mới project
export async function createProject(formData) {
  return await apiFetch("/api/v1/projects", {
    method: "POST",
    body: formData,
  });
}

//  Lấy danh sách tất cả projects
export async function getAllProjects() {
  return await apiFetch("/api/v1/projects/all", {
    method: "GET",
  });
}
//lấy project theo id
export async function getProjectById(id) {
  return await apiFetch(`/api/v1/projects/${id}`, {
    method: "GET",
  });
}
// Cập nhật project theo id

export async function updateProjectById(id, formData) {
  return await apiFetch(`/api/v1/projects/${id}`, {
    method: "PUT",
    body: formData,
  });
};