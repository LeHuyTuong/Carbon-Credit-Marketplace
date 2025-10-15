export async function apiFetch(path, options = {}) {
  const API = import.meta.env.VITE_API_BASE;

  // 🔹 Lấy token từ localStorage / sessionStorage
  let token = null;
  try {
    const authData =
      JSON.parse(sessionStorage.getItem("auth")) ||
      JSON.parse(localStorage.getItem("auth"));
    token = authData?.token;
  } catch {
    token = null;
  }

  // 🔹 fallback: nếu cũ lưu ở "token"
  if (!token) token = localStorage.getItem("token");

  // 🔹 Tạo traceId và thời gian
  const traceId = crypto.randomUUID();
  const dateTime = new Date().toISOString();

  
  // //cấu hình fetch
  // const config = {
  //   method: options.method || "GET",
  //   headers: {
  //     "Content-Type": "application/json",
  //     Accept: "*/*",
  //     "X-Request-Trace": crypto.randomUUID(),
  //     "X-Request-DateTime": new Date().toISOString(),
  //     ...(token ? { Authorization: `Bearer ${token}` } : {}),
  //     ...(options.headers || {}),
  //   },
  // };

  const headers = {
    Accept: "*/*",
    "X-Request-Trace": crypto.randomUUID(),
    "X-Request-DateTime": new Date().toISOString(),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {}),
  };

  // Chỉ thêm Content-Type nếu có body (hoặc không phải GET)
  if (options.body && (options.method || "GET") !== "GET") {
    headers["Content-Type"] = "application/json";
  }

  const config = {
    method: options.method || "GET",
    headers,
  };


  //tự động thêm trace và datetime vào body (nếu payload là JSON)

  if (config.method !== "GET" && options.body) {
    let bodyObj;
    try {
      bodyObj = typeof options.body === "string" ? JSON.parse(options.body) : options.body;
    } catch {
      bodyObj = options.body;
    }

    // ✅ Bọc payload vào "data"
    config.body = JSON.stringify({
      requestTrace: traceId,
      requestDateTime: dateTime,
      data: bodyObj,
    });
  }

  console.log("🚀 Fetching:", `${API}${path}`, config);

  // 🔹 Thực thi fetch
  const res = await fetch(`${API}${path}`, config);
  const data = await res.json().catch(() => ({}));


  //nếu lỗi, log chi tiết và ném lỗi với thông báo phù hợp

  // 🔹 Xử lý lỗi
 
  if (!res.ok) {
    console.error("API Error:", { path, status: res.status, data });
    let userMessage;
    switch (true) {
      case path.includes("/auth/login"):
        userMessage = "Login failed. Please check your credentials.";
        break;
      case path.includes("/auth/register"):
        userMessage = "Registration failed. Please try again.";
        break;
      case path.includes("/auth/change-password"):
        userMessage = "Password change failed. Please try again.";
        break;
      case path.includes("/auth/reset-password"):
        userMessage = "Password reset failed. Please try again.";
        break;
      case path.includes("/kyc"):
        userMessage = "KYC verification failed. Please try again.";
        break;
      case path.includes("/wallet"):
        userMessage = "Wallet transaction failed. Please try again.";
        break;
      case path.includes("/withdrawal"):
        userMessage = "Withdrawal request failed. Please try again.";
        break;
      case path.includes("/projects"):
        userMessage = "Project operation failed. Please try again.";
        break;
      default:
        userMessage = "Something went wrong. Please try again later.";
        break;
    }

    const error = new Error(userMessage);
    error.status = res.status;
    throw error;
  }

  return data;
}
