import { useState, useEffect } from "react";
import { apiFetch } from "../../../utils/apiFetch";

export default function useWalletData() {
  const [wallet, setWallet] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [issuedCredits, setIssuedCredits] = useState([]);
  const [purchasedCredits, setPurchasedCredits] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [creditDetails, setCreditDetails] = useState([]);

  // === FETCH WALLET ===
  const fetchWallet = async () => {
    try {
      const res = await apiFetch("/api/v1/wallet", { method: "GET" });
      setWallet(res.response);
    } catch (err) {
      console.error("Failed to fetch wallet:", err);
    }
  };

  // === FETCH TRANSACTIONS ===
  const fetchTransactions = async () => {
    try {
      const res = await apiFetch("/api/v1/wallet/transactions", {
        method: "GET",
      });
      const sorted = [...(res.response || [])].sort(
        (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
      );
      setTransactions(sorted);
    } catch (err) {
      console.error("Failed to fetch transactions:", err);
    }
  };

  // === FETCH ISSUED CREDITS ===
  const fetchIssuedCredits = async (filters = {}) => {
    try {
      setLoading(true);
      const query = new URLSearchParams({
        page: filters.page || 0,
        size: filters.size || 10,
      }).toString();

      const res = await apiFetch(`/api/v1/my/credits/batches?${query}`, {
        method: "GET",
      });

      const list = res?.response?.content || [];

      const mapped = list.map((item) => ({
        id: item.id,
        batchCode: item.batchCode,
        projectTitle: item.projectTitle,
        creditsCount: item.creditsCount,
        totalTco2e: item.totalTco2e,
        residualTco2e: item.residualTco2e,
        status: item.status,
        issuedAt: item.issuedAt
          ? new Date(item.issuedAt).toLocaleString("vi-VN", {
              timeZone: "Asia/Ho_Chi_Minh",
              hour12: false,
            })
          : "Not Issued",
      }));

      setIssuedCredits(mapped);
    } catch (err) {
      console.error("Failed to fetch issued credits:", err);
    } finally {
      setLoading(false);
    }
  };

  // === FETCH PURCHASED CREDITS ===
  const fetchPurchasedCredits = async () => {
    try {
      setLoading(true);
      const res = await apiFetch("/api/v1/wallet", { method: "GET" });
      const walletData = res?.response || {};

      // Dữ liệu giao dịch nằm trong walletTransactions
      const txList = walletData.walletTransactions || [];

      // Lọc các giao dịch BUY_CARBON_CREDIT
      const purchases = txList.filter(
        (tx) => tx.transactionType === "BUY_CARBON_CREDIT"
      );

      const mapped = purchases.map((tx) => ({
        id: tx.id,
        orderId: tx.orderId,
        description: tx.description || "Carbon credit purchase",
        unitPrice: tx.unitPrice || 0,
        amount: tx.amount || 0,
        quantity: tx.carbonCreditQuantity || 0,
        createdAt: new Date(tx.createdAt).toLocaleString("vi-VN", {
          timeZone: "Asia/Ho_Chi_Minh",
          hour12: false,
        }),
      }));

      setPurchasedCredits(mapped);
    } catch (err) {
      console.error("Failed to fetch purchased credits:", err);
      setPurchasedCredits([]);
    } finally {
      setLoading(false);
    }
  };

  // === FETCH MY CREDITS (theo batchId) ===
  const fetchMyCredits = async (batchId) => {
    try {
      setLoading(true);
      const res = await apiFetch(`/api/v1/my/credits/${batchId}`, {
        method: "GET",
      });

      // Nếu API trả về 1 object, thì wrap lại trong mảng
      const data = res?.response;
      if (Array.isArray(data)) {
        setCreditDetails(data);
      } else if (data) {
        setCreditDetails([data]);
      } else {
        setCreditDetails([]);
      }
    } catch (err) {
      console.error("Failed to fetch credit details:", err);
      setCreditDetails([]);
      setError(err.message || "Unable to load credit details.");
    } finally {
      setLoading(false);
    }
  };

  // === FETCH SUMMARY ===
  const fetchSummary = async () => {
    try {
      const res = await apiFetch("/api/v1/my/credits/summary", {
        method: "GET",
      });
      setSummary(res?.response || {});
    } catch (err) {
      console.error("Failed to fetch summary:", err);
    }
  };

  // === FETCH CREDIT BALANCE ===
  const fetchCreditBalance = async () => {
    try {
      const res = await apiFetch("/api/v1/my/credits/balance", { method: "GET" });
      return res?.response || 0; // có thể set vào state nếu muốn
    } catch (err) {
      console.error("Failed to fetch credit balance:", err);
      return 0;
    }
  };

  // === FETCH CREDIT BY ID ===
  const fetchCreditById = async (id) => {
    try {
      const res = await apiFetch(`/api/v1/my/credits/${id}`, { method: "GET" });
      return res?.response || {};
    } catch (err) {
      console.error(`Failed to fetch credit #${id}:`, err);
      return null;
    }
  };


  useEffect(() => {
    fetchWallet();
  }, []);

  return {
    wallet,
    transactions,
    issuedCredits,
    purchasedCredits,
    summary,
    loading,
    error,
    creditDetails,
    setLoading,
    fetchWallet,
    fetchTransactions,
    fetchIssuedCredits,
    fetchPurchasedCredits,
    fetchSummary,
    fetchCreditBalance,
    fetchCreditById,
    fetchMyCredits,
  };
}
