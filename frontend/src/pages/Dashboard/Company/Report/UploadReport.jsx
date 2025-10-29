import { useState, useEffect, useRef } from "react";
import "../../EVOwner/ManageVehicle/manage.css";
import { Button, Modal, Toast, ToastContainer, Form } from "react-bootstrap";
import { apiFetch } from "../../../../utils/apiFetch";
import useReveal from "../../../../hooks/useReveal";
import { useParams, useNavigate } from "react-router-dom";
import PaginatedTable from "../../../../components/Pagination/PaginatedTable";

export default function UploadReport() {
  const [reports, setReports] = useState([]);
  const [show, setShow] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [toast, setToast] = useState({
    show: false,
    message: "",
    variant: "success",
  });
  const sectionRef = useRef(null);
  useReveal(sectionRef);
  const { projectId } = useParams();
  const nav = useNavigate();

  useEffect(() => {
    if (!projectId) {
      nav("/company/upload-report"); // nếu không có ID → quay lại trang chọn project
    }
  }, [projectId, nav]);

  //list report đã nộp
  const fetchReports = async () => {
    try {
      setUploading(true);
      const res = await apiFetch("/api/v1/reports/my-reports", {
        method: "GET",
      });

      const data = res?.response || [];
      console.log("API data:", data);
      console.log("projectId param:", projectId);

      //lọc theo projectId
      const filtered = data.filter((r) => r.projectId === Number(projectId));

      const mapped = filtered.map((r) => ({
        id: r.id,
        projectName: r.projectName,
        uploadOriginalFilename: r.uploadOriginalFilename,
        uploadStorageUrl: r.uploadStorageUrl,
        status: r.status,
        submittedAt: r.submittedAt,
      }));

      setReports(mapped);
      const approvedReports = data.filter((r) => r.status === "ADMIN_APPROVED");
      if (approvedReports.length > 0) {
        sessionStorage.setItem("refreshCredits", "true");
      }
    } catch (err) {
      console.error("Failed to load reports:", err);
      showToast("Failed to fetch your reports.", "danger");
    } finally {
      setUploading(false);
    }
  };

  useEffect(() => {
    if (projectId) fetchReports();
  }, [projectId]);

  //upload report
  const handleUpload = async (e) => {
    e.preventDefault();
    console.log("projectId from params:", projectId);

    const file = e.target.file?.files[0];
    if (!file) return showToast("Please select a file first.", "warning");

    const formData = new FormData();
    formData.append("file", file);
    formData.append("projectId", projectId);
    console.log("Uploading with projectId:", projectId);

    try {
      setUploading(true);
      const res = await apiFetch("/api/v1/reports/upload", {
        method: "POST",
        body: formData,
      });

      const code =
        res?.responseStatus?.responseCode?.trim?.().toUpperCase?.() || "";

      showToast("Report uploaded successfully.");
      fetchReports();
      setShow(false);
    } catch (err) {
      console.error("Upload failed:", err);

      // Lấy code từ error trả về
      const backendCode =
        err?.response?.responseStatus?.responseCode || err?.code || err?.status;

      if (backendCode === "409101") {
        showToast(
          "A report for this project and period already exists. Please check your previous uploads.",
          "warning"
        );
      } else {
        showToast(
          "Upload failed: " + (err.message || "Unexpected error."),
          "danger"
        );
      }
    } finally {
      setUploading(false);
    }
  };

  const showToast = (message, variant = "success") =>
    setToast({ show: true, message, variant });

  return (
    <div ref={sectionRef} className="reveal">
      <div className="vehicle-search-section2">
        <h1 className="title fw-bold">Your Reports</h1>
        <div className="d-flex justify-content-center gap-3 mt-2">
          <Button className="mb-3" onClick={() => setShow(true)}>
            Upload Report
          </Button>

          {/* Nút xem mẫu CSV */}
          <Button
            variant="outline-info"
            className="mb-3"
            onClick={() => window.open("/sample-report.csv", "_blank")}
          >
            View CSV Template
          </Button>
        </div>
      </div>

      {/* Modal Upload */}
      <Modal show={show} onHide={() => setShow(false)}>
        <Modal.Header closeButton>
          <Modal.Title>Upload New Report</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleUpload}>
          <Modal.Body>
            <Form.Group>
              <Form.Label>Select CSV File</Form.Label>
              <Form.Control type="file" name="file" accept=".csv" />
              <Form.Text className="text-muted">
                Upload monthly emission report in CSV format. You can{" "}
                <a href="/sample-report.csv" target="_blank">
                  download the template here
                </a>
                .
              </Form.Text>
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShow(false)}>
              Close
            </Button>
            <Button type="submit" variant="primary" disabled={uploading}>
              {uploading ? "Uploading..." : "Submit"}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <div className="table-wrapper">
        <table className="vehicle-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Project</th>
              <th>File Name</th>
              <th>Status</th>
              <th>Submitted At</th>
              <th>Actions</th>
            </tr>
          </thead>
          <PaginatedTable
            items={reports}
            itemsPerPage={5}
            renderRow={(r, index) => (
              <tr key={r.id}>
                <td>{index + 1}</td>
                <td>{r.projectName}</td>
                <td>
                  <a
                    href={r.uploadStorageUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    {r.uploadOriginalFilename || "—"}
                  </a>
                </td>
                <td>
                  <span className={`status-badge ${r.status?.toLowerCase()}`}>
                    {r.status}
                  </span>
                </td>
                <td>
                  {new Date(r.submittedAt).toLocaleString("vi-VN", {
                    timeZone: "Asia/Ho_Chi_Minh",
                    hour12: false,
                  })}
                </td>
                <td className="action-buttons">
                  <button
                    className="action-btn view"
                    title="View Details"
                    onClick={() => nav(`/detail-report/${r.id}`)}
                  >
                    <i className="bi bi-eye"></i>
                  </button>
                </td>
              </tr>
            )}
          />
        </table>
      </div>

      <ToastContainer position="bottom-end" className="p-3">
        <Toast
          onClose={() => setToast({ ...toast, show: false })}
          show={toast.show}
          bg={toast.variant}
          delay={3000}
          autohide
        >
          <Toast.Body className="text-white">{toast.message}</Toast.Body>
        </Toast>
      </ToastContainer>
    </div>
  );
}
