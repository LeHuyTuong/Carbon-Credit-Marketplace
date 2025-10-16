import { useEffect, useState } from "react";
import { apiFetch } from "../utils/apiFetch";

export function useCompanyProfile() {
  const [company, setCompany] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchCompany = async () => {
      setLoading(true);
      try {
        const data = await apiFetch("/api/v1/kyc/company", { method: "GET" });
        const info = data.response;

        if (!info) {
          setCompany(null);
          return;
        }

        setCompany({
          businessLicense: info.businessLicense || "",
          taxCode: info.taxCode || "",
          companyName: info.companyName || "",
          address: info.address || "",
        });
      } catch (err) {
        console.error("Error fetching company:", err);
        if (err.status === 404 || err.status === 400) {
          setCompany(null);
        } else {
          setError(err.message || "Failed to fetch company info");
        }
      } finally {
        setLoading(false);
      }
    };

    fetchCompany();
  }, []);

  return { company, loading, error };
}
