import React from "react";
import "../wallet.css";

export default function CreditsList({ credits = [] }) {
  return (
    <div className="credits-table glass-card mt-5 p-3">
      <h5 className="text-accent mb-3 text-center fw-bold">My Credits</h5>

      <div className="table-responsive">
        {/* Nếu không có credits, render thông báo */}
        {!credits.length ? (
          <div className="credits-empty text-light mt-4 text-center">
            No credits available
          </div>
        ) : (
          <table className="table table-dark table-hover align-middle mb-0">
            <thead>
              <tr className="text-accent text-uppercase small">
                <th scope="col">Credit Code</th>
                <th scope="col">Title</th>
                <th scope="col">Price ($)</th>
                <th scope="col">Sold</th>
                <th scope="col">Remaining</th>
                <th scope="col">Status</th>
                <th scope="col">Expires At</th>
              </tr>
            </thead>
            <tbody>
              {credits.map((c, i) => {
                const sold = c.sold || 0;
                const remaining =
                  c.remaining ?? Math.max((c.quantity || 0) - (c.sold || 0), 0);

                return (
                  <tr key={i}>
                    <td>{c.creditCode || c.id}</td>
                    <td>{c.title}</td>
                    <td>${c.price?.toLocaleString()}</td>
                    <td>{sold.toLocaleString()}</td>
                    <td>{remaining.toLocaleString()}</td>
                    <td>
                      <span
                        className={`badge px-3 py-2 ${
                          c.status === "active"
                            ? "bg-success"
                            : c.status === "expired"
                            ? "bg-secondary"
                            : "bg-warning text-dark"
                        }`}
                      >
                        {c.status}
                      </span>
                    </td>
                    <td>
                      {c.expiresAt
                        ? new Date(c.expiresAt).toLocaleDateString()
                        : "N/A"}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
