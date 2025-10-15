import { useEffect, useState } from "react";
import { Button, Table, Spinner } from "react-bootstrap";
import { toast } from "react-toastify";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../../../context/AuthContext";
import { apiFetch } from "../../../../utils/apiFetch";
import { FaArrowLeft } from "react-icons/fa";

export default function ListProjects() {
  const { user } = useAuth();
  const nav = useNavigate();
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);

  // lấy danh sách project
  const fetchProjects = async () => {
    setLoading(true);
    try {
      const res = await apiFetch("/api/v1/projects");
      const data = res?.response || [];
      setProjects(data);
    } catch (err) {
      console.error("Error fetching projects:", err);
      toast.error("Failed to load projects list");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProjects();
  }, []);

  const handleView = (id) => {
    nav("/detail-project");
  };

  return (
    <div className="auth-hero min-vh-100 bg-light py-4">
      <div className="container">
        <div className="d-flex justify-content-between align-items-center mb-4">
          <Button
            variant="outline-info"
            className="d-flex align-items-center gap-2"
            onClick={() => nav("/home")}
          >
            <FaArrowLeft /> Back to Home
          </Button>
          <Button onClick={() => nav("/company/project-register")}>
            + New Project
          </Button>
        </div>

        <h2 className="text-center mb-4 fw-bold">Your Company Projects</h2>

        {loading ? (
          <div className="d-flex justify-content-center align-items-center vh-50">
            <Spinner animation="border" />
          </div>
        ) : projects.length === 0 ? (
          <div className="text-center mt-5">
            <h5>No projects submitted yet</h5>
            <p>Click “+ New Project” to create one.</p>
          </div>
        ) : (
          <Table bordered hover responsive className="shadow-sm rounded-3">
            <thead className="table-light">
              <tr>
                <th>#</th>
                <th>Title</th>
                <th>Status</th>
                <th>Company</th>
                <th>Created At</th>
                <th>Reviewer</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {projects.map((p, index) => (
                <tr key={p.id}>
                  <td>{index + 1}</td>
                  <td>{p.title}</td>
                  <td>
                    <span
                      className={`badge ${
                        p.status === "APPROVED"
                          ? "bg-success"
                          : p.status === "REJECTED"
                          ? "bg-danger"
                          : "bg-secondary"
                      }`}
                    >
                      {p.status || "PENDING"}
                    </span>
                  </td>
                  <td>{p.companyName}</td>
                  <td>{new Date(p.createdAt).toLocaleDateString()}</td>
                  <td>{p.reviewer || "-"}</td>
                  <td>
                    <Button
                      size="sm"
                      variant="outline-primary"
                      onClick={() => handleView(p.id)}
                    >
                      View
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </div>
    </div>
  );
}
