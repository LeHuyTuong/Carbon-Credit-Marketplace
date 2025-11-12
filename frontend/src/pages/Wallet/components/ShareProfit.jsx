// import React, { useEffect, useState } from "react";
// import { Modal, Button, Form, Spinner, Table } from "react-bootstrap";
// import { toast } from "react-toastify";
// import useWalletData from "./useWalletData";
// import { apiFetch } from "../../../utils/apiFetch";
// import { useNavigate } from "react-router-dom";

// export default function ShareProfit({ show, onHide }) {
//   //hooks từ useWalletData
//   const { fetchApprovedProjects, fetchApprovedReports, shareProfit } =
//     useWalletData();

//   //state quản lý dữ liệu
//   const [projects, setProjects] = useState([]);
//   const [approvedReports, setApprovedReports] = useState([]);
//   const [summary, setSummary] = useState(null);

//   //state điều khiển giao diện
//   const [formula, setFormula] = useState(null); // công thức chia payout

//   const [loading, setLoading] = useState(false); // loading cho project + report
//   const [previewing, setPreviewing] = useState(false); // loading cho phần preview summary
//   const navigate = useNavigate();

//   // Load project list + formula khi mở modal
//   useEffect(() => {
//     if (show) {
//       (async () => {
//         try {
//           setLoading(true);
//           // 1. Lấy danh sách dự án đã được admin duyệt
//           const projs = await fetchApprovedProjects();
//           setProjects(projs);
//           // 2. Lấy công thức chia payout
//           const f = await apiFetch("/api/v1/companies/payout-formula", {
//             method: "GET",
//           });
//           setFormula(f.response);
//         } catch (err) {
//           toast.error("Failed to load formula.");
//           console.error(err);
//         } finally {
//           setLoading(false);
//         }
//       })();
//     } else {
//       // reset state khi đóng modal
//       setApprovedReports([]);
//       setSelectedReport("");
//       setOwners([]);
//       setSummary(null);
//     }
//   }, [show]);

//   // Load reports when khi chọn project mới
//   useEffect(() => {
//     if (show && projectIdState) {
//       (async () => {
//         try {
//           setLoading(true);
//           const reports = await fetchApprovedReports(projectIdState);
//           setApprovedReports(reports);
//         } catch (err) {
//           toast.error("Failed to load reports.");
//           console.error(err);
//         } finally {
//           setLoading(false);
//         }
//       })();
//     }
//   }, [show, projectIdState]);

//   // Auto fetch payout summary khi chọn report
//   useEffect(() => {
//     if (selectedReport) fetchPayoutSummary(selectedReport);
//   }, [selectedReport]);

//   //gọi api xem trước payout theo report
//   const fetchPayoutSummary = async (reportId) => {
//     try {
//       setPreviewing(true);
//       const res = await apiFetch(
//         `/api/v1/companies/reports/${reportId}/owners`,
//         { method: "GET" }
//       );
//       const data = res?.response;
//       // cập nhật danh sách EV owners và tổng payout
//       setOwners(data?.summary?.items || []);
//       setSummary(data?.summary || null);
//       toast.success("Loaded payout summary.");
//     } catch (err) {
//       toast.error("Failed to fetch payout summary.");
//       console.error(err);
//     } finally {
//       setPreviewing(false);
//     }
//   };

//   //gửi request chia lợi nhuận
//   const handleConfirm = async () => {
//     if (!projectIdState || !selectedReport)
//       return toast.error("Please select project and report first.");

//     try {
//       const res = await shareProfit({
//         projectId: Number(projectIdState),
//         emissionReportId: Number(selectedReport),
//       });

//       if (res?.responseCode === "200" || res?.responseCode === "OK") {
//         toast.success("Payout distribution executed successfully.");
//         onHide();
//       } else {
//         toast.error(res?.responseMessage || "Failed to execute payout.");
//       }
//     } catch (err) {
//       toast.error(err.message || "Unexpected error during payout.");
//       console.error(err);
//     }
//   };

//   return (
//     <Modal show={show} onHide={onHide} size="md" centered>
//       <Modal.Header closeButton>
//         <Modal.Title>Distribute Payout</Modal.Title>
//       </Modal.Header>

//       <Modal.Body>
//         {loading ? (
//           // Khi đang tải dữ liệu
//           <div className="d-flex justify-content-center py-4">
//             <Spinner animation="border" />
//           </div>
//         ) : (
//           <>
//             {/*Select Project*/}
//             <Form.Group className="mb-3">
//               <Form.Label>Select Project</Form.Label>
//               <Form.Select
//                 value={projectIdState}
//                 onChange={async (e) => {
//                   const pid = Number(e.target.value);
//                   setProjectId(pid);
//                   setSelectedReport("");
//                   setSummary(null);
//                   const reports = await fetchApprovedReports(pid);
//                   setApprovedReports(reports);
//                 }}
//               >
//                 <option value="">-- Choose a project --</option>
//                 {projects.map((p) => (
//                   <option key={p.id} value={p.projectId}>
//                     {p.projectTitle}
//                   </option>
//                 ))}
//               </Form.Select>
//             </Form.Group>

//             {/*Select Approved Report*/}
//             <Form.Group className="mb-4">
//               <Form.Label>Select Approved Report</Form.Label>
//               <Form.Select
//                 value={selectedReport}
//                 onChange={(e) => setSelectedReport(e.target.value)}
//                 disabled={!approvedReports.length}
//               >
//                 <option value="">-- Select report --</option>
//                 {approvedReports.map((r) => (
//                   <option key={r.id} value={r.id}>
//                     {r.projectName} — {r.fileName} (
//                     {new Date(r.submittedAt).toLocaleDateString("vi-VN")})
//                   </option>
//                 ))}
//               </Form.Select>
//             </Form.Group>

//             {/*Formula Info*/}
//             {formula && (
//               <div className="border rounded p-3 mb-3 bg-light">
//                 <p className="mb-1">
//                   <strong>Formula:</strong> {formula.pricingMode}
//                 </p>
//                 <p className="mb-1">
//                   <strong>Unit Price per Credit:</strong>{" "}
//                   {formula.unitPricePerCredit}
//                 </p>
//                 <p className="mb-0">
//                   <strong>Unit Price per kWh:</strong> {formula.unitPricePerKwh}
//                 </p>
//               </div>
//             )}

//             {/*Summary*/}
//             {previewing ? (
//               <div className="d-flex justify-content-center py-4">
//                 <Spinner animation="border" />
//               </div>
//             ) : summary ? (
//               <>
//                 <div className="mb-3 p-3 border rounded bg-light">
//                   <p className="mb-1">
//                     <strong>Total EV Owners:</strong> {summary.ownersCount}
//                   </p>
//                   <p className="mb-1">
//                     <strong>Total Energy (kWh):</strong>{" "}
//                     {summary.totalEnergyKwh}
//                   </p>
//                   <p className="mb-1">
//                     <strong>Total Credits:</strong> {summary.totalCredits}
//                   </p>
//                   <p className="mb-0">
//                     <strong>Grand Total Payout:</strong>{" "}
//                     {summary.grandTotalPayout.toLocaleString("vi-VN")}
//                   </p>
//                 </div>

//                 {/*Nút xem chi tiết*/}
//                 <div className="d-flex justify-content-end mb-3">
//                   <Button
//                     variant="outline-info"
//                     onClick={() =>
//                       navigate(`/payout/preview/${selectedReport}`)
//                     }
//                   >
//                     View Details
//                   </Button>
//                 </div>
//               </>
//             ) : selectedReport ? (
//               <p className="text-muted text-center">
//                 No payout summary available for this report.
//               </p>
//             ) : null}
//           </>
//         )}
//       </Modal.Body>

//       <Modal.Footer>
//         <Button variant="secondary" onClick={onHide}>
//           Cancel
//         </Button>
//         <Button
//           variant="primary"
//           onClick={handleConfirm}
//           disabled={loading || !summary}
//         >
//           Confirm Distribution
//         </Button>
//       </Modal.Footer>
//     </Modal>
//   );
// }
import React, { useEffect, useState } from "react";
import { Modal, Button, Spinner, Form } from "react-bootstrap";
import { Formik } from "formik";
import * as Yup from "yup";
import { toast } from "react-toastify";
import useWalletData from "./useWalletData";
import { apiFetch } from "../../../utils/apiFetch";
import { useNavigate } from "react-router-dom";

const schema = Yup.object().shape({
  projectId: Yup.number().required("Please select a project."),
  reportId: Yup.number().required("Please select an approved report."),
});

export default function ShareProfit({ show, onHide, initialData }) {
  const { fetchApprovedProjects, fetchApprovedReports, shareProfit } =
    useWalletData();

  const [projects, setProjects] = useState([]);
  const [reports, setReports] = useState([]);
  const [summary, setSummary] = useState(initialData?.summary || null);
  const [formula, setFormula] = useState(null);
  const [loading, setLoading] = useState(false);
  const [previewing, setPreviewing] = useState(false);
  const navigate = useNavigate();

  // load dữ liệu ban đầu
  useEffect(() => {
    if (!show) return;
    (async () => {
      try {
        setLoading(true);
        const projectList = await fetchApprovedProjects();
        setProjects(projectList);
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

  useEffect(() => {
    if (show && initialData?.summary) {
      setSummary(initialData.summary);
    }
  }, [show, initialData]);

  useEffect(() => {
    if (!show) {
      setReports([]);
    }
  }, [show]);

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
                const res = await shareProfit({
                  projectId: Number(values.projectId),
                  emissionReportId: Number(values.reportId),
                });

                if (res?.responseCode === "200" || res?.responseCode === "OK") {
                  toast.success("Payout executed successfully.");
                  onHide();
                } else {
                  toast.error(res?.responseMessage || "Payout failed.");
                }
              } catch (err) {
                toast.error(err.message || "Unexpected error.");
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
                      <p className="mb-1">
                        <strong>Formula:</strong> {formula.pricingMode}
                      </p>
                      <p className="mb-1">
                        <strong>Price per Credit:</strong>{" "}
                        {formula.unitPricePerCredit}
                      </p>
                      <p className="mb-0">
                        <strong>Price per kWh:</strong>{" "}
                        {formula.unitPricePerKwh}
                      </p>
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
