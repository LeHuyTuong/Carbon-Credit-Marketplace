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

  //nếu lỗi, log chi tiết và ném lỗi với thông báo phù hợp
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
