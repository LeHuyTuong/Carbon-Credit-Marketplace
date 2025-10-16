import { apiFetch } from "@/utils/apiFetch";

//  Tạo mới project
export async function createProject(payload) {
  return await apiFetch("/api/v1/projects", {
    method: "POST",
    body: payload,
  });
}

//  Lấy danh sách tất cả projects
export async function getAllProjects() {
  return await apiFetch("/api/v1/projects/all", {
    method: "GET",
  });
}
export async function getProjectById(id) {
  return await apiFetch(`/api/v1/projects/${id}`, {
    method: "GET",
  });
}

