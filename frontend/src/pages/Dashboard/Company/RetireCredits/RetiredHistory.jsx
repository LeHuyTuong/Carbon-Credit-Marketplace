import React, { useEffect, useState } from "react";
import useWalletData from "../../../Wallet/components/useWalletData";

export default function RetiredHistory() {
  const { fetchRetiredCredits, loading } = useWalletData();
  const [credits, setCredits] = useState([]);
  const [search, setSearch] = useState("");
  const [filtered, setFiltered] = useState([]);

  useEffect(() => {
    loadRetired();
  }, []);

  const loadRetired = async () => {
    const data = await fetchRetiredCredits();
    setCredits(data);
    setFiltered(data);
  };

  //lọc nhanh theo mã tín chỉ hoặc tên dự án
  const handleSearch = (e) => {
    const val = e.target.value.toLowerCase();
    setSearch(val);
    setFiltered(
      credits.filter(
        (c) =>
          c.creditCode.toLowerCase().includes(val) ||
          c.projectTitle.toLowerCase().includes(val)
      )
    );
  };

  return (
    <div className="auth-hero2 py-5 d-flex flex-column align-items-center">
      <div className="glass-card p-4 w-75">
        <div className="d-flex justify-content-between align-items-center mb-3">
          <h4 className="text-accent fw-bold mb-0">Retired Credits</h4>
          <input
            type="text"
            className="form-control form-control-sm w-25"
            placeholder="Search..."
            value={search}
            onChange={handleSearch}
          />
        </div>

        {loading ? (
          <div className="text-light text-center">Loading...</div>
        ) : !filtered.length ? (
          <div className="text-light text-center">
            No retired credits found.
          </div>
        ) : (
          <table className="table table-dark table-hover align-middle mb-0">
            <thead>
              <tr className="text-accent text-uppercase small">
                <th>Credit Code</th>
                <th>Project</th>
                <th>Vintage Year</th>
                <th>Status</th>
                <th>Issued At</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((c) => (
                <tr key={c.id}>
                  <td>{c.creditCode}</td>
                  <td>{c.projectTitle}</td>
                  <td>{c.vintageYear}</td>
                  <td>
                    <span className="badge bg-secondary">{c.status}</span>
                  </td>
                  <td>{c.issuedAt}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
