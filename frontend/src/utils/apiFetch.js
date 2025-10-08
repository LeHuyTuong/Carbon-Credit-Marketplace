export async function apiFetch(path, options = {}) {
  const API = import.meta.env.VITE_API_BASE;

  const config = {
    method: options.method || "GET",
    headers: {
      "Content-Type": "application/json",
      Accept: "*/*",
      "X-Request-Trace": crypto.randomUUID(),
      "X-Request-DateTime": new Date().toISOString(),
      ...(options.headers || {}),
    },
  };

  //chỉ gán body khi không phải GET
  if (config.method !== "GET" && options.body) {
    config.body = options.body;
  }

  console.log("Fetching:", `${API}${path}`, config); //debug

  const res = await fetch(`${API}${path}`, config);
  const data = await res.json().catch(() => ({}));

  if (!res.ok) {
    const msg =
      data?.responseStatus?.responseMessage ||
      data?.message ||
      `Request failed with status ${res.status}`;
    throw new Error(msg);
  }

  return data;
}
