import { useEffect, useState, useRef } from "react";
import { Button, Table, Spinner, Card } from "react-bootstrap";
import { toast } from "react-toastify";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../../../context/AuthContext";
import { apiFetch } from "../../../../utils/apiFetch";
import { FaArrowLeft } from "react-icons/fa";
import useReveal from "../../../../hooks/useReveal";
import PaginatedTable from "../../../../components/Pagination/PaginatedTable";

export default function ListProjects() {
  const { user } = useAuth();
  const nav = useNavigate();
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [requestDateTime, setRequestDateTime] = useState(null); //lưu thời gian response
  const sectionRef = useRef(null);
  useReveal(sectionRef);

  const fetchMyApplications = async () => {
    setLoading(true);
    try {
      const res = await apiFetch("/api/v1/project-applications/my", {
        method: "GET",
      });

      const code =
        res?.responseStatus?.responseCode?.trim?.().toUpperCase?.() || "";

      if (code !== "SUCCESS" && code !== "00000000") {
        throw new Error(
          res?.responseStatus?.responseMessage ||
            "Failed to fetch project applications"
        );
      }

      const data = res?.response || [];
      setApplications(data);
      setRequestDateTime(res?.requestDateTime || new Date().toISOString()); //lưu request time
    } catch (err) {
      console.error("Error fetching project applications:", err);
      toast.error("Failed to load registered projects");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMyApplications();
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
        onClick={() => nav("/home")}
      >
        <FaArrowLeft /> Back to Home
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
            Your Projects Registered
          </h2>
          <Button
            onClick={() => nav("/home#register")}
            className="fw-semibold px-4"
            style={{
              background: "#28a745",
              border: "none",
              borderRadius: "8px",
              boxShadow: "0 2px 8px rgba(0,0,0,0.2)",
            }}
          >
            + Register New Project
          </Button>
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
              <h5>No registered projects yet</h5>
              <p className="text-muted mb-0">
                Click “+ Register New Project” to submit one.
              </p>
            </div>
          ) : (
            <Table hover responsive className="align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>#</th>
                  <th>Project Title</th>
                  <th>Status</th>
                  <th>Submitted At</th>
                  <th>Review Note</th>
                  <th>Action</th>
                </tr>
              </thead>
              <PaginatedTable
                items={applications}
                itemsPerPage={5}
                renderRow={(a, index) => (
                  <tr key={a.id}>
                    <td className="fw-semibold">{index + 1}</td>
                    <td className="fw-semibold">{a.projectTitle}</td>
                    <td>
                      <span
                        className={`badge text-light px-3 py-2 ${
                          a.status === "ADMIN_APPROVED"
                            ? "bg-success"
                            : a.status.includes("REJECTED")
                            ? "bg-danger"
                            : a.status === "UNDER_REVIEW"
                            ? "bg-warning text-dark"
                            : "bg-secondary"
                        }`}
                        style={{ fontSize: "0.8rem" }}
                      >
                        {a.status.replaceAll("_", " ")}
                      </span>
                    </td>

                    <td className="text-muted">
                      {a.submittedAt
                        ? new Date(a.submittedAt).toLocaleString("vi-VN", {
                            timeZone: "Asia/Ho_Chi_Minh",
                            hour12: false,
                          })
                        : "—"}
                    </td>

                    <td className="text-muted">
                      {a.reviewNote && a.reviewNote.trim() !== ""
                        ? a.reviewNote
                        : "-"}
                    </td>
                    <td>
                      <Button
                        size="sm"
                        variant="outline-primary"
                        onClick={() => handleView(a.id)}
                        className="fw-semibold rounded-3"
                      >
                        View Project
                      </Button>
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
