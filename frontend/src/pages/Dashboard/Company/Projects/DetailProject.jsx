import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Button, Card, Spinner, Row, Col } from "react-bootstrap";
import { toast } from "react-toastify";
import { apiFetch } from "../../../../utils/apiFetch";
import { FaArrowLeft } from "react-icons/fa";

export default function DetailProject() {
  const { id } = useParams();
  const nav = useNavigate();
  const [project, setProject] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchProject = async () => {
    setLoading(true);
    try {
      const res = await apiFetch(`/api/v1/projects/${id}`);
      if (!res?.response) throw new Error("Project not found");
      setProject(res.response);
    } catch (err) {
      console.error("Error fetching project detail:", err);
      toast.error("Failed to load project detail");
      nav("/detail-project");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProject();
  }, [id]);

  if (loading)
    return (
      <div className="d-flex justify-content-center align-items-center vh-100">
        <Spinner animation="border" />
      </div>
    );

  if (!project)
    return (
      <div className="text-center mt-5">
        <p className="text-danger">Project not found or deleted.</p>
        <Button variant="outline-info" onClick={() => nav("/company/projects")}>
          <FaArrowLeft /> Back to Projects
        </Button>
      </div>
    );

  return (
    <div className="auth-hero min-vh-100 bg-light py-4">
      <div className="container">
        <Button
          variant="outline-info"
          className="d-flex align-items-center gap-2 mb-4"
          onClick={() => nav("/company/projects")}
        >
          <FaArrowLeft /> Back to Projects
        </Button>

        <Card className="shadow-lg border-0 rounded-4 p-4">
          <Card.Body>
            <Row>
              <Col md={8}>
                <h3 className="fw-bold mb-3">{project.title}</h3>
                <p className="text-muted mb-4">{project.description}</p>

                <h5>Commitments</h5>
                <p>{project.commitments}</p>

                <h5>Technical Indicators</h5>
                <p>{project.technicalIndicators}</p>

                <h5>Measurement Method</h5>
                <p>{project.measurementMethod}</p>

                <h5>Legal Documents</h5>
                <p>
                  <a
                    href={project.legalDocsUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    {project.legalDocsUrl}
                  </a>
                </p>
              </Col>

              <Col md={4} className="text-center">
                {project.logo ? (
                  <img
                    src={project.logo}
                    alt="Project Logo"
                    className="img-fluid rounded mb-3"
                    style={{ maxHeight: "180px", objectFit: "contain" }}
                  />
                ) : (
                  <div className="bg-secondary text-white rounded p-5 mb-3">
                    No Logo
                  </div>
                )}
                <div className="mb-2">
                  <span
                    className={`badge ${
                      project.status === "APPROVED"
                        ? "bg-success"
                        : project.status === "REJECTED"
                        ? "bg-danger"
                        : "bg-warning text-dark"
                    }`}
                  >
                    {project.status}
                  </span>
                </div>

                <div className="text-muted small">
                  Created:{" "}
                  {new Date(
                    project.createdAt || project.createAt
                  ).toLocaleString()}
                </div>

                {project.reviewer && (
                  <div className="mt-3">
                    <h6 className="fw-bold mb-1">Reviewer</h6>
                    <p>{project.reviewer}</p>
                    {project.reviewNote && (
                      <>
                        <h6 className="fw-bold mb-1">Review Note</h6>
                        <p>{project.reviewNote}</p>
                      </>
                    )}
                  </div>
                )}
              </Col>
            </Row>
          </Card.Body>
        </Card>
      </div>
    </div>
  );
}
