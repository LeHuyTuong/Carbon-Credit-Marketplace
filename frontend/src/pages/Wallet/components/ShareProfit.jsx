import React, { useEffect, useState } from "react";
import { Modal, Button, Spinner, Form, Collapse } from "react-bootstrap";
import { Formik } from "formik";
import * as Yup from "yup";
import { toast } from "react-toastify";
import useWalletData from "./useWalletData";
import { apiFetch } from "../../../utils/apiFetch";
import { useNavigate } from "react-router-dom";
import formulaImg from "/src/assets/formula.jpeg";

// Schema validate
const schema = Yup.object().shape({
  projectId: Yup.number().required("Please select a project."),
  reportId: Yup.number().required("Please select an approved report."),
});

export default function ShareProfit({ show, onHide, initialData }) {
  const {
    fetchApprovedProjects,
    fetchApprovedReports,
    shareProfit,
    fetchWallet,
  } = useWalletData();

  // State lưu dự án, báo cáo, tổng hợp payout, công thức tính
  const [projects, setProjects] = useState([]);
  const [reports, setReports] = useState([]);
  const [summary, setSummary] = useState(initialData?.summary || null);
  const [formula, setFormula] = useState(null);

  // State cho UI
  const [loading, setLoading] = useState(false);
  const [previewing, setPreviewing] = useState(false);
  const [showFormulaImg, setShowFormulaImg] = useState(false);
  const navigate = useNavigate();

  // load dữ liệu ban đầu
  useEffect(() => {
    if (!show) return;
    (async () => {
      try {
        setLoading(true);
        // Lấy danh sách dự án đã được duyệt
        const projectList = await fetchApprovedProjects();
        setProjects(projectList);
        // Lấy công thức tính giá payout
        const f = await apiFetch("/api/v1/companies/payout-formula", {
          method: "GET",
        });
        setFormula(f.response);
      } catch (err) {
        console.error(err);
        toast.error("Failed to load initial data.");
      } finally {
        setLoading(false);
      }
    })();
  }, [show]);

  // Gán lại summary nếu modal mở lại hoặc có initialData
  useEffect(() => {
    if (show && initialData?.summary) {
      setSummary(initialData.summary);
    }
  }, [show, initialData]);

  // Reset danh sách report khi modal đóng
  useEffect(() => {
    if (!show) {
      setReports([]);
    }
  }, [show]);

  // Hàm preview chi tiết tính toán payout của report
  const handlePreview = async (reportId) => {
    try {
      setPreviewing(true);
      const res = await apiFetch(
        `/api/v1/companies/reports/${reportId}/owners`,
        { method: "GET" }
      );
      const data = res?.response?.summary;
      setSummary(data || null);
    } catch (err) {
      console.error(err);
      toast.error("Failed to load payout summary.");
    } finally {
      setPreviewing(false);
    }
  };

  return (
    <Modal show={show} onHide={onHide} size="md" centered>
      <Modal.Header closeButton>
        <Modal.Title>Distribute Payout</Modal.Title>
      </Modal.Header>

      <Modal.Body>
        {loading ? (
          // Hiển thị spinner trong khi tải dữ liệu ban đầu
          <div className="text-center py-5">
            <Spinner animation="border" />
          </div>
        ) : (
          <Formik
            enableReinitialize
            validationSchema={schema}
            initialValues={{
              projectId: initialData?.projectId || "",
              reportId: initialData?.reportId || "",
            }}
            onSubmit={async (values, { setSubmitting }) => {
              try {
                setSubmitting(true);

                // 1. Lấy wallet balance mới nhất
                const wallet = await fetchWallet();
                const balance = wallet?.balance || 0;

                // 2. Lấy số tiền cần trả
                const required = summary?.grandTotalPayout || 0;

                // 3. So sánh
                if (balance < required) {
                  toast.error(`Insufficient balance to process the payout`);
                  setSubmitting(false);
                  return; // Dừng không cho chạy tiếp
                }

                // 4. Nếu đủ tiền → thực hiện payout
                const res = await shareProfit({
                  projectId: Number(values.projectId),
                  emissionReportId: Number(values.reportId),
                });

                if (res?.responseCode === "200" || res?.responseCode === "OK") {
                  toast.success("Payout successfully.");
                  onHide();
                } else {
                  toast.error(res?.responseMessage || "Payout failed.");
                }
              } catch (err) {
                toast.error(err.message || "Unknown error.");
              } finally {
                setSubmitting(false);
              }
            }}
          >
            {({
              handleSubmit,
              handleChange,
              values,
              touched,
              errors,
              isSubmitting,
              setFieldValue,
              ...rest
            }) => {
              // Khi mở modal bằng initialData thì load danh sách báo cáo của project đó
              useEffect(() => {
                if (initialData?.projectId && show) {
                  (async () => {
                    try {
                      const list = await fetchApprovedReports(
                        initialData.projectId
                      );
                      setReports(list);
                    } catch (err) {
                      console.error("Failed to load reports for project:", err);
                    }
                  })();
                }
              }, [initialData, show]);

              // Khi reports đã có, set lại reportId
              useEffect(() => {
                if (initialData?.reportId && reports.length > 0) {
                  setFieldValue("reportId", initialData.reportId);
                  if (!summary) handlePreview(initialData.reportId);
                }
              }, [reports]);

              return (
                <form noValidate onSubmit={handleSubmit}>
                  {/* Project Select */}
                  <Form.Group className="mb-3">
                    <Form.Label>Project</Form.Label>
                    <Form.Select
                      name="projectId"
                      value={values.projectId}
                      onChange={async (e) => {
                        const pid = e.target.value;
                        handleChange(e);
                        setSummary(null);
                        if (pid) {
                          try {
                            const reports = await fetchApprovedReports(pid);
                            setReports(reports);
                          } catch {
                            toast.error("Failed to load reports.");
                          }
                        } else setReports([]);
                      }}
                      isInvalid={touched.projectId && !!errors.projectId}
                    >
                      <option value="">-- Select a project --</option>
                      {projects.map((p) => (
                        <option key={p.id} value={p.projectId}>
                          {p.projectTitle}
                        </option>
                      ))}
                    </Form.Select>
                    <Form.Control.Feedback type="invalid">
                      {errors.projectId}
                    </Form.Control.Feedback>
                  </Form.Group>

                  {/* Report Select */}
                  <Form.Group className="mb-4">
                    <Form.Label>Approved Report</Form.Label>
                    <Form.Select
                      name="reportId"
                      value={values.reportId}
                      onChange={(e) => {
                        handleChange(e);
                        if (e.target.value) handlePreview(e.target.value);
                      }}
                      disabled={!reports.length}
                      isInvalid={touched.reportId && !!errors.reportId}
                    >
                      <option value="">-- Select report --</option>
                      {reports.map((r) => (
                        <option key={r.id} value={r.id}>
                          {r.projectName} — {r.fileName} (
                          {new Date(r.submittedAt).toLocaleDateString("vi-VN")})
                        </option>
                      ))}
                    </Form.Select>
                    <Form.Control.Feedback type="invalid">
                      {errors.reportId}
                    </Form.Control.Feedback>
                  </Form.Group>

                  {/* Formula info */}
                  {formula && (
                    <div className="border rounded p-3 mb-3 bg-light">
                      {/* Toggle ảnh công thức */}
                      <div className="text-center mb-2">
                        <Button
                          variant="outline-info"
                          size="sm"
                          onClick={() => setShowFormulaImg((prev) => !prev)}
                        >
                          {showFormulaImg ? "Hide Formula" : "Show Formula"}
                        </Button>
                      </div>

                      {/* Nội dung collapse */}
                      <Collapse in={showFormulaImg}>
                        <div>
                          <p className="mb-1">
                            <strong>Formula:</strong> {formula.pricingMode}
                          </p>

                          <p className="mb-1">
                            <strong>Price per Credit:</strong>{" "}
                            {formula.unitPricePerCredit}$
                          </p>
                          <p className="mb-3">
                            <strong>Price per kWh:</strong>{" "}
                            {formula.unitPricePerKwh}$
                          </p>

                          <p
                            className="text-muted"
                            style={{ fontSize: "0.85rem" }}
                          >
                            <strong>Note:</strong> Price per Credit is
                            dynamically benchmarked against the current average
                            market rate.
                          </p>

                          <div className="text-center mb-2">
                            <img
                              src={formulaImg}
                              alt="Formula"
                              style={{
                                width: "95%",
                                borderRadius: "6px",
                                border: "1px solid #e3e3e3",
                                backgroundColor: "#f8f9fa",
                                padding: "4px",
                                transition: "all 0.3s ease",
                              }}
                            />
                          </div>
                        </div>
                      </Collapse>
                    </div>
                  )}

                  {/* Preview summary */}
                  {previewing ? (
                    <div className="text-center py-4">
                      <Spinner animation="border" />
                    </div>
                  ) : (
                    summary && (
                      <div className="border rounded p-3 mb-3 bg-light">
                        <p className="mb-1">
                          <strong>Total EV Owners:</strong>{" "}
                          {summary.ownersCount}
                        </p>
                        <p className="mb-1">
                          <strong>Total Energy (kWh):</strong>{" "}
                          {summary.totalEnergyKwh}
                        </p>
                        <p className="mb-1">
                          <strong>Total Credits:</strong> {summary.totalCredits}
                        </p>
                        <p className="mb-0">
                          <strong>Grand Total Payout:</strong>{" "}
                          {summary.grandTotalPayout.toLocaleString("vi-VN")}
                        </p>
                        <div className="text-end mt-3">
                          <Button
                            variant="outline-info"
                            size="sm"
                            onClick={() =>
                              navigate(`/payout/preview/${values.reportId}`, {
                                state: {
                                  fromModal: true,
                                  modalState: {
                                    projectId: values.projectId,
                                    reportId: values.reportId,
                                    summary,
                                  },
                                },
                              })
                            }
                          >
                            View Details
                          </Button>
                        </div>
                      </div>
                    )
                  )}

                  {/* Footer buttons */}
                  <div className="d-flex justify-content-between mt-4">
                    <Button
                      variant="secondary"
                      onClick={onHide}
                      disabled={isSubmitting}
                    >
                      Cancel
                    </Button>
                    <Button
                      type="submit"
                      variant="primary"
                      disabled={isSubmitting || !summary}
                    >
                      {isSubmitting ? "Processing..." : "Confirm Distribution"}
                    </Button>
                  </div>
                </form>
              );
            }}
          </Formik>
        )}
      </Modal.Body>
    </Modal>
  );
}
