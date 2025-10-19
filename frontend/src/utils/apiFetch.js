// export async function apiFetch(path, options = {}) {
//   const API = import.meta.env.VITE_API_BASE;

//   //lấy token từ AuthContext hoặc localStorage/sessionStorage
//   let token;
//   try {
//     const authData =
//       JSON.parse(sessionStorage.getItem("auth")) ||
//       JSON.parse(localStorage.getItem("auth"));
//     token = authData?.token;
//   } catch {
//     token = null;
//   }

//   if (!token) token = localStorage.getItem("token");

//   //tạo traceId và datetime
//   const traceId = crypto.randomUUID();
//   const dateTime = new Date().toISOString();
// const hasValidToken = token && token !== "null" && token !== "undefined";

//   //base headers
//   const headers = {
//   Accept: "*/*",
//   "X-Request-Trace": traceId,
//   "X-Request-DateTime": dateTime,
//   ...(hasValidToken ? { Authorization: `Bearer ${token}` } : {}),
//   ...(options.headers || {}),
// };

//   const method = options.method || "GET";
//   const config = { method, headers };

//   //body handling
//   if (method !== "GET" && options.body) {
//     const isFormData = options.body instanceof FormData;

//     if (!isFormData) {
//       headers["Content-Type"] = "application/json";
//       config.body =
//         typeof options.body === "object"
//           ? JSON.stringify(options.body)
//           : options.body;
//     } else {
//       //không set Content-Type cho FormData (tránh lỗi boundary)
//       config.body = options.body;
//     }
//   }

//   console.log("Fetching:", `${API}${path}`, config);

//   const res = await fetch(`${API}${path}`, config);
//   const data = await res.json().catch(() => ({}));

//   //error handling
//   if (!res.ok) {
//     console.error("API Error:", { path, status: res.status, data });

//     let userMessage;
//     switch (true) {
//       case path.includes("/auth/login"):
//         userMessage = "Login failed. Please check your credentials.";
//         break;
//       case path.includes("/auth/register"):
//         userMessage = "Registration failed. Please try again.";
//         break;
//       case path.includes("/auth/change-password"):
//         userMessage = "Password change failed. Please try again.";
//         break;
//       case path.includes("/project-applications"):
//         userMessage = "Application submission failed. Please check your data.";
//         break;
//       case path.includes("/projects"):
//         userMessage = "Project operation failed. Please try again.";
//         break;
//       default:
//         userMessage = "Something went wrong. Please try again later.";
//         break;
//     }

//     const error = new Error(userMessage);
//     error.status = res.status;
//     error.response = data;
//     throw error;
//   }

//   return data;
// }
export async function apiFetch(path, options = {}) {
  const API = import.meta.env.VITE_API_BASE;

  // lấy token từ session hoặc localStorage
  let token;
  try {
    const authData =
      JSON.parse(sessionStorage.getItem("auth")) ||
      JSON.parse(localStorage.getItem("auth"));
    token = authData?.token;
  } catch {
    token = null;
  }

  if (!token) token = localStorage.getItem("token");

  const traceId = crypto.randomUUID();
  const dateTime = new Date().toISOString();
  const hasValidToken = token && token !== "null" && token !== "undefined";

  const headers = {
    Accept: "*/*",
    "X-Request-Trace": traceId,
    "X-Request-DateTime": dateTime,
    ...(hasValidToken ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {}),
  };

  const method = options.method || "GET";
  const config = { method, headers };

  // handle body
  if (method !== "GET" && options.body) {
    const isFormData = options.body instanceof FormData;

    if (!isFormData) {
      headers["Content-Type"] = "application/json";
      config.body =
        typeof options.body === "object"
          ? JSON.stringify(options.body)
          : options.body;
    } else {
      config.body = options.body;
    }
  }

  console.log("Fetching:", `${API}${path}`, config);

  const res = await fetch(`${API}${path}`, config);
  const data = await res.json().catch(() => ({}));

  // HTTP-level error
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
      case path.includes("/project-applications"):
        userMessage = "Application submission failed. Please check your data.";
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
    error.response = data;
    throw error;
  }

  // BE-level error (logic layer)
  const code = data?.responseStatus?.responseCode?.trim?.()?.toUpperCase?.();
const message = data?.responseStatus?.responseMessage?.trim?.()?.toUpperCase?.();

// // chấp nhận nhiều chuẩn success khác nhau
// const successCodes = ["200", "00000000", "SUCCESS"];
// const isSuccess =
//   successCodes.includes(code) || message === "SUCCESS";

const isSuccess =
  !data?.responseStatus ||               // không có responseStatus => bỏ qua check
  ["200", "00000000", "SUCCESS", "OK"].includes(code) ||
  ["SUCCESS", "OK"].includes(message);
  
if (!isSuccess) {
  const errMsg =
    data?.responseStatus?.responseMessage ||
    "Server returned a logical error.";
  const error = new Error(errMsg);
  error.status = res.status;
  error.response = data;
  throw error;
}

return data;
}