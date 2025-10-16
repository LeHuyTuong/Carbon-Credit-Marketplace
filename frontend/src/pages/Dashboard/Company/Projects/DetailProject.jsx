import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import {
  Container,
  Row,
  Col,
  Card,
  Image,
  Spinner,
  Alert,
} from "react-bootstrap";
import RegisterProject from "./RegisterProject";
import { apiFetch } from "../../../../utils/apiFetch";

export default function ProjectDetailsPage() {
  const { id } = useParams();
  const [project, setProject] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchProjectById = async () => {
      setLoading(true);
      try {
        const res = await apiFetch(`/api/v1/projects/${id}`, {
          method: "GET",
        });

        const code =
          res?.responseStatus?.responseCode?.trim?.().toUpperCase?.() || "";

        if (code !== "SUCCESS" && code !== "00000000") {
          throw new Error(
            res?.responseStatus?.responseMessage || "Fetch failed"
          );
        }

        setProject(res?.response || {});
      } catch (err) {
        console.error("Error fetching project:", err);
        setError(err.message || "Failed to load project");
      } finally {
        setLoading(false);
      }
    };

    fetchProjectById();
  }, [id]);

  if (loading)
    return (
      <div className="d-flex justify-content-center align-items-center vh-100">
        <Spinner animation="border" />
      </div>
    );

  if (error)
    return (
      <Container className="mt-5">
        <Alert variant="danger">{error}</Alert>
      </Container>
    );

  if (!project)
    return (
      <Container className="mt-5">
        <Alert variant="warning">Project not found.</Alert>
      </Container>
    );

  return (
    <Container fluid className="py-4">
      <Row>
        {/*left: project details */}
        <Col md={8}>
          <Card className="shadow-sm mb-4">
            <Card.Body>
              <div className="d-flex align-items-center mb-3">
                {project.logo && (
                  <Image
                    src={project.logo}
                    alt="Project Logo"
                    width={70}
                    height={70}
                    roundedCircle
                    className="me-3 border"
                  />
                )}
                <div>
                  <h3 className="mb-0">{project.title}</h3>
                  <small className="text-muted">
                    Status: {project.status || "N/A"}
                  </small>
                </div>
              </div>
              <p>{project.description}</p>
            </Card.Body>
          </Card>

          <Card className="shadow-sm mb-3">
            <Card.Header>Commitments</Card.Header>
            <Card.Body>
              <p>{project.commitments || "N/A"}</p>
            </Card.Body>
          </Card>

          <Card className="shadow-sm mb-3">
            <Card.Header>Technical Indicators</Card.Header>
            <Card.Body>
              <p>{project.technicalIndicators || "N/A"}</p>
            </Card.Body>
          </Card>

          <Card className="shadow-sm mb-3">
            <Card.Header>Measurement Method</Card.Header>
            <Card.Body>
              <p>{project.measurementMethod || "N/A"}</p>
            </Card.Body>
          </Card>

          {project.legalDocsUrl && (
            <Card className="shadow-sm">
              <Card.Header>Legal Documents</Card.Header>
              <Card.Body>
                <a
                  href={project.legalDocsUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  View Legal Document
                </a>
              </Card.Body>
            </Card>
          )}

          <Card className="shadow-sm mt-3">
            <Card.Header>Reviewer Info</Card.Header>
            <Card.Body>
              <p>
                <strong>Reviewer:</strong> {project.reviewer || "N/A"}
              </p>
              <p>
                <strong>Review Note:</strong> {project.reviewNote || "N/A"}
              </p>
              <p>
                <strong>Final Reviewer:</strong>{" "}
                {project.finalReviewer || "N/A"}
              </p>
              <p>
                <strong>Created At:</strong>{" "}
                {new Date(project.createdAt).toLocaleString()}
              </p>
            </Card.Body>
          </Card>
        </Col>

        {/*right: tegister form */}
        <Col md={4}>
          <RegisterProject />
        </Col>
      </Row>
    </Container>
  );
}
