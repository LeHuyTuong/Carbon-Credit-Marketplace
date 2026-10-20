// Logger chỉ dùng ở môi trường dev.
//
// Bundle production không được in console.log: vừa rò rỉ response API vào console
// của người dùng, vừa làm nhiễu. Vite thay `import.meta.env.DEV` bằng false khi build
// production nên nhánh này bị loại khỏi bundle.
export function devLog(...args) {
  if (import.meta.env.DEV) {
    console.log(...args);
  }
}
