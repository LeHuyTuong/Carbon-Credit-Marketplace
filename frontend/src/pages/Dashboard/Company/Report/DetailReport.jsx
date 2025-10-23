import { useEffect, useState, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  Card,
  Spinner,
  Container,
  Row,
  Col,
  Badge,
  Button,
} from "react-bootstrap";
import { apiFetch } from "../../../../utils/apiFetch";
import { FaArrowLeft } from "react-icons/fa";
import useReveal from "../../../../hooks/useReveal";

export default function ReportDetail() {
  const { id } = useParams();
  const nav = useNavigate();
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const sectionRef = useRef(null);
  useReveal(sectionRef);

  useEffect(() => {
    const fetchDetail = async () => {
      try {
        setLoading(true);

        // Gọi API chi tiết report theo ID
        const res = await apiFetch(`/api/v1/reports/${id}`, {
          method: "GET",
        });

        // backend trả về chi tiết report trong "response"
        setReport(res?.response || null);
      } catch (err) {
        console.error("Failed to load report detail:", err);
        setReport(null);
      } finally {
        setLoading(false);
      }
    };

    fetchDetail();
  }, [id]);

  if (loading)
    return (
      <div ref={sectionRef} className="reveal min-vh-100">
        {loading ? (
          <Spinner animation="border" variant="primary" />
        ) : (
          <ReportContent report={report} />
        )}
      </div>
    );

  if (!report)
    return (
      <Container className="mt-5 text-center">
        <p>No details found for this report.</p>
        <Button variant="outline-secondary" onClick={() => nav(-1)}>
          Back
        </Button>
      </Container>
    );

  return (
    <div className="auth-hero d-flex justify-content-center align-items-center min-vh-100 w-100">
      <Container className="py-4">
        {/*nút back */}
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
          onClick={() => nav("/upload-report")}
        >
          <FaArrowLeft /> Back to Report
        </Button>

        <Card
          className="shadow-lg border-0 p-3"
          style={{
            borderRadius: "15px",
            background: "rgba(255,255,255,0.9)",
            backdropFilter: "blur(10px)",
          }}
        >
          <Card.Body>
            <h3 className="fw-bold mb-3">Report #{id}</h3>
            <Row className="g-3">
              <Col md={6}>
                <p>
                  <strong>Project:</strong> {report.projectName}
                </p>
                <p>
                  <strong>Seller:</strong> {report.sellerName}
                </p>
                <p>
                  <strong>Period:</strong> {report.period}
                </p>
                <p>
                  <strong>Total Energy:</strong> {report.totalEnergy} kWh
                </p>
                <p>
                  <strong>Total CO₂:</strong> {report.totalCo2} kg
                </p>
                <p>
                  <strong>Vehicles:</strong> {report.vehicleCount}
                </p>
              </Col>
              <Col md={6}>
                <p>
                  <strong>Status:</strong>{" "}
                  <Badge bg="info">{report.status}</Badge>
                </p>
                <p>
                  <strong>Submitted At:</strong>{" "}
                  {new Date(report.submittedAt).toLocaleString("vi-VN", {
                    timeZone: "Asia/Ho_Chi_Minh",
                    hour12: false,
                  })}
                </p>
                <p>
                  <strong>Source:</strong> {report.source}
                </p>
                <p>
                  <strong>Uploaded File:</strong>
                </p>
                <a
                  href={report.uploadStorageUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  {report.uploadOriginalFilename}
                </a>
                <p className="mt-2">
                  <strong>Rows:</strong> {report.uploadRows} |{" "}
                  <strong>Size:</strong>{" "}
                  {(report.uploadSizeBytes / 1024).toFixed(1)} KB
                </p>
              </Col>
            </Row>
          </Card.Body>
        </Card>
      </Container>
    </div>
  );
}
