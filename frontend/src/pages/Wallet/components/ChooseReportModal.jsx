import React, { useEffect, useState } from "react";
import { Modal, Button, Form, Spinner } from "react-bootstrap";
import useWalletData from "./useWalletData";
import { toast } from "react-toastify";

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
  const [sharePercent, setSharePercent] = useState("");
  const [description, setDescription] = useState("");
  const [projectIdState, setProjectId] = useState(projectId || "");
  // trạng thái loading để hiển thị spinner
  const [loading, setLoading] = useState(false);

  // fetch danh sách dự án được duyệt khi mở modal
  useEffect(() => {
    if (show) {
      (async () => {
        setLoading(true);
        const projs = await fetchApprovedProjects();
        setProjects(projs);
        setLoading(false);
      })();
    } else {
      setSelectedReport("");
      setAmount("");
      setSharePercent("");
      setDescription("");
    }
  }, [show]);

  // fetch danh sách report khi modal mở hoặc projectId thay đổi
  useEffect(() => {
    if (show && projectIdState) {
      (async () => {
        setLoading(true);
        const reports = await fetchApprovedReports(projectIdState);
        setApprovedReports(reports);
        setLoading(false);
      })();
    } else {
      setApprovedReports([]);
    }
  }, [show, projectIdState]);

  // xử lý khi bấm xác nhận chia lợi nhuận
  const handleSubmit = () => {
    // tìm report đã được chọn trong danh sách approvedReports
    // dùng Number() để ép kiểu vì giá trị từ <select> là string
    const selected = approvedReports.find(
      (r) => r.id === Number(selectedReport)
    );
    if (!projectIdState) return toast.error("Please select a project.");
    // nếu chưa chọn report thì cảnh báo và dừng lại
    if (!selected) return toast.error("Please select an approved report.");
    // nếu chưa nhập hoặc sai thì cảnh báo
    if (!amount || Number(amount) <= 0)
      return toast.error("Enter a valid total amount.");
    if (!sharePercent || Number(sharePercent) <= 0)
      return toast.error("Enter a valid company share percent (1–100).");
    if (!description.trim()) return toast.error("Please enter a description.");

    // nếu mọi thứ hợp lệ, gọi callback onConfirm và truyền dữ liệu
    // ép kiểu về Number để backend nhận đúng định dạng
    onConfirm({
      projectId: Number(projectIdState),
      emissionReportId: selected.id,
      totalMoneyToDistribute: Number(amount),
      companySharePercent: Number(sharePercent),
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
                disabled={!approvedReports.length}
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

            {/* --- Company Share Percent --- */}
            <Form.Group className="mb-3">
              <Form.Label>Company Share Percent (%)</Form.Label>
              <Form.Control
                type="number"
                placeholder="Enter company share percent"
                min="1"
                max="100"
                value={sharePercent}
                onChange={(e) => setSharePercent(e.target.value)}
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
        <Button variant="primary" onClick={handleSubmit} disabled={loading}>
          Confirm Share
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
