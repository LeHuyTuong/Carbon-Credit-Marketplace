import React, { useEffect, useState, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Card, Spinner, Alert, Button, Badge } from "react-bootstrap";
import { FaArrowLeft } from "react-icons/fa";
import { apiFetch } from "../../../../utils/apiFetch";
import useReveal from "../../../../hooks/useReveal";

export default function ViewRegisteredProject() {
  const { id } = useParams(); // applicationId
  const nav = useNavigate();
  const [application, setApplication] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [requestDateTime, setRequestDateTime] = useState(null); //thêm state lưu thời gian fallback
  const sectionRef = useRef(null);
  useReveal(sectionRef);

  //fetch api
  useEffect(() => {
    const fetchApplication = async () => {
      setLoading(true);
      try {
        // gọi API lấy thông tin application
        const res = await apiFetch(`/api/v1/project-applications/${id}`, {
          method: "GET",
        });

        // chuẩn hoá responseCode và kiểm tra status
        const code =
          res?.responseStatus?.responseCode?.trim?.().toUpperCase?.() || "";

        if (code !== "SUCCESS" && code !== "00000000") {
          throw new Error(
            res?.responseStatus?.responseMessage || "Failed to load data"
          );
        }

        // gán data application
        setApplication(res?.response || {});
        // lưu thời gian request để dùng fallback
        setRequestDateTime(res?.requestDateTime || new Date().toISOString()); //lưu thời điểm request
      } catch (err) {
        console.error("Error fetching application:", err);
        setError(err.message || "Failed to fetch detail");
      } finally {
        setLoading(false);
      }
    };

    fetchApplication();
  }, [id]); // chạy lại khi id thay đổi

  //load data
  if (loading)
    return (
      <div
        ref={sectionRef}
        className="reveal d-flex justify-content-center align-items-center vh-100 bg-light"
      >
        {/* hiển thị spinner khi đang load */}
        <Spinner animation="border" />
      </div>
    );

  if (error)
    return (
      <div className="container mt-5">
        {/* render lỗi */}
        <Alert variant="danger">{error}</Alert>
      </div>
    );

  if (!application)
    return (
      <div className="container mt-5">
        {/* không tìm thấy dữ liệu */}
        <Alert variant="warning">Application not found</Alert>
      </div>
    );

  // chọn màu badge theo status
  const statusColor =
    application.status === "ADMIN_APPROVED"
      ? "success"
      : application.status.includes("REJECTED")
      ? "danger"
      : application.status === "UNDER_REVIEW"
      ? "warning text-dark"
      : "secondary";

  return (
    <div className="auth-hero min-vh-100 d-flex align-items-center justify-content-center py-5">
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
        onClick={() => nav("/list-projects")}
      >
        <FaArrowLeft /> Back to List
      </Button>

      <Card
        className="shadow-lg border-0"
        style={{
          maxWidth: "700px",
          width: "100%",
          background: "rgba(255,255,255,0.95)",
          backdropFilter: "blur(8px)",
          borderRadius: "15px",
        }}
      >
        <Card.Body>
          <h3 className="text-center mb-4 fw-bold text-dark">
            Registered Project Details
          </h3>

          <div className="mb-3">
            <strong>Project Title:</strong> {application.projectTitle}
          </div>
          <div className="mb-3">
            <strong>Company Name:</strong> {application.companyName}
          </div>
          <div className="mb-3">
            <strong>Status:</strong>{" "}
            <Badge bg={statusColor}>{application.status}</Badge>
          </div>

          {/*hiển thị thời gian fallback nếu submittedAt null */}
          <div className="mb-3">
            <strong>Submitted At:</strong>{" "}
            {application.submittedAt
              ? new Date(application.submittedAt).toLocaleString("en-GB", {
                  day: "2-digit",
                  month: "2-digit",
                  year: "numeric",
                  hour: "2-digit",
                  minute: "2-digit",
                  second: "2-digit",
                })
              : new Date(requestDateTime || Date.now()).toLocaleString(
                  "en-GB",
                  {
                    day: "2-digit",
                    month: "2-digit",
                    year: "numeric",
                    hour: "2-digit",
                    minute: "2-digit",
                    second: "2-digit",
                  }
                )}
          </div>

          <div className="mb-3">
            <strong>Review Note:</strong>{" "}
            {application.reviewNote || "No review yet"}
          </div>
          <div className="mb-3">
            <strong>Final Review Note:</strong>{" "}
            {application.finalReviewNote || "-"}
          </div>
          <div>
            <strong>Legal Documents:</strong>{" "}
            {application.applicationDocsUrl ? (
              <a
                href={application.applicationDocsUrl}
                target="_blank"
                rel="noopener noreferrer"
              >
                View Document
              </a>
            ) : (
              "Not uploaded"
            )}
          </div>
        </Card.Body>
      </Card>
    </div>
  );
}
