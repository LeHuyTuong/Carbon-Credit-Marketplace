const pickData = (j) => j?.response ?? j?.responseData ?? j?.data ?? j?.payload ?? null;

const commonHeaders = (token) => {
  const h = {
    "X-Request-Trace": `trace_${Date.now()}`,
    "X-Request-DateTime": new Date().toISOString(),
  };
  if (token) h.Authorization = `Bearer ${token}`;
  return h;
};

/** GET /api/cva/dashboard/cards */
export async function fetchCvaCards(token) {
  const res = await fetch("/api/cva/dashboard/cards", {
    method: "GET",
    headers: commonHeaders(token),
  });
  if (!res.ok) throw new Error(`cards: HTTP ${res.status}`);
  const json = await res.json();
  return pickData(json);
}

/** GET /api/cva/dashboard/reports/status */
export async function fetchMonthlyReportStatus(token) {
  const res = await fetch("/api/cva/dashboard/reports/status", {
    method: "GET",
    headers: commonHeaders(token),
  });
  if (!res.ok) throw new Error(`reports/status: HTTP ${res.status}`);
  const json = await res.json();
  return pickData(json) ?? [];
}

/** GET /api/cva/dashboard/credits/status/monthly */
export async function fetchMonthlyCreditStatus(token) {
  const res = await fetch("/api/cva/dashboard/credits/status/monthly", {
    method: "GET",
    headers: commonHeaders(token),
  });
  if (!res.ok) throw new Error(`credits/status/monthly: HTTP ${res.status}`);
  const json = await res.json();
  return pickData(json) ?? [];
}
/** GET /api/cva/dashboard/projects/status/monthly */
export async function fetchMonthlyApplicationStatus(token) {
  const res = await fetch("/api/cva/dashboard/projects/status/monthly", {
    method: "GET",
    headers: commonHeaders(token),
  });
  if (!res.ok) throw new Error(`projects/status/monthly: HTTP ${res.status}`);
  const json = await res.json();
  return pickData(json) ?? [];
}
/** GET /api/cva/dashboard/reports */
export async function fetchDashboardReport(token) {
  const res = await fetch("/api/cva/dashboard/reports", {
    method: "GET",
    headers: commonHeaders(token),
  });

  if (!res.ok) throw new Error(`dashboard/reports: HTTP ${res.status}`);

  const json = await res.json();
  
  // Trả về total nếu có, hoặc null nếu không
  return pickData(json)?.total ?? null;
}
