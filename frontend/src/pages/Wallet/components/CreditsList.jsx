import React from "react";
import { useNavigate } from "react-router-dom";
import PaginatedTable from "../../../components/Pagination/PaginatedTable";
import { Table } from "react-bootstrap";

export default function CreditsList({ credits = [] }) {
  const nav = useNavigate();

  if (!credits) {
    return <div>Loading...</div>;
  }

  // nếu phần tử có field "unitPrice" -> là Purchased Credits
  const isPurchased = credits[0]?.unitPrice !== undefined;

  return (
    <div className="credits-table glass-card mt-4 p-3">
      <h5 className="text-accent text-center fw-bold mb-3">
        {isPurchased ? "Purchased Credits" : "Issued Credits"}
      </h5>

      <Table className="table table-dark table-hover align-middle mb-0">
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
        <PaginatedTable
          items={credits}
          itemsPerPage={5}
          renderEmpty={() => (
            <p className="text-muted mb-0">No credit data available</p>
          )}
          renderRow={(item) =>
            isPurchased ? (
              <tr key={item.id}>
                <td>#{item.orderId}</td>
                <td>{item.description}</td>
                <td>{item.unitPrice} USD</td>
                <td>{item.quantity}</td>
                <td>{item.amount} USD</td>
                <td>{item.createdAt}</td>
              </tr>
            ) : (
              <tr key={item.id}>
                <td>{item.batchCode}</td>
                <td>{item.projectTitle}</td>
                <td>{item.totalTco2e}</td>
                <td>{item.creditsCount}</td>
                <td>
                  <span
                    className={`badge ${
                      item.status === "ISSUED" ? "bg-success" : "bg-secondary"
                    }`}
                  >
                    {item.status}
                  </span>
                </td>
                <td>{item.issuedAt}</td>
                <td>
                  <button
                    className="btn btn-outline-info btn-sm"
                    onClick={() => nav(`/wallet/credits/${item.id}`)}
                  >
                    View
                  </button>
                </td>
              </tr>
            )
          }
        />
      </Table>
    </div>
  );
}
