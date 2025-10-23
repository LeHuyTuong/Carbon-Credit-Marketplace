import { useEffect, useState, useRef } from "react";
import { Button, Table, Spinner, Card } from "react-bootstrap";
import { FaArrowLeft } from "react-icons/fa";
import { useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../../../../context/AuthContext";
import { apiFetch } from "../../../../utils/apiFetch";
import useReveal from "../../../../hooks/useReveal";
import PaginatedTable from "../../../../components/Pagination/PaginatedTable";

export default function PurchaseHistory() {
  const { user } = useAuth();
  const nav = useNavigate();
  const sectionRef = useRef(null);
  const { state } = useLocation();
  const from = state?.from || "marketplace";

  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useReveal(sectionRef);

  useEffect(() => {
    const fetchOrders = async () => {
      try {
        setLoading(true);
        const res = await apiFetch("/api/v1/orders", { method: "GET" });
        const list = res?.response || [];
        setOrders(
          list.map((o) => ({
            id: o.id,
            companyId: o.companyId,
            status: o.status,
            totalAmount: o.totalAmount,
            createdAt: o.createAt,
          }))
        );
      } catch (err) {
        setError(err.message || "Unable to load orders.");
      } finally {
        setLoading(false);
      }
    };
    fetchOrders();
  }, []);

  const handleBack = () => {
    if (from === "wallet") nav("/wallet");
    else if (from === "order") nav("/marketplace");
    else nav(-1);
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
        onClick={handleBack}
      >
        <FaArrowLeft /> Back
      </Button>

      <div
        className="container"
        style={{ maxWidth: "1100px", marginTop: "4rem" }}
      >
        <div className="d-flex justify-content-between align-items-center mb-5">
          <h2 className="fw-bold text-white mb-0 text-shadow">
            Your Purchases History
          </h2>
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
          ) : error ? (
            <div className="text-center py-5 text-danger">
              <h5>{error}</h5>
            </div>
          ) : orders.length === 0 ? (
            <div className="text-center py-5">
              <h5>No purchases history found</h5>
              <p className="text-muted mb-0">Buy credits in Marketplace.</p>
            </div>
          ) : (
            <Table hover responsive className="align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>#</th>
                  <th>Order ID</th>
                  <th>Status</th>
                  <th>Total Amount ($)</th>
                  <th>Created At</th>
                </tr>
              </thead>
              <PaginatedTable
                items={orders}
                itemsPerPage={5}
                renderRow={(o, index) => (
                  <tr key={o.id || index}>
                    <td>{index + 1}</td>
                    <td>{o.id}</td>
                    <td>
                      <span
                        className={`badge bg-${
                          o.status === "SUCCESS"
                            ? "success"
                            : o.status === "PENDING"
                            ? "warning"
                            : "secondary"
                        }`}
                      >
                        {o.status}
                      </span>
                    </td>
                    <td>${o.totalAmount?.toLocaleString() || 0}</td>
                    <td>
                      {o.createdAt
                        ? new Date(o.createdAt).toLocaleString("vi-VN", {
                            timeZone: "Asia/Ho_Chi_Minh",
                            hour12: false,
                          })
                        : "—"}
                    </td>
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
