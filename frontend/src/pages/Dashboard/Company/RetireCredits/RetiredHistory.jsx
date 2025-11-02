import React, { useEffect, useState, useRef } from "react";
import { Card, Spinner, Table, Button, Form } from "react-bootstrap";
import { useNavigate } from "react-router-dom";
import useWalletData from "../../../Wallet/components/useWalletData";
import useReveal from "../../../../hooks/useReveal";
import PaginatedTable from "../../../../components/Pagination/PaginatedTable";
import { FaArrowLeft } from "react-icons/fa";

export default function RetiredHistory() {
  const { fetchRetiredCredits, loading } = useWalletData();
  const [credits, setCredits] = useState([]);
  const [search, setSearch] = useState("");
  const [filtered, setFiltered] = useState([]);
  const nav = useNavigate();
  const sectionRef = useRef(null);
  useReveal(sectionRef);

  useEffect(() => {
    loadRetired();
  }, []);

  const loadRetired = async () => {
    const data = await fetchRetiredCredits();
    setCredits(data || []);
    setFiltered(data || []);
  };

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
    <div
      ref={sectionRef}
      className="auth-hero min-vh-100 d-flex flex-column align-items-center justify-content-start py-5 reveal"
    >
      <Button
        variant="outline-info"
        size="sm"
        className="position-fixed top-0 start-0 m-3 px-3 py-2 d-flex align-items-center gap-2 fw-semibold shadow-sm"
        style={{
          borderRadius: "10px",
          background: "rgba(255, 255, 255, 0.85)",
          backdropFilter: "blur(6px)",
          zIndex: 20,
        }}
        onClick={() => nav("/retire")}
      >
        <FaArrowLeft /> Back
      </Button>

      <div
        className="container"
        style={{ maxWidth: "1100px", marginTop: "4rem" }}
      >
        {/* --- Header --- */}
        <div className="d-flex justify-content-between align-items-center mb-5">
          <h2 className="fw-bold text-white mb-0 text-shadow">
            My Retired Carbon Credits
          </h2>
          <div className="d-flex gap-3 align-items-center">
            <Form.Control
              type="text"
              placeholder="Search..."
              value={search}
              onChange={handleSearch}
              className="form-control-sm shadow-sm"
              style={{ width: "220px", borderRadius: "8px" }}
            />
          </div>
        </div>

        {/* --- Table Section --- */}
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
          ) : !filtered.length ? (
            <div className="text-center py-5">
              <h5>No retired credits found.</h5>
              <p className="text-muted mb-0">
                Once you retire credits, they’ll appear here.
              </p>
            </div>
          ) : (
            <Table hover responsive className="align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>Credit Code</th>
                  <th>Project</th>
                  <th>Vintage Year</th>
                  <th>Status</th>
                  <th>Issued At</th>
                </tr>
              </thead>

              <PaginatedTable
                items={filtered}
                itemsPerPage={6}
                renderRow={(c) => (
                  <tr key={c.id}>
                    <td>{c.creditCode}</td>
                    <td>{c.projectTitle}</td>
                    <td>{c.vintageYear}</td>
                    <td>
                      <span
                        className={`badge ${
                          c.status === "RETIRED"
                            ? "bg-secondary"
                            : "bg-light text-dark"
                        }`}
                      >
                        {c.status}
                      </span>
                    </td>
                    <td>{c.issuedAt}</td>
                  </tr>
                )}
              />
            </Table>
          )}
        </Card>
      </div>
    </div>
  );
}
