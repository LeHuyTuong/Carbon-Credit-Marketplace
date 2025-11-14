import { useState, useEffect, useRef } from "react";
import "../../EVOwner/ManageVehicle/manage.css";
import { Button, Modal, Toast, ToastContainer, Form } from "react-bootstrap";
import { apiFetch } from "../../../../utils/apiFetch";
import useReveal from "../../../../hooks/useReveal";
import { useParams, useNavigate } from "react-router-dom";
import PaginatedTable from "../../../../components/Pagination/PaginatedTable";

export default function UploadReport() {
  const [reports, setReports] = useState([]); // danh sách báo cáo đã upload
  const [show, setShow] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [toast, setToast] = useState({
    // hiển thị toast message
    show: false,
    message: "",
    variant: "success",
  });
  const sectionRef = useRef(null); // ref cho hiệu ứng reveal
  useReveal(sectionRef);
  const { projectId } = useParams(); // lấy projectId từ URL
  const nav = useNavigate();

  // nếu không có projectId → quay lại trang chọn project
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

      // chuẩn hóa dữ liệu trước khi render
      const mapped = filtered.map((r) => ({
        id: r.id,
        projectName: r.projectName,
        uploadOriginalFilename: r.uploadOriginalFilename,
        uploadStorageUrl: r.uploadStorageUrl,
        status: r.status,
        waitingFor: r.waitingFor,
        submittedAt: r.submittedAt,
      }));

      //sort cái mới nhất
      const sorted = mapped.sort((a, b) => {
        const r1 = new Date(a.submittedAt);
        const r2 = new Date(b.submittedAt);
        return r2 - r1;
      });

      setReports(sorted);
      // nếu có report đã được duyệt → trigger refresh credit
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

  // tự động fetch khi có projectId
  useEffect(() => {
    if (projectId) fetchReports();
  }, [projectId]);

  //upload report
  const handleUpload = async (e) => {
    e.preventDefault();
    console.log("projectId from params:", projectId);

    // lấy file từ input
    const file = e.target.file?.files[0];
    if (!file) return showToast("Please select a file first.", "warning");

    // tạo formData để gửi file và projectId lên server
    const formData = new FormData();
    formData.append("file", file);
    formData.append("projectId", projectId);
    console.log("Uploading with projectId:", projectId);

    try {
      setUploading(true); // bật trạng thái đang upload
      const res = await apiFetch("/api/v1/reports/upload", {
        method: "POST",
        body: formData, // gửi dữ liệu file dưới dạng multipart/form-data
      });

      // lấy mã phản hồi từ backend
      const code =
        res?.responseStatus?.responseCode?.trim?.().toUpperCase?.() || "";

      // nếu upload thành công → reload danh sách và đóng modal
      showToast("Report uploaded successfully.");
      fetchReports();
      setShow(false);
    } catch (err) {
      console.error("Upload failed:", err);

      // Lấy code từ error trả về
      const backendCode =
        err?.response?.responseStatus?.responseCode || err?.code || err?.status;

      // lấy mã lỗi trả về từ backend
      if (backendCode === "409101") {
        showToast(
          "A report for this project and period already exists. Please check your previous uploads.",
          "warning"
        );
      } else {
        // trường hợp report đã tồn tại
        showToast(
          "Upload failed: " + (err.message || "Unexpected error."),
          "danger"
        );
      }
    } finally {
      setUploading(false);
    }
  };

  // hiển thị toast message
  const showToast = (message, variant = "success") =>
    setToast({ show: true, message, variant });

  return (
    <div ref={sectionRef} className="reveal">
      {/* tiêu đề và nút hành động */}
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
            style={{
              borderRadius: "8px",
              background: "rgba(255, 255, 255, 0.85)",
              backdropFilter: "blur(6px)",
              zIndex: 20,
            }}
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

      {/* bảng danh sách report */}
      <div className="table-wrapper">
        <table className="vehicle-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Project</th>
              <th>File Name</th>
              <th>Status</th>
              <th>Waiting Status</th>
              <th>Submitted At</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {/* dùng component phân trang */}
            <PaginatedTable
              items={reports}
              itemsPerPage={5}
              renderRow={(r, index) => (
                <tr key={r.id}>
                  <td>{index + 1}</td>
                  <td className="fw-bold">{r.projectName}</td>
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
                  <td className="text-muted">{r?.waitingFor}</td>
                  <td>
                    {new Date(r.submittedAt + "Z").toLocaleString("vi-VN", {
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
          </tbody>
        </table>
      </div>

      {/* toast hiển thị thông báo */}
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
