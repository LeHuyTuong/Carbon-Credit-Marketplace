import { useEffect, useState } from "react";
import { Container, Table, Accordion, Spinner, Button } from "react-bootstrap";
import { useParams, useLocation, useNavigate } from "react-router-dom";
import { apiFetch } from "../../../../utils/apiFetch";
import { FaArrowLeft } from "react-icons/fa";

export default function PayoutOwnerDetail() {
  const params = useParams();
  const location = useLocation(); // dùng để xác định đang ở chế độ preview hay review
  const [owners, setOwners] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const nav = useNavigate();
  // tự xác định mode: preview hay review
  const mode = location.pathname.includes("/preview/") ? "preview" : "review";

  // lấy id từ params theo chế độ
  const id = params.reportId || params.distributionId;

  //api lấy dữ liệu chi tiết payout
  const fetchPayoutOwners = async () => {
    try {
      setLoading(true); // bật spinner
      setError(""); // reset lỗi cũ

      // xác định endpoint phù hợp
      const url =
        mode === "preview"
          ? `/api/v1/companies/reports/${id}/owners` // preview payout (chưa chia)
          : `/api/v1/companies/payouts/${id}/summary`; // review payout (đã chia)

      // gọi API backend
      const res = await apiFetch(url, { method: "GET" });
      const data = res?.response?.summary || {};

      // danh sách chủ EV
      setOwners(data.items || []);
      // dữ liệu tổng hợp
      setSummary({
        ownersCount: data.ownersCount || 0,
        totalEnergyKwh: data.totalEnergyKwh || 0,
        totalCredits: data.totalCredits || 0,
        grandTotalPayout: data.grandTotalPayout || 0,
      });
    } catch (err) {
      console.error("Error fetching payout detail:", err);
      setError(err.message || "Failed to load payout detail.");
    } finally {
      setLoading(false); // tắt spinner
    }
  };

  //tự động call api khi có id or đổi mode
  useEffect(() => {
    if (id) fetchPayoutOwners();
  }, [id, mode]);

  return (
    <div className="auth-hero min-vh-100 py-4">
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
        onClick={() => {
          if (location.state?.fromModal) {
            nav("/wallet", {
              state: {
                fromModal: true,
                modalState: location.state.modalState,
              },
            });
          } else {
            nav("/wallet");
          }
        }}
      >
        <FaArrowLeft /> Back to Wallet
      </Button>
      <Container>
        {/*Tiêu đề trang*/}
        <h1 className="text-light fw-bold mb-2 mt-5">
          {mode === "preview"
            ? "Payout Preview by EV Owner"
            : "Payout Summary by EV Owner"}
        </h1>

        {/*Mô tả phụ thuộc mode*/}
        <p className="text-light mb-4">
          {mode === "preview"
            ? `Simulated distribution for emission report #${id}.`
            : `Official distribution record for payout #${id}.`}
        </p>

        {/*Trạng thái đang tải*/}
        {loading ? (
          <div className="text-center mt-5">
            <Spinner animation="border" variant="light" />
          </div>
        ) : error ? (
          // khi có lỗi
          <p className="text-danger text-center">{error}</p>
        ) : owners.length === 0 ? (
          <p className="text-light text-center">No payout data found.</p>
        ) : (
          //khi có dữ liệu
          <>
            {/*Thông tin tổng hợp payout*/}
            {summary && (
              <div className="d-flex justify-content-center">
                <div
                  className="text-dark border rounded p-3 mb-2 bg-white shadow-sm"
                  style={{
                    maxWidth: "500px",
                    width: "100%",
                  }}
                >
                  <p>
                    <strong>Total EV Owners:</strong> {summary.ownersCount}
                  </p>
                  <p>
                    <strong>Total Energy (kWh):</strong>{" "}
                    {summary.totalEnergyKwh}
                  </p>
                  <p>
                    <strong>Total Credits:</strong> {summary.totalCredits}
                  </p>
                  <p>
                    <strong>Grand Total Payout (VND):</strong>{" "}
                    {summary.grandTotalPayout.toLocaleString("vi-VN")}
                  </p>
                </div>
              </div>
            )}

            {/*Danh sách chi tiết từng EV Owner*/}
            <Accordion className="mt-3">
              {owners.map((owner, idx) => (
                <Accordion.Item eventKey={idx.toString()} key={owner.ownerId}>
                  {/* Header accordion hiển thị tên chủ EV và tổng tiền nhận */}
                  <Accordion.Header>
                    <div className="w-100 d-flex justify-content-between align-items-center">
                      <span className="fw-bold text-dark">
                        {owner.ownerName}
                      </span>
                      <small className="text-muted">
                        {owner.email} | Payout:{" "}
                        {owner.amountUsd.toLocaleString("vi-VN")}₫
                      </small>
                    </div>
                  </Accordion.Header>

                  {/* Body accordion hiển thị chi tiết năng lượng, credits, payout */}
                  <Accordion.Body>
                    <Table striped bordered hover responsive size="sm">
                      <thead className="table-light">
                        <tr>
                          <th>Email</th>
                          <th>Phone</th>
                          <th>Vehicles</th>
                          <th>Energy (kWh)</th>
                          <th>Credits</th>
                          <th>Payout ($)</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          <td>{owner.email}</td>
                          <td>{owner.phone}</td>
                          <td>{owner.vehiclesCount}</td>
                          <td>{owner.energyKwh}</td>
                          <td>{owner.credits}</td>
                          <td>{owner.amountUsd?.toLocaleString("vi-VN")}</td>
                        </tr>
                      </tbody>
                    </Table>
                  </Accordion.Body>
                </Accordion.Item>
              ))}
            </Accordion>
          </>
        )}
      </Container>
    </div>
  );
}
