import React, { useEffect, useState, useRef } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  Container,
  Row,
  Col,
  Card,
  Image,
  Spinner,
  Alert,
  Badge,
  Button,
} from "react-bootstrap";
import RegisterProject from "./RegisterProject";
import { apiFetch } from "../../../../utils/apiFetch";
import { FaArrowLeft } from "react-icons/fa";
import useReveal from "../../../../hooks/useReveal";

export default function ProjectDetailsPage() {
  const { id } = useParams(); //lấy id project từ URL
  const [project, setProject] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const nav = useNavigate(); // ref cho hiệu ứng reveal
  const sectionRef = useRef(null);
  useReveal(sectionRef);

  // gọi API lấy thông tin project theo id
  useEffect(() => {
    if (!id) return; // nếu không có id thì bỏ qua
    const fetchProjectById = async () => {
      setLoading(true);
      try {
        const res = await apiFetch(`/api/v1/projects/${id}`, {
          method: "GET",
        });

        //chuẩn hóa response code để xử lý thành công thất bại
        const code =
          res?.responseStatus?.responseCode?.trim?.().toUpperCase?.() || "";

        // nếu không phải mã thành công thì ném lỗi
        if (code !== "SUCCESS" && code !== "00000000") {
          throw new Error(
            res?.responseStatus?.responseMessage || "Fetch failed"
          );
        }

        setProject(res?.response || {}); // cập nhật dữ liệu
      } catch (err) {
        console.error("Error fetching project:", err);
        setError(err.message || "Failed to load project");
      } finally {
        setLoading(false);
      }
    };

    fetchProjectById();
  }, [id]);

  // khi đang tải dữ liệu
  if (loading)
    return (
      <div
        ref={sectionRef}
        className="reveal d-flex justify-content-center align-items-center vh-100 bg-light"
      >
        <Spinner animation="border" variant="primary" />
      </div>
    );

  // khi có lỗi xảy ra
  if (error)
    return (
      <Container className="mt-5">
        <Alert variant="danger">{error}</Alert>
      </Container>
    );

  // khi không tìm thấy project
  if (!project)
    return (
      <Container className="mt-5">
        <Alert variant="warning">Project not found.</Alert>
      </Container>
    );

  // status màu cho badge
  const getStatusVariant = (status) => {
    switch (status?.toUpperCase()) {
      case "OPEN":
        return "success";
      case "PENDING":
        return "warning";
      case "REJECTED":
        return "danger";
      default:
        return "secondary";
    }
  };

  return (
    <div>
      {/* nút quay lại trang Home */}
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

      {/* hiển thị nội dung chính */}
      <div
        className="py-5"
        style={{
          background: "linear-gradient(180deg, #f8f9fa 0%, #e9ecef 100%)",
          minHeight: "100vh",
        }}
      >
        <h1 className="text-center fw-bold mb-5">Register Project</h1>

        <Container>
          <Row className="g-4 align-items-start">
            {/* Cột bên trái: thông tin chi tiết project */}
            <Col lg={8}>
              <Card className="border-0 shadow-lg rounded-4 overflow-hidden mb-4">
                {/* ảnh banner của project */}
                {project.bannerUrl ? (
                  <Card.Img
                    variant="top"
                    src={project.bannerUrl}
                    alt="Project banner"
                    style={{ height: "200px", objectFit: "cover" }}
                  />
                ) : (
                  // nếu không có banner thì hiển thị màu nền mặc định
                  <div
                    style={{
                      background:
                        "linear-gradient(135deg, #4dabf7, #22b8cf, #51cf66)",
                      height: "200px",
                    }}
                  />
                )}

                <Card.Body className="p-4">
                  {/* phần logo, tiêu đề và trạng thái */}
                  <div className="d-flex align-items-center mb-3">
                    {project.logo && (
                      <Image
                        src={project.logo}
                        alt="Project Logo"
                        width={70}
                        height={70}
                        roundedCircle
                        className="me-3 border shadow-sm"
                      />
                    )}
                    <div>
                      <h3 className="fw-bold mb-1 text-primary">
                        {project.title}
                      </h3>
                      <Badge bg={getStatusVariant(project.status)}>
                        {project.status || "N/A"}
                      </Badge>
                    </div>
                  </div>

                  {/* mô tả project */}
                  <p className="text-muted">{project.description}</p>
                </Card.Body>
              </Card>

              {/* các thẻ hiển thị thông tin chi tiết */}
              <Row xs={1} md={2} className="g-3">
                <Col>
                  <Card className="shadow-sm border-0 rounded-4 h-100">
                    <Card.Header className="fw-semibold bg-light border-0">
                      Commitments
                    </Card.Header>
                    <Card.Body>
                      <p className="mb-0 text-secondary">
                        {project.commitments || "N/A"}
                      </p>
                    </Card.Body>
                  </Card>
                </Col>

                <Col>
                  <Card className="shadow-sm border-0 rounded-4 h-100">
                    <Card.Header className="fw-semibold bg-light border-0">
                      Technical Indicators
                    </Card.Header>
                    <Card.Body>
                      <p className="mb-0 text-secondary">
                        {project.technicalIndicators || "N/A"}
                      </p>
                    </Card.Body>
                  </Card>
                </Col>

                <Col>
                  <Card className="shadow-sm border-0 rounded-4 h-100">
                    <Card.Header className="fw-semibold bg-light border-0">
                      Measurement Method
                    </Card.Header>
                    <Card.Body>
                      <p className="mb-0 text-secondary">
                        {project.measurementMethod || "N/A"}
                      </p>
                    </Card.Body>
                  </Card>
                </Col>

                {/* hiển thị link tài liệu pháp lý nếu có */}
                {project.legalDocsFile && (
                  <Col>
                    <Card className="shadow-sm border-0 rounded-4 h-100">
                      <Card.Header className="fw-semibold bg-light border-0">
                        Legal Documents
                      </Card.Header>
                      <Card.Body>
                        <a
                          href={project.legalDocsFile}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          View Legal Document
                        </a>
                      </Card.Body>
                    </Card>
                  </Col>
                )}
              </Row>
            </Col>

            {/* Cột bên phải: form đăng ký project */}
            <Col lg={4}>
              <div
                className="p-3 rounded-4 shadow-lg border-0"
                style={{
                  background: "white",
                  position: "sticky",
                  top: "20px",
                }}
              >
                <RegisterProject />
              </div>
            </Col>
          </Row>
        </Container>
      </div>
    </div>
  );
}
