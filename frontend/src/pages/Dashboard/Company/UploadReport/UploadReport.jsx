import { useState, useEffect, useRef } from "react";
import "../../EVOwner/ManageVehicle/manage.css";
import { Button, Modal, Toast, ToastContainer, Form } from "react-bootstrap";
import { apiFetch } from "../../../../utils/apiFetch";
import useReveal from "../../../../hooks/useReveal";

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

  const fetchReports = async () => {
    try {
      const res = await apiFetch(
        "/api/v1/reports/list-cva-check?status=PENDING"
      );
      setReports(res?.response || []);
    } catch (err) {
      console.error("Failed to load reports:", err);
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
      <div className="vehicle-search-section">
        <h1 className="title">Your Reports</h1>
        <Button className="mb-3" onClick={() => setShow(true)}>
          Upload Report
        </Button>
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
                Upload monthly emission report in CSV format.
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

      {/* Table */}
      <div className="table-wrapper">
        <table className="vehicle-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Project</th>
              <th>Period</th>
              <th>Total Energy</th>
              <th>Total CO₂</th>
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
                  <td>{r.period}</td>
                  <td>{r.totalEnergy?.toLocaleString()}</td>
                  <td>{r.totalCo2?.toLocaleString()}</td>
                  <td>
                    <span className={`status-badge ${r.status?.toLowerCase()}`}>
                      {r.status}
                    </span>
                  </td>
                  <td>{new Date(r.submittedAt).toLocaleDateString()}</td>
                  <td className="action-buttons">
                    <button className="action-btn view">
                      <i className="bi bi-eye"></i>
                    </button>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="8" className="no-data text-center py-5">
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
