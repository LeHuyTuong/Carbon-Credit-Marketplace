import React, { useEffect, useState, useRef } from "react";
import { Button, Card, Spinner, Table } from "react-bootstrap";
import { FaArrowLeft } from "react-icons/fa";
import useWalletData from "../Wallet/components/useWalletData";
import { useNavigate } from "react-router-dom";
import useReveal from "../../hooks/useReveal";
import PaginatedTable from "../../components/Pagination/PaginatedTable";

export default function RetireCredits() {
  const { fetchAllCredits, retireCredits, loading } = useWalletData();
  const [credits, setCredits] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [toast, setToast] = useState({ show: false, msg: "", type: "" });
  const [statusFilter, setStatusFilter] = useState("");
  const nav = useNavigate();
  const sectionRef = useRef(null);
  useReveal(sectionRef);

  const loadCredits = async () => {
    const data = await fetchAllCredits(
      statusFilter ? { status: statusFilter } : {}
    );
    setCredits(data || []);
  };

  useEffect(() => {
    loadCredits();
  }, [statusFilter]);

  const handleSelect = (id) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    );
  };

  const handleRetire = async () => {
    try {
      await retireCredits(selectedIds);
      setToast({
        show: true,
        msg: "Credits retired successfully!",
        type: "success",
      });
      setSelectedIds([]);
      loadCredits();
    } catch (err) {
      setToast({
        show: true,
        msg: err.message || "Failed to retire credits",
        type: "danger",
      });
    }
  };

  return (
    <div
      ref={sectionRef}
      className="auth-hero min-vh-100 d-flex flex-column align-items-center justify-content-start py-5 reveal"
    >
      <div
        className="container"
        style={{
          maxWidth: "1100px",
          marginTop: "4rem",
        }}
      >
        <div className="d-flex justify-content-between align-items-center mb-5">
          <h2 className="fw-bold text-white mb-0 text-shadow">
            Retire My Carbon Credits
          </h2>
          <select
            className="form-select form-select-sm w-auto fw-semibold shadow-sm"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            style={{
              borderRadius: "8px",
              border: "none",
            }}
          >
            <option value="">All</option>
            <option value="AVAILABLE">Available</option>
            <option value="ISSUED">Issued</option>
            <option value="SOLD">Sold</option>
            <option value="RETIRED">Retired</option>
          </select>
        </div>

        <Card
          className="shadow-lg border-0 p-3"
          style={{
            borderRadius: "15px",
            background: "rgba(255,255,255,0.9)",
            backdropFilter: "blur(10px)",
          }}
        >
          {loading ? (
            <div className="d-flex justify-content-center align-items-center py-5">
              <Spinner animation="border" />
            </div>
          ) : credits.length === 0 ? (
            <div className="text-center py-5">
              <h5>No available credits to retire.</h5>
              <p className="text-muted mb-0">
                Purchase or receive carbon credits before retiring them.
              </p>
            </div>
          ) : (
            <>
              <Table hover responsive className="align-middle mb-3">
                <thead className="table-light">
                  <tr>
                    <th></th>
                    <th>Credit Code</th>
                    <th>Project</th>
                    <th>Vintage Year</th>
                    <th>Status</th>
                    <th>Issued At</th>
                  </tr>
                </thead>

                <PaginatedTable
                  items={credits}
                  itemsPerPage={5}
                  renderRow={(c, index) => (
                    <tr key={c.id}>
                      <td>
                        <input
                          type="checkbox"
                          checked={selectedIds.includes(c.id)}
                          onChange={() => handleSelect(c.id)}
                        />
                      </td>
                      <td className="fw-semibold">{c.creditCode}</td>
                      <td>{c.projectTitle}</td>
                      <td>{c.vintageYear}</td>
                      <td>
                        <span
                          className={`badge text-light px-3 py-2 ${
                            c.status === "AVAILABLE"
                              ? "bg-success"
                              : c.status === "RETIRED"
                              ? "bg-secondary"
                              : "bg-warning text-dark"
                          }`}
                          style={{ fontSize: "0.8rem" }}
                        >
                          {c.status}
                        </span>
                      </td>
                      <td className="text-muted">{c.issuedAt}</td>
                    </tr>
                  )}
                />
              </Table>

              <div className="text-end">
                <Button
                  variant="success"
                  className="fw-semibold px-4 rounded-3"
                  disabled={!selectedIds.length || loading}
                  onClick={handleRetire}
                >
                  Retire Selected ({selectedIds.length})
                </Button>
              </div>
            </>
          )}
        </Card>
      </div>

      {toast.show && (
        <div
          className={`alert alert-${toast.type} position-fixed bottom-0 end-0 m-4 shadow`}
          style={{ minWidth: "300px", zIndex: 100 }}
        >
          {toast.msg}
        </div>
      )}
    </div>
  );
}
