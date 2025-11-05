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
    const formData = new FormData();

    // append tất cả key/value trong payload
    Object.entries(payload).forEach(([key, value]) => {
      if (value !== null && value !== undefined) {
        formData.append(key, value);
      }
    });

    const res = await fetch(`/api/v1/projects/${id}`, {
      method: "PUT",
      body: formData, // gửi form-data
      headers: {
        "X-Request-Trace": payload.requestTrace || `trace_${Date.now()}`,
        "X-Request-DateTime": payload.requestDateTime || new Date().toISOString(),
      },
    });

    return await res.json();
  } catch (err) {
    console.error("Error updating project:", err);
    throw err;
  }
};



