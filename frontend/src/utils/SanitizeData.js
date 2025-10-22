// src/utils/SanitizeData.js

/**
 * Sanitize bar chart data to ensure Nivo doesn't crash.
 * - Convert numeric strings to numbers
 * - Replace NaN or invalid values with 0
 * - Replace negative values with 0 (optional: nếu bạn muốn giữ số âm thì bỏ đoạn check < 0)
 */
export function sanitizeData(data) {
  if (!Array.isArray(data)) return [];

  return data.map((item) => {
    const clean = { ...item };

    for (let key in clean) {
      // Bỏ qua field không phải giá trị số (ví dụ color, country)
      if (key.toLowerCase().includes("color") || key === "country") continue;

      let value = clean[key];

      // Nếu là string và parse được -> convert thành số
      if (typeof value === "string" && !isNaN(Number(value))) {
        value = Number(value);
      }

      // Nếu không phải số -> set 0
      if (typeof value !== "number" || isNaN(value)) {
        value = 0;
      }

      // Nếu âm thì set 0 (nếu bạn muốn giữ số âm thì comment dòng này)
      if (value < 0) {
        value = 0;
      }

      clean[key] = value;
    }

    return clean;
  });
}
