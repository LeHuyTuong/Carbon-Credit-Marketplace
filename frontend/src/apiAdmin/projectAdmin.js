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

    // Append all text + file fields
    Object.entries(payload).forEach(([key, value]) => {
      if (value !== null && value !== undefined) {
        formData.append(key, value);
      }
    });

    const token = localStorage.getItem("accessToken");

    const res = await fetch(`/api/v1/projects/${id}`, {
      method: "PUT",
      body: formData,
      headers: {
        Authorization: `Bearer ${token}`,
        "X-Request-Trace": payload.requestTrace || `trace_${Date.now()}`,
        "X-Request-DateTime": payload.requestDateTime || new Date().toISOString(),
      },
    });

    if (!res.ok) {
      const errorText = await res.text();
      console.error("Update failed:", res.status, errorText);
      throw new Error(`HTTP ${res.status}: ${errorText}`);
    }

    return await res.json();
  } catch (err) {
    console.error("Error updating project:", err);
    throw err;
  }
};


