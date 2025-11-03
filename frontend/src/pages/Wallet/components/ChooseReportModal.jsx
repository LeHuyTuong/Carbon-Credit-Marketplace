import React, { useEffect, useState } from "react";
import { Modal, Button, Form, Spinner } from "react-bootstrap";
import useWalletData from "./useWalletData";

export default function ChooseReportModal({
  show,
  onHide,
  onConfirm,
  projectId,
}) {
  // lấy các hàm fetch từ hook
  const { fetchApprovedProjects, fetchApprovedReports } = useWalletData();
  // state quản lý danh sách project và report
  const [approvedReports, setApprovedReports] = useState([]);
  const [projects, setProjects] = useState([]);
  // state lưu giá trị người dùng chọn
  const [selectedReport, setSelectedReport] = useState("");
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [projectIdState, setProjectId] = useState(projectId || "");
  // trạng thái loading để hiển thị spinner
  const [loading, setLoading] = useState(false);

  // fetch danh sách dự án được duyệt khi mở modal
  useEffect(() => {
    const loadProjects = async () => {
      const projs = await fetchApprovedProjects();
      setProjects(projs);
    };
    loadProjects();
  }, []);

  // fetch danh sách report khi modal mở hoặc projectId thay đổi
  useEffect(() => {
    if (show) {
      (async () => {
        setLoading(true);
        const reports = await fetchApprovedReports(projectId);
        setApprovedReports(reports);
        setLoading(false);
      })();
    } else {
      // reset khi modal đóng
      setSelectedReport("");
      setAmount("");
    }
  }, [show, projectId]);

  // xử lý khi bấm xác nhận chia lợi nhuận
  const handleSubmit = () => {
    // tìm report đã được chọn trong danh sách approvedReports
    // dùng Number() để ép kiểu vì giá trị từ <select> là string
    const selected = approvedReports.find(
      (r) => r.id === Number(selectedReport)
    );
    // nếu chưa chọn report thì cảnh báo và dừng lại
    if (!selected) return alert("Please select a report first.");
    // nếu chưa nhập hoặc sai thì cảnh báo
    if (!amount || Number(amount) <= 0)
      return alert("Please enter a valid amount.");
    if (!description.trim()) return alert("Please enter a description.");
    if (!projectIdState) return alert("Project ID is required.");

    // nếu mọi thứ hợp lệ, gọi callback onConfirm và truyền dữ liệu
    // ép kiểu về Number để backend nhận đúng định dạng
    onConfirm({
      projectId: Number(projectIdState),
      emissionReportId: selected.id,
      totalMoneyToDistribute: Number(amount),
      description: description.trim(),
    });
  };

  return (
    <Modal show={show} onHide={onHide} centered>
      <Modal.Header closeButton>
        <Modal.Title>Distribute Profit</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {/* trạng thái đang tải */}
        {loading ? (
          <div className="d-flex justify-content-center py-4">
            <Spinner animation="border" />
          </div>
        ) : approvedReports.length === 0 ? (
          // khi không có report nào
          <p className="text-muted">
            No approved reports available for profit sharing.
          </p>
        ) : (
          // form chọn dự án, report, nhập số tiền và mô tả
          <Form>
            {/* --- Select Project --- */}
            <Form.Group className="mb-3">
              <Form.Label>Select Project</Form.Label>
              <Form.Select
                value={projectIdState}
                onChange={async (e) => {
                  const pid = Number(e.target.value);
                  setProjectId(pid);
                  setLoading(true);
                  const reports = await fetchApprovedReports(pid);
                  setApprovedReports(reports);
                  setLoading(false);
                }}
              >
                <option value="">-- Choose a project --</option>
                {projects.map((p) => (
                  <option key={p.id} value={p.projectId}>
                    {p.projectTitle}
                  </option>
                ))}
              </Form.Select>
            </Form.Group>

            {/* --- Select Approved Report --- */}
            <Form.Group className="mb-3">
              <Form.Label>Approved Report</Form.Label>
              <Form.Select
                value={selectedReport}
                onChange={(e) => setSelectedReport(e.target.value)}
              >
                <option value="">-- Select report to share --</option>
                {approvedReports.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.projectName} — {r.fileName} (
                    {new Date(r.submittedAt).toLocaleDateString("vi-VN")})
                  </option>
                ))}
              </Form.Select>
            </Form.Group>

            {/* --- Amount --- */}
            <Form.Group className="mb-3">
              <Form.Label>Total Money to Distribute ($)</Form.Label>
              <Form.Control
                type="number"
                placeholder="Enter total amount"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
              />
            </Form.Group>

            {/* --- Description --- */}
            <Form.Group className="mb-3">
              <Form.Label>Description</Form.Label>
              <Form.Control
                type="text"
                placeholder="Describe this profit sharing"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </Form.Group>
          </Form>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onHide}>
          Cancel
        </Button>
        <Button
          variant="primary"
          onClick={handleSubmit}
          disabled={loading || approvedReports.length === 0}
        >
          Confirm Share
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
