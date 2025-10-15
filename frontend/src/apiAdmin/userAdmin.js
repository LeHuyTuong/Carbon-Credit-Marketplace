import { apiFetch } from "@/utils/apiFetch";

// Lấy danh sách user
export async function getAllUsers() {
  return await apiFetch("/v1/users", { method: "GET" });
}

// Lấy chi tiết user theo id
export async function getUserById(id) {
  return await apiFetch(`/v1/users/${id}`, { method: "GET" });
}

// Cập nhật user
export async function updateUser(id, payload) {
  return await apiFetch(`/v1/users/${id}`, {
    method: "PUT",
    body: payload,
  });
}
