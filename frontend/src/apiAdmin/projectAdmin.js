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
export const updateProjectById = async (id, payload) => {
  try {
    const res = await fetch(`/api/v1/projects/${id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload), // gửi thẳng JSON
    });
    return await res.json();
  } catch (err) {
    console.error("Error updating project:", err);
    throw err;
  }
};


