import { useEffect, useState, useRef } from "react";
import { Button, Table, Spinner, Card } from "react-bootstrap";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { apiFetch } from "../../../../utils/apiFetch";
import { FaArrowLeft } from "react-icons/fa";
import useReveal from "../../../../hooks/useReveal";

export default function ChooseProjectToUpload() {
  const [approvedProjects, setApprovedProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const sectionRef = useRef(null);
  const nav = useNavigate();
  useReveal(sectionRef);

  const fetchApprovedProjects = async () => {
    try {
      setLoading(true);
      const res = await apiFetch("/api/v1/project-applications/my", {
        method: "GET",
      });

      const code =
        res?.responseStatus?.responseCode?.trim?.().toUpperCase?.() || "";

      if (code !== "SUCCESS" && code !== "00000000") {
        throw new Error(res?.responseStatus?.responseMessage || "Fetch failed");
      }

      const data = res?.response || [];
      // lọc chỉ project được admin duyệt
      const approved = data.filter((p) => p.status === "ADMIN_APPROVED");
      setApprovedProjects(approved);
    } catch (err) {
      console.error(err);
      toast.error("Failed to load approved projects");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchApprovedProjects();
  }, []);

  const handleSelect = (projectId) => {
    nav(`/upload-report/${projectId}`);
  };

  return (
    <div
      ref={sectionRef}
      className="auth-hero min-vh-100 d-flex flex-column align-items-center justify-content-start py-5 reveal"
    >
      <div
        className="container"
        style={{ maxWidth: "1000px", marginTop: "4rem" }}
      >
        <h2 className="fw-bold text-white text-shadow mb-4">
          Select Approved Project to Upload Report
        </h2>

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
          ) : approvedProjects.length === 0 ? (
            <div className="text-center py-5">
              <h5>No approved projects yet</h5>
              <p className="text-muted mb-0">
                Once your project is approved, you can upload reports here.
              </p>
            </div>
          ) : (
            <Table hover responsive className="align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>#</th>
                  <th>Project Title</th>
                  <th>Approved At</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {approvedProjects.map((p, index) => (
                  <tr key={p.id}>
                    <td>{index + 1}</td>
                    <td className="fw-semibold">{p.projectTitle}</td>
                    <td className="text-muted">
                      {new Date(p.submittedAt).toLocaleString("vi-VN", {
                        timeZone: "Asia/Ho_Chi_Minh",
                        hour12: false,
                      })}
                    </td>
                    <td>
                      <Button
                        size="sm"
                        variant="primary"
                        onClick={() => handleSelect(p.projectId)}
                        className="fw-semibold rounded-3"
                      >
                        Upload Report
                      </Button>
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
