import { apiFetch } from "@/utils/apiFetch";

// Lấy danh sách user
export async function getAllUsers() {
  return await apiFetch("/api/v1/users", { method: "GET" });
}

// Lấy chi tiết user theo email
export async function getUserById(id) {
  return await apiFetch(`/api/v1/users/${encodeURIComponent(id)}`, {
    method: "GET",
  });
}

//Update trạng thái user
export async function updateUserStatus(id, payload) {
  return await apiFetch(`/api/v1/users/${encodeURIComponent(id)}/status`, {
    method: "PATCH",
    body: payload,
  });
}


