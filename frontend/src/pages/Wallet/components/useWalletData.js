import { useState, useEffect } from "react";
import { apiFetch } from "../../../utils/apiFetch";

//hook quản lý toàn bộ dữ liệu của ví (wallet & transactions)
export default function useWalletData() {
  const [wallet, setWallet] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(false);

  //lấy thông tin ví hiện tại của user (balance, carbonCreditBalance, ...)
  const fetchWallet = async () => {
    try {
      const res = await apiFetch("/api/v1/wallet", { method: "GET" });
      setWallet(res.response);
    } catch (err) {
      console.error("Failed to fetch wallet:", err);
    }
  };

   //lấy lịch sử giao dịch ví
  const fetchTransactions = async () => {
    try {
      const res = await apiFetch("/api/v1/wallet/transactions", {
        method: "GET",
      });
      //sắp xếp các giao dịch theo thời gian (mới nhất trước)
      const sorted = [...(res.response || [])].sort(
        (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
      );
      setTransactions(sorted);
    } catch (err) {
      console.error("Failed to fetch transactions:", err);
    }
  };

  //khi component lần đầu render -> tự động gọi fetchWallet
  useEffect(() => {
    fetchWallet();
  }, []);

  //trả ra tất cả dữ liệu và hàm helper để component khác sử dụng
  return {
    wallet,
    transactions,
    loading,
    setLoading,
    fetchWallet,
    fetchTransactions,
  };
}
