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

    const txList = walletData.walletTransactions || [];
    const carbonCredits = walletData.carbonCredits || [];

    const purchases = txList
      .filter((tx) => tx.transactionType === "BUY_CARBON_CREDIT");

    // dữ liệu transaction ra danh sách hiển thị
    const mapped = purchases.map((tx) => {
      //tìm credit liên quan nếu cần thông tin thêm
      const relatedCredit = carbonCredits.find(
        (c) => c.batchCode === tx.batchCode
      );

      return {
        id: tx.id,
        orderId: tx.orderId,
        description: tx.description || "Carbon credit purchase",
        unitPrice: tx.unitPrice || 0,
        amount: tx.amount || 0,
        quantity: tx.carbonCreditQuantity || 0,
        balanceBefore: tx.balanceBefore || 0,
        balanceAfter: tx.balanceAfter || 0,
        creditStatus: relatedCredit?.status || null,
        createdAt: new Date(tx.createdAt).toLocaleString("vi-VN", {
          timeZone: "Asia/Ho_Chi_Minh",
          hour12: false,
        }),
      };
    });

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
      const res = await apiFetch(`/api/v1/my/credits/batch/${batchId}`, {
        method: "GET",
      });

      // Nếu API trả về 1 object, thì wrap lại trong mảng
      const data = res?.response;
      setCreditDetails(data);
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

  // === FETCH ALL MY CREDITS (tổng hợp) ===
const fetchAllCredits = async (filters = {}) => {
  try {
    setLoading(true);
    const params = new URLSearchParams({
      projectId: filters.projectId || "",
      status: filters.status || "",
      vintageYear: filters.vintageYear || "",
      page: filters.page || 0,
      size: filters.size || 20,
    }).toString();

    const res = await apiFetch(`/api/v1/my/credits?${params}`, { method: "GET" });
    const list = res?.response?.content || [];

    const mapped = list.map((c) => ({
      id: c.id,
      creditCode: c.creditCode,
      status: c.status,
      projectId: c.projectId,
      projectTitle: c.projectTitle,
      companyId: c.companyId,
      companyName: c.companyName,
      vintageYear: c.vintageYear,
      batchCode: c.batchCode,
      issuedAt: c.issuedAt
        ? new Date(c.issuedAt).toLocaleString("vi-VN", {
            timeZone: "Asia/Ho_Chi_Minh",
            hour12: false,
          })
        : "-",
      expiryDate: c.expiryDate,
      availableAmount: c.availableAmount,
      listedAmount: c.listedAmount,
    }));

    return mapped;
  } catch (err) {
    console.error("Failed to fetch credits:", err);
    return [];
  } finally {
    setLoading(false);
  }
};

// === FETCH RETIRABLE CREDITS ===
const fetchRetirableCredits = async () => {
  try {
    setLoading(true);
    const res = await apiFetch("/api/v1/my/credits/retirable", { method: "GET" });
    const list = res?.response || [];

    // chỉ lấy AVAILABLE
    const availableList = list.filter(c => c.status === "AVAILABLE");

    // nhóm theo batchCode
    const grouped = Object.values(
      availableList.reduce((acc, c) => {
        if (!acc[c.batchCode]) {
          acc[c.batchCode] = {
            batchCode: c.batchCode,
            projectTitle: c.projectTitle,
            vintageYear: c.vintageYear,
            projectId: c.projectId,
            availableAmount: 0,
            expiryDate: c.expiryDate,
            issuedAt: c.issuedAt,
            creditIds: [],
          };
        }
        acc[c.batchCode].availableAmount += c.availableAmount || 0;
        acc[c.batchCode].creditIds.push(c.id);
        return acc;
      }, {})
    );

    return grouped;
  } catch (err) {
    console.error("Failed to fetch retirable credits:", err);
    return [];
  } finally {
    setLoading(false);
  }
};



// === RETIRE CREDIT (theo từng creditId) ===
const retireCredits = async (retireList = []) => {
  if (!retireList.length) throw new Error("No credits selected to retire.");

  try {
    setLoading(true);
    const results = [];

    for (const item of retireList) {
      const { creditIds = [], quantity } = item;

      // Chỉ lấy đúng số lượng cần retire
      const idsToRetire = creditIds.slice(0, quantity);

      if (!idsToRetire.length) continue;

      for (const id of idsToRetire) {
        console.log(`➡ Retiring creditId: ${id}`);
        const res = await apiFetch(`/api/v1/my/credits/${id}/retire`, {
          method: "POST",
          body: { creditId: id, quantity: 1 },
        });
        results.push(res?.response);
      }
    }

    return results;
  } catch (err) {
    console.error("Failed to retire credits:", err);
    throw new Error(err.message || "Retire credits failed.");
  } finally {
    setLoading(false);
  }
};



  // === FETCH RETIRED CREDITS===
  const fetchRetiredCredits = async () => {
  try {
    setLoading(true);
    const res = await apiFetch("/api/v1/my/credits", { method: "GET" });
    const list = res?.response?.content || res?.response || [];

    // lọc những cái đã retire
    return list
      .filter((c) => c.status === "RETIRED")
      .map((c) => ({
        id: c.id,
        creditCode: c.creditCode || "-",
        projectTitle: c.projectTitle || "-",
        vintageYear: c.vintageYear || "-",
        status: c.status || "-",
        issuedAt: c.issuedAt
          ? new Date(c.issuedAt).toLocaleDateString("en-GB")
          : "-",
      }));
  } catch (err) {
    console.error("Failed to fetch retired credits:", err);
    return [];
  } finally {
    setLoading(false);
  }
};

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
    fetchAllCredits,
    fetchRetirableCredits,
    retireCredits,
    fetchRetiredCredits,
  };
}
