export async function apiFetch(path, options = {}) {
  const API = import.meta.env.VITE_API_BASE;
  
  //ưu tiên đọc token từ AuthContext
  let token;
  try {
    const authData =
      JSON.parse(sessionStorage.getItem("auth")) ||
      JSON.parse(localStorage.getItem("auth"));
    token = authData?.token;
  } catch {
    token = null;
  }

  //giữ lại token cũ nếu project trước đây lưu ở "token"
  if (!token) {
    token = localStorage.getItem("token");
  }

  //tạo traceId và datetime
  const traceId = crypto.randomUUID();
  const dateTime = new Date().toISOString();
  
  //cấu hình fetch
  const config = {
    method: options.method || "GET",
    headers: {
      "Content-Type": "application/json",
      Accept: "*/*",
      "X-Request-Trace": crypto.randomUUID(),
      "X-Request-DateTime": new Date().toISOString(),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {}),
    },
  };

  //tự động thêm trace và datetime vào body (nếu payload là JSON)
  if (config.method !== "GET" && options.body) {
    let body = options.body;

    //nếu payload là object, gắn thêm trace + datetime vào body
    if (typeof body === "object") {
      body = {
        requestTrace: traceId,
        requestDateTime: dateTime,
        ...body,
      };
      config.body = JSON.stringify(body);
    } else {
      config.body = body; // fallback nếu đã stringify trước
    }
  }
  console.log("Fetching:", `${API}${path}`, config); //debug

  //thực hiện fetch
  const res = await fetch(`${API}${path}`, config);
  const data = await res.json().catch(() => ({}));

  //nếu không ok thì ném lỗi
  if (!res.ok) {
    const msg =
      data?.responseStatus?.responseMessage ||
      data?.message ||
      `Request failed with status ${res.status}`;
    const error = new Error(msg);
    error.status = res.status;
    throw error;
  }

  return data;
}
