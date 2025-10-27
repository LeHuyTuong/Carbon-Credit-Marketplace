import React, { useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import useWalletData from "./useWalletData";

export default function CreditBatchDetail() {
  const { id } = useParams();
  const nav = useNavigate();
  const { creditDetails, fetchMyCredits, loading } = useWalletData();

  useEffect(() => {
    if (id) fetchMyCredits(id);
  }, [id]);

  return (
    <div className="glass-card mt-4 p-3">
      <button
        className="btn btn-outline-light btn-sm mb-3"
        onClick={() => nav(-1)}
      >
        ← Back
      </button>
      <h5 className="text-accent text-center fw-bold mb-3">
        Credit Details (Batch #{id})
      </h5>
      {loading ? (
        <div className="text-center text-light">Loading...</div>
      ) : !creditDetails.length ? (
        <div className="text-light text-center">
          No credits found for this batch
        </div>
      ) : (
        <table className="table table-dark table-hover align-middle mb-0">
          <thead>
            <tr className="text-accent text-uppercase small">
              <th>Credit Code</th>
              <th>Project</th>
              <th>Company</th>
              <th>Status</th>
              <th>Issued At</th>
            </tr>
          </thead>
          <tbody>
            {creditDetails.map((c) => (
              <tr key={c.id}>
                <td>{c.creditCode}</td>
                <td>{c.projectTitle}</td>
                <td>{c.companyName}</td>
                <td>
                  <span
                    className={`badge ${
                      c.status === "AVAILABLE"
                        ? "bg-success"
                        : "bg-warning text-dark"
                    }`}
                  >
                    {c.status}
                  </span>
                </td>
                <td>{c.issuedAt}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
