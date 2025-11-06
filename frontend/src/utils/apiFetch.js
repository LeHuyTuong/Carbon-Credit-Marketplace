export async function apiFetch(path, options = {}) {
  const API = import.meta.env.VITE_API_BASE;

  // lấy token từ session hoặc localStorage
  let token;


  //  Ưu tiên token admin trước nếu có
  const adminToken =
    sessionStorage.getItem("admin_token") || localStorage.getItem("admin_token");

    //  Chỉ dùng adminToken khi đang ở trang /admin
  const isAdminPage = window.location.pathname.startsWith("/admin");

  // Nếu đang ở /admin và có admin_token hợp lệ → dùng token này
  if (isAdminPage && adminToken && adminToken !== "null" && adminToken !== "undefined") {
    token = adminToken;
  } else {
    // Nếu không ở admin → fallback sang token user (auth / token)
    try {
      const authData =
        JSON.parse(sessionStorage.getItem("auth")) ||
        JSON.parse(localStorage.getItem("auth"));
      token = authData?.token;
    } catch {
      token = null;
    }

    // Nếu không có token từ authData → lấy token cũ trong localStorage
    if (!token) token = localStorage.getItem("token");
  }

  //  Ưu tiên token cva trước nếu có
  const cvaToken =
    sessionStorage.getItem("cva_token") || localStorage.getItem("cva_token");

    //  Chỉ dùng cvaToken khi đang ở trang /cva
  const isCvaPage = window.location.pathname.startsWith("/cva");

  // Nếu đang ở /cva và có token hợp lệ → override token
  if (isCvaPage && cvaToken && cvaToken !== "null" && cvaToken !== "undefined") {
    token = cvaToken;
  } else {
    // Nếu không → fallback tương tự user token
    try {
      const authData =
        JSON.parse(sessionStorage.getItem("auth")) ||
        JSON.parse(localStorage.getItem("auth"));
      token = authData?.token;
    } catch {
      token = null;
    }

    if (!token) token = localStorage.getItem("token");
  }

  //sinh traceId và timestamp để be audit
  const traceId = crypto.randomUUID();
  const dateTime = new Date().toISOString();
  const hasValidToken = token && token !== "null" && token !== "undefined";

  //tạo header theo api
  const headers = {
    Accept: "*/*",
    "X-Request-Trace": traceId,
    "X-Request-DateTime": dateTime,
    ...(hasValidToken ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {}),
  };

  //http method
  const method = options.method || "GET";
  const config = { method, headers };

  // handle body (auto stringify nếu không phải formdata)
  if (method !== "GET" && options.body) {
    const isFormData = options.body instanceof FormData;

    if (!isFormData) {
      headers["Content-Type"] = "application/json";
      config.body =
        typeof options.body === "object"
          ? JSON.stringify(options.body)
          : options.body;
    } else {
      config.body = options.body; //formdata dữ nguyên
    }
  }

   //tránh double /api or //
  let cleanPath = path;
  if (API.endsWith("/api") && path.startsWith("/api")) {
    cleanPath = path.replace(/^\/api/, ""); //xóa /api bị lặp
  }
  if (API.endsWith("/") && cleanPath.startsWith("/")) {
    cleanPath = cleanPath.substring(1); //xóa / thừa
  }
  const url = `${API}${cleanPath}`;

  console.log("Fetching:", url, config);

  //gọi api
  const res = await fetch(url, config);
  //parse json an toàn tránh crash khi be ko trả json
  const data = await res.json().catch(() => ({}));

  // HTTP-level error
  if (!res.ok) {
    console.error("API Error:", { path, status: res.status, data });
  //ưu tiên message thật từ backend nếu có
  const beMsg =
    data?.responseStatus?.responseMessage ||
    data?.message ||
    data?.error ||
    `HTTP ${res.status}`;

    const error = new Error(beMsg);
    error.status = res.status;
    error.response = data;
    error.code =
    data?.responseStatus?.responseCode?.toString() || res.status.toString();
    throw error;
  }

  //be-level logic check
  const rawCode = data?.responseStatus?.responseCode ?? "";
  const rawMessage = data?.responseStatus?.responseMessage ?? "";

  const code = String(rawCode).trim().toUpperCase();
  const message = String(rawMessage).trim().toUpperCase();

  //những status thành công của be
  const successValues = ["200", "201", "00000000", "SUCCESS", "OK"];

  // Nếu không có responseStatus → mặc định thành công
  let isSuccess = !data?.responseStatus;

  // Nếu có → chỉ cần code hoặc message nằm trong successValues là pass
  if (!isSuccess) {
    isSuccess =
      successValues.some((val) => code.includes(val) || message.includes(val));
  }

  console.log("[apiFetch] Parsed status:", { code, message, isSuccess });

  //nếu be trả lỗi logic
  if (!isSuccess) {
    //ném lỗi có đủ thông tin BE trả về
    const errMsg = data?.responseStatus?.responseMessage || "Server logical error.";
    const error = new Error(errMsg);
    error.status = res.status; // HTTP status (200)
    error.response = data; // toàn bộ payload BE
    error.code = code; // thêm code để FE nhận diện
    throw error;
  }

  //thành công
  return data;
}