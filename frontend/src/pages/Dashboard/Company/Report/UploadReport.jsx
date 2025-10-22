import { useState, useEffect, useRef } from "react";
import "../../EVOwner/ManageVehicle/manage.css";
import { Button, Modal, Toast, ToastContainer, Form } from "react-bootstrap";
import { apiFetch } from "../../../../utils/apiFetch";
import useReveal from "../../../../hooks/useReveal";
import { useNavigate } from "react-router-dom";

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
  const nav = useNavigate();

  const fetchReports = async () => {
    try {
      setUploading(true);
      const res = await apiFetch("/api/v1/reports/my-reports", {
        method: "GET",
      });

      //dữ liệu BE trả về trong res.response
      const data = res?.response || [];

      // map sang định dạng UI
      const mapped = data.map((r) => ({
        id: r.id,
        projectName: r.projectName,
        uploadOriginalFilename: r.uploadOriginalFilename,
        uploadStorageUrl: r.uploadStorageUrl,
        status: r.status,
        submittedAt: r.submittedAt,
      }));

      setReports(mapped);
    } catch (err) {
      console.error("Failed to load reports:", err);
      showToast("Failed to fetch your reports.", "danger");
    } finally {
      setUploading(false);
    }
  };

  useEffect(() => {
    fetchReports();
  }, []);

  const handleUpload = async (e) => {
    e.preventDefault();
    const file = e.target.file?.files[0];
    if (!file) return showToast("Please select a file first.", "warning");

    const formData = new FormData();
    formData.append("file", file);

    try {
      setUploading(true);
      const res = await apiFetch("/api/v1/reports/upload", {
        method: "POST",
        body: formData,
      });
      showToast("Report uploaded successfully.");
      fetchReports();
      setShow(false);
    } catch (err) {
      showToast("Upload failed: " + err.message, "danger");
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
            variant="outline-warning"
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
          <tbody>
            {reports.length > 0 ? (
              reports.map((r) => (
                <tr key={r.id}>
                  <td>{r.id}</td>
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
                  </td>{" "}
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
              ))
            ) : (
              <tr>
                <td colSpan="6" className="no-data text-center py-5">
                  <i className="bi bi-file-earmark-text text-accent fs-3 d-block mb-2"></i>
                  <h5 className="text-dark">No reports yet</h5>
                  <p className="text-muted">
                    Upload your first emission report to get started.
                  </p>
                </td>
              </tr>
            )}
          </tbody>
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
