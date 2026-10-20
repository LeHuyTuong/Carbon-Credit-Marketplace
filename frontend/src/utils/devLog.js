// Logger chỉ dùng ở môi trường dev.
//
// Bundle production không được in console: vừa rò rỉ response API (có dữ liệu người
// dùng khác) vào console của người dùng, vừa làm nhiễu. Vite thay `import.meta.env.DEV`
// bằng false khi build production nên nhánh này bị loại khỏi bundle.
export function devLog(...args) {
  if (import.meta.env.DEV) {
    console.log(...args);
  }
}

export function devWarn(...args) {
  if (import.meta.env.DEV) {
    console.warn(...args);
  }
}

export function devError(...args) {
  if (import.meta.env.DEV) {
    console.error(...args);
  }
}
