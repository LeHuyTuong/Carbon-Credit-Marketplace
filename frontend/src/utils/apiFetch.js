export async function apiFetch(path, options = {}) {
  const API = import.meta.env.VITE_API_BASE;

  // Lấy token từ AuthContext hoặc localStorage/sessionStorage
  let token;
  try {
    const authData =
      JSON.parse(sessionStorage.getItem("auth")) ||
      JSON.parse(localStorage.getItem("auth"));
    token = authData?.token;
  } catch {
    token = null;
  }

  if (!token) {
    token = localStorage.getItem("token");
  }

  // Tạo traceId và datetime
  const traceId = crypto.randomUUID();
  const dateTime = new Date().toISOString();

  // Tạo headers
  const headers = {
    Accept: "*/*",
    "X-Request-Trace": traceId,
    "X-Request-DateTime": dateTime,
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {}),
  };

  // Chỉ thêm Content-Type nếu có body và method khác GET
  if (options.body && (options.method || "GET") !== "GET") {
    headers["Content-Type"] = "application/json";
  }

  const config = {
    method: options.method || "GET",
    headers,
  };

  // Thêm body nếu có, stringify nếu là object
  if (config.method !== "GET" && options.body) {
    config.body =
      typeof options.body === "object" ? JSON.stringify(options.body) : options.body;
  }

  console.log("Fetching:", `${API}${path}`, config); // debug

  // Thực hiện fetch
  const res = await fetch(`${API}${path}`, config);
  const data = await res.json().catch(() => ({}));

  // Nếu lỗi, log chi tiết và ném error
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
