import React from "react";
import { useNavigate } from "react-router-dom";

export default function CreditsList({ credits = [] }) {
  const nav = useNavigate();

  if (!credits?.length)
    return (
      <div className="credits-table glass-card mt-4 p-3 text-center text-light">
        No credit batches found
      </div>
    );

  // nếu phần tử có field "unitPrice" -> là Purchased Credits
  const isPurchased = credits[0]?.unitPrice !== undefined;

  return (
    <div className="credits-table glass-card mt-4 p-3">
      <h5 className="text-accent text-center fw-bold mb-3">
        {isPurchased ? "Purchased Credits" : "Issued Credits"}
      </h5>

      <table className="table table-dark table-hover align-middle mb-0">
        <thead>
          <tr className="text-accent text-uppercase small">
            {isPurchased ? (
              <>
                <th>Order ID</th>
                <th>Description</th>
                <th>Unit Price</th>
                <th>Quantity</th>
                <th>Total Amount</th>
                <th>Purchased At</th>
              </>
            ) : (
              <>
                <th>Batch Code</th>
                <th>Project</th>
                <th>Total tCO₂e</th>
                <th>Credits Count</th>
                <th>Status</th>
                <th>Issued At</th>
                <th></th>
              </>
            )}
          </tr>
        </thead>
        <tbody>
          {isPurchased
            ? credits.map((p) => (
                <tr key={p.id}>
                  <td>#{p.orderId}</td>
                  <td>{p.description}</td>
                  <td>{p.unitPrice} USD</td>
                  <td>{p.quantity}</td>
                  <td>{p.amount} USD</td>
                  <td>{p.createdAt}</td>
                </tr>
              ))
            : credits.map((b) => (
                <tr key={b.id}>
                  <td>{b.batchCode}</td>
                  <td>{b.projectTitle}</td>
                  <td>{b.totalTco2e}</td>
                  <td>{b.creditsCount}</td>
                  <td>
                    <span
                      className={`badge ${
                        b.status === "ISSUED" ? "bg-success" : "bg-secondary"
                      }`}
                    >
                      {b.status}
                    </span>
                  </td>
                  <td>{b.issuedAt}</td>
                  <td>
                    <button
                      className="btn btn-outline-info btn-sm"
                      onClick={() => nav(`/wallet/credits/${b.id}`)}
                    >
                      View
                    </button>
                  </td>
                </tr>
              ))}
        </tbody>
      </table>
    </div>
  );
}
