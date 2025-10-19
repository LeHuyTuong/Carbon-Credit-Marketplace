import { useEffect, useState, useRef } from "react";
import { Button, Table, Spinner, Card } from "react-bootstrap";
import { toast } from "react-toastify";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../../../context/AuthContext";
import { apiFetch } from "../../../../utils/apiFetch";
import { FaArrowLeft } from "react-icons/fa";
import useReveal from "../../../../hooks/useReveal";

export default function PurchaseHistory() {
  const { user } = useAuth();
  const nav = useNavigate();
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [requestDateTime, setRequestDateTime] = useState(null); //lưu thời gian response
  const sectionRef = useRef(null);
  useReveal(sectionRef);

  useEffect(() => {
    setLoading(true);
    const stored = JSON.parse(localStorage.getItem("purchases") || "[]");
    setApplications(stored);
    setLoading(false);
  }, []);

  const handleView = (applicationId) => {
    nav(`/view-registered-project/${applicationId}`);
  };

  return (
    <div
      ref={sectionRef}
      className="auth-hero min-vh-100 d-flex flex-column align-items-center justify-content-start py-5 reveal"
    >
      {/*nút Back to Home cố định góc trên trái */}
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
        onClick={() => nav("/marketplace")}
      >
        <FaArrowLeft /> Back to Marketplace
      </Button>

      <div
        className="container"
        style={{
          maxWidth: "1100px",
          marginTop: "4rem",
        }}
      >
        <div className="d-flex justify-content-between align-items-center mb-5">
          <h2 className="fw-bold text-white mb-0 text-shadow">
            Your Credits Purchased
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
          ) : applications.length === 0 ? (
            <div className="text-center py-5">
              <h5>No credits purchased yet</h5>
              <p className="text-muted mb-0">Buy credits in Marketplace.</p>
            </div>
          ) : (
            <Table hover responsive className="align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>#</th>
                  <th>Credits Name</th>
                  <th>Quantity (tCO₂)</th>
                  <th>Total ($)</th>
                  <th>Beneficiary</th>
                  <th>Purchased At</th>
                </tr>
              </thead>
              <tbody>
                {applications.map((a, index) => (
                  <tr key={index}>
                    <td>{index + 1}</td>
                    <td>{a.title}</td>
                    <td>{a.quantity}</td>
                    <td>${a.total.toFixed(2)}</td>
                    <td>{a.beneficiaryName}</td>
                    <td>
                      {new Date(a.purchasedAt).toLocaleString("en-GB", {
                        day: "2-digit",
                        month: "2-digit",
                        year: "numeric",
                        hour: "2-digit",
                        minute: "2-digit",
                        second: "2-digit",
                      })}
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </Card>
      </div>
    </div>
  );
}
