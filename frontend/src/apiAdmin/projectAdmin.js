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
//lấy project theo id
export async function getProjectById(id) {
  return await apiFetch(`/api/v1/projects/${id}`, {
    method: "GET",
  });
}
export const updateProjectById = async (id, data) => {
  const body = {
    requestTrace: "trace-update-project",
    requestDateTime: new Date().toISOString(),
    data: {
      title: data.title,
      description: data.description,
      logo: data.logo,
      commitments: data.commitments,
      technicalIndicators: data.technicalIndicators,
      measurementMethod: data.measurementMethod,
      legalDocsUrl: data.legalDocsUrl,
    },
  };

  return await apiFetch(`/api/v1/projects/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
};

