import React from "react";
import "../wallet.css";

export default function CreditsList({ credits = [] }) {
  //nếu chưa có dữ liệu credit thì render thông báo trống
  if (!credits.length)
    return (
      <div className="credits-empty text-muted mt-4">No credits available</div>
    );

  return (
    <div className="credits-table glass-card mt-5 p-3">
      <h5 className="text-accent mb-3 text-center fw-bold">My Credits</h5>

      <div className="table-responsive">
        <table className="table table-dark table-hover align-middle mb-0">
          <thead>
            <tr className="text-accent text-uppercase small">
              <th scope="col">ID</th>
              <th scope="col">Title</th>
              <th scope="col">Price ($)</th>
              <th scope="col">Quantity</th>
              <th scope="col">Status</th>
              <th scope="col">Expires At</th>
            </tr>
          </thead>
          <tbody>
            {credits.map((c, i) => (
              <tr key={i}>
                <td>{c.id}</td>
                <td>{c.title}</td>
                <td>${c.price?.toLocaleString()}</td>
                <td>{c.quantity?.toLocaleString()}</td>
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
                <td>{c.expiresAt}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
